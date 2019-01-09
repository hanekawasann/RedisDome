package com.yukms.article;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.yukms.base.BaseService;
import org.apache.commons.lang.StringUtils;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

/**
 * @author yukms 2019/1/7.
 */
@Service
public class ArticleService extends BaseService {
    private static final String ARTICLE = "article:";
    private static final String TIME = "time:";
    private static final String VOTES = "votes:";
    private static final String GROUP = "group:";

    public List<Article> get(String sort, String group) {
        List<String> keys = getKeys(sort, group);
        return getArticles(keys);
    }

    public void add(List<Article> articles) {
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
        for (Article article : articles) {
            String id = article.getId();
            String key = ARTICLE + id;
            hashOperations.put(key, "id", id);
            hashOperations.put(key, "title", article.getTitle());
            String time = article.getTime();
            hashOperations.put(key, "time", time);
            String votes = article.getVotes();
            hashOperations.put(key, "votes", votes);
            List<String> groups = article.getGroups();
            hashOperations.put(key, "groups", StringUtils.join(groups.toArray(new String[groups.size()]), ","));
            zSetOperations.add(TIME, key, Double.valueOf(time));
            zSetOperations.add(VOTES, key, Double.valueOf(votes));
            SetOperations<String, String> setOperations = redisTemplate.opsForSet();
            for (String group : groups) {
                setOperations.add(GROUP + group, key);
            }
        }
    }

    public void vote(String id) {
        String key = ARTICLE + id;
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        hashOperations.increment(key, "votes", 1);
        redisTemplate.opsForZSet().incrementScore(VOTES, key, 1);
    }

    private List<String> getKeys(String sort, String group) {
        ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
        String votesAndGroup = VOTES + group;
        // Boolean hasKey = redisTemplate.hasKey(votesAndGroup);
        // if (!hasKey) {
        String groupKey = GROUP + group;
        String otherKeys = "time".equals(sort) ? TIME : VOTES;
        zSetOperations.intersectAndStore(groupKey, Collections.singletonList(otherKeys), votesAndGroup,
            RedisZSetCommands.Aggregate.MAX);
        //     redisTemplate.expire(votesAndGroup, 60L, TimeUnit.SECONDS);
        // }
        Set<String> ketSet = zSetOperations.range(votesAndGroup, 0, 10);
        List<String> keys = new ArrayList<>();
        Optional.ofNullable(ketSet).ifPresent(keys::addAll);
        return keys;
    }

    private List<Article> getArticles(List<String> keys) {
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        List<Article> articles = new ArrayList<>();
        for (String key : keys) {
            Map<String, String> entries = hashOperations.entries(key);
            Article article = hash2Article(entries);
            articles.add(article);
        }
        return articles;
    }

    private Article hash2Article(Map<String, String> entries) {
        Article article = new Article();
        article.setId(entries.get("id"));
        article.setTitle(entries.get("title"));
        article.setTime(entries.get("time"));
        article.setVotes(entries.get("votes"));
        article.setGroups(Arrays.asList(entries.get("groups").split(",")));
        return article;
    }

}
