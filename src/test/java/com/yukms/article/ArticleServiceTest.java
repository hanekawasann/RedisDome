package com.yukms.article;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.yukms.BaseServiceTest;
import com.yukms.article.entity.Article;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ArticleServiceTest extends BaseServiceTest {
    @Autowired
    private ArticleService articleService;

    @Test
    public void test_add() {
        List<Article> articles = articleService.get("votes", "design");
        Assert.assertEquals(0, articles.size());
        articleService.add(getArticles());
        List<Article> articles2 = articleService.get("votes", "design");
        Assert.assertEquals(3, articles2.size());
    }

    @Test
    public void test_get() {
        articleService.add(getArticles());
        List<Article> articles = articleService.get("votes", "design");
        Assert.assertEquals("3", articles.get(0).getId());
        Assert.assertEquals("2", articles.get(1).getId());
        Assert.assertEquals("1", articles.get(2).getId());

        articleService.vote("3");
        articleService.vote("3");
        List<Article> articles2 = articleService.get("votes", "design");
        Assert.assertEquals("2", articles2.get(0).getId());
        Assert.assertEquals("1", articles2.get(1).getId());
        Article article = articles2.get(2);
        Assert.assertEquals("3", article.getId());
        Assert.assertEquals("3", article.getVotes());
    }

    private List<Article> getArticles() {
        List<String> groups = Collections.singletonList("design");
        Article article1 = new Article();
        article1.setId("1");
        article1.setTitle("设计模式之创建型模式");
        article1.setTime(time2LongString("2013-12-03T10:15:30"));
        article1.setGroups(groups);
        article1.setVotes("3");
        Article article2 = new Article();
        article2.setId("2");
        article2.setTitle("设计模式之结构型模式");
        article2.setTime(time2LongString("2011-12-03T10:15:30"));
        article2.setGroups(groups);
        article2.setVotes("2");
        Article article3 = new Article();
        article3.setId("3");
        article3.setTitle("New year new bug");
        article3.setTime(time2LongString("2018-12-03T10:15:30"));
        article3.setGroups(groups);
        article3.setVotes("1");
        return Arrays.asList(article1, article2, article3);
    }

    private String time2LongString(String time) {
        Instant instant = Instant.now();
        ZoneId systemZoneId = ZoneId.systemDefault();
        ZoneOffset zoneOffset = systemZoneId.getRules().getOffset(instant);
        return String.valueOf(LocalDateTime.parse(time).toEpochSecond(zoneOffset));
    }
}