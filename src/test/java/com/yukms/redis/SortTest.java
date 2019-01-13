package com.yukms.redis;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.yukms.BaseRedisServiceTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.SortParameters;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.query.SortQuery;
import org.springframework.lang.Nullable;

/**
 * @author yukms 2019/1/13
 */
public class SortTest extends BaseRedisServiceTest {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private static final String USER_ID = "user_id:";
    private static final String USER_NAME = "user_name:";
    private static final String USER_LEVEL = "user_level:";

    @Before
    public void initData() {
        ListOperations<String, String> listOperations = stringRedisTemplate.opsForList();
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        getUsers().forEach(user -> {
            String userId = user.getId();
            listOperations.leftPush(USER_ID, userId);
            valueOperations.set(USER_NAME + userId, user.getName());
            valueOperations.set(USER_LEVEL + userId, user.getLevel());
        });
    }

    @Test
    public void test_default() {
        List<User> users = getUsers();
        List<String> afterSort = stringRedisTemplate.sort(new ByDefault());
        Assert.assertNotNull(afterSort);
        for (int i = 0; i < afterSort.size(); i++) {
            Assert.assertTrue(users.get(i).getId().equals(afterSort.get(i)));
        }
    }

    @Test
    public void test_by_and_get() {
        List<User> users = getUsers();
        users.sort(Comparator.comparing(User::getLevel));
        List<String> afterSort = stringRedisTemplate.sort(new ByUserLevelAndGetUserName());
        Assert.assertNotNull(afterSort);
        for (int i = 0; i < afterSort.size(); i++) {
            Assert.assertTrue(users.get(i).getName().equals(afterSort.get(i)));
        }
    }

    @Test
    public void test_get_multiple() {
        List<User> users = getUsers();
        List<String> afterSort = stringRedisTemplate.sort(new GetMultiple());
        Assert.assertNotNull(afterSort);
        Assert.assertEquals(8, afterSort.size());
        Assert.assertEquals(users.get(0).getName(), afterSort.get(0));
        Assert.assertEquals(users.get(0).getLevel(), afterSort.get(1));
        Assert.assertEquals(users.get(1).getName(), afterSort.get(2));
        Assert.assertEquals(users.get(1).getLevel(), afterSort.get(3));
        Assert.assertEquals(users.get(2).getName(), afterSort.get(4));
        Assert.assertEquals(users.get(2).getLevel(), afterSort.get(5));
        Assert.assertEquals(users.get(3).getName(), afterSort.get(6));
        Assert.assertEquals(users.get(3).getLevel(), afterSort.get(7));
    }

    private List<User> getUsers() {
        User user1 = new User("1", "admin", "9999");
        User user2 = new User("2", "jack", "10");
        User user3 = new User("3", "peter", "25");
        User user4 = new User("4", "mary", "70");
        return Arrays.asList(user1, user2, user3, user4);
    }

    private static class ByDefault implements SortQuery<String> {
        @Override
        public String getKey() {
            return USER_ID;
        }

        @Nullable
        @Override
        public SortParameters.Order getOrder() {
            return null;
        }

        @Nullable
        @Override
        public Boolean isAlphabetic() {
            return null;
        }

        @Nullable
        @Override
        public SortParameters.Range getLimit() {
            return null;
        }

        @Nullable
        @Override
        public String getBy() {
            return null;
        }

        @Override
        public List<String> getGetPattern() {
            return null;
        }
    }

    private static class ByUserLevelAndGetUserName extends ByDefault {
        @Nullable
        @Override
        public String getBy() {
            return USER_LEVEL + "*";
        }

        @Override
        public List<String> getGetPattern() {
            return Collections.singletonList(USER_NAME + "*");
        }
    }

    private static class GetMultiple extends ByDefault {
        @Override
        public List<String> getGetPattern() {
            return Arrays.asList(USER_NAME + "*", USER_LEVEL + "*");
        }
    }

    private static class User {
        private String id;
        private String name;
        private String level;

        public User(String id, String name, String level) {
            this.id = id;
            this.name = name;
            this.level = level;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }
    }
}
