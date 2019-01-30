package com.yukms.redisinactiondemo.chat;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.alibaba.fastjson.JSON;
import com.yukms.redisinactiondemo.lock.RedisLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

/**
 * 聊天
 *
 * @author yukms 2019/1/23
 */
@Component
public class ChatComponent {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedisLock lock;
    /** 群组ID递增序列 */
    private static final String IDS_CHAT_KEY = "ids:chat:";
    /** 群组 */
    private static final String CHAT_KEY = "chat:{0}";
    /** 已读有序集合：用户参加各个群组的ID以及用户在这些群里面已读的最大群组消息 */
    private static final String SEEN_KEY = "seen:{0}";
    /** 群组信息ID递增序列 */
    private static final String IDS_KEY = "ids:{0}";
    /** 消息 */
    private static final String MESSAGE_KEY = "msgs:{0}";

    /**
     * 群组聊天产生的内容会以消息为成员、消息ID为分值的形式存储在有序集合里里面。
     * 在创建新群组的时候，程序首先会对一个全局计数器执行自增操作，以此来获得一个新的群组ID。
     * 之后，程序会把群组的初始用户全部添加到一个有序集合里面，并将这些用户在群组里面的最大已读消息初始化为0，
     * 另外还会把这个新群组的ID添加到记录用户已参加群组的有序集合里面。
     * 最后，程序会将一条初始化消息放置到群组有序集合里面，以此来向参加聊天的用户发送初始化消息。
     *
     * @param senderId   创建人ID
     * @param recipients 其他成员ID
     * @param message    消息
     */
    public void creatChat(String senderId, List<String> recipients, String message) {
        Long increment = stringRedisTemplate.opsForValue().increment(IDS_CHAT_KEY);
        Objects.requireNonNull(increment);
        String chatId = increment.toString();
        String chatKey = MessageFormat.format(CHAT_KEY, chatId);
        recipients.add(senderId);
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        for (String memberId : recipients) {
            // 将所有参与群聊的用户添加到有序集合里面
            zSetOperations.add(chatKey, memberId, 0L);
            // 初始化已读有序集合
            String seenKey = MessageFormat.format(SEEN_KEY, memberId);
            zSetOperations.add(seenKey, chatId, 0L);
        }
        sendMessage(chatId, senderId, message);
    }

    /**
     * 一般来说，当程序使用一个来自Redis的值去构建另一个将要被添加到Redis里面的值时，就需要使用锁或者由WATCH、
     * MULTI和EXEC组成的事务来消除竞争条件。
     *
     * @param chatId   聊天室ID
     * @param senderId 发送人ID
     * @param message  消息
     */
    public void sendMessage(String chatId, String senderId, String message) {
        String chatKey = MessageFormat.format(CHAT_KEY, chatId);
        Optional<String> identifier = lock.acquireLock(chatKey, 1L);
        if (identifier.isPresent()) {
            try {
                String idsKey = MessageFormat.format(IDS_KEY, chatId);
                Long messageId = stringRedisTemplate.opsForValue().increment(idsKey);
                Objects.requireNonNull(messageId);
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setMid(messageId);
                chatMessage.setSenderId(senderId);
                chatMessage.setMessage(message);
                chatMessage.setDateTime(LocalDateTime.now());
                String messageKey = MessageFormat.format(MESSAGE_KEY, chatId);
                String value = JSON.toJSONString(chatMessage);
                stringRedisTemplate.opsForZSet().add(messageKey, value, messageId);
            } finally {
                lock.releaseLock(chatKey, identifier.get());
            }
        }
        throw new RuntimeException("Couldn't get the lock.");
    }



}
