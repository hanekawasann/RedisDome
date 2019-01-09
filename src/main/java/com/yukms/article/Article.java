package com.yukms.article;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yukms 2019/1/7.
 */
public class Article {
    private String id;
    private String title;
    private String time;
    private String votes;
    private List<String> groups = new ArrayList<>();

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getVotes() {
        return votes;
    }

    public void setVotes(String votes) {
        this.votes = votes;
    }
}
