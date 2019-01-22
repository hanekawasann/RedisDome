package com.yukms.redisinactiondemo.queue;

import java.time.LocalDateTime;

/**
 * 已售出商品的邮件
 *
 * @author yukms 2019/1/22
 */
public class SoldEmail {

    private String sellerId;
    private String itemId;
    private double price;
    private String buyerId;
    private LocalDateTime time;

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(String buyerId) {
        this.buyerId = buyerId;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

}
