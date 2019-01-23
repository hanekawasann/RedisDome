package com.yukms.redisinactiondemo.chat;

import java.time.LocalDateTime;

/**
 * @author yukms 2019/1/23
 */
public class ChatMessage {
    private long mid;
    private String senderId;
    private String message;
    private LocalDateTime dateTime;

    public long getMid() {
        return mid;
    }

    public void setMid(long mid) {
        this.mid = mid;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }
}
