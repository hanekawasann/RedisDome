package com.yukms.redisinactiondemo.redis.base;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.sun.istack.internal.NotNull;
import com.yukms.redisinactiondemo.BaseRedisServiceTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.SortParameters;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.query.SortQuery;
import org.springframework.lang.Nullable;

/**
 * 测试排序命令
 *
 * @author yukms 2019/1/13
 */
public class SortTest extends BaseRedisServiceTest {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private static final String USER_ID_KEY = "user_id";
    private static final String USER_NAME_KEY = "user_name:{0}";
    private static final String USER_LEVEL_KEY = "user_level:{0}";
    private static final String USER_KEY = "user:{0}";

    @Before
    public void initData() {
        ListOperations<String, String> listOperations = stringRedisTemplate.opsForList();
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        HashOperations<String, Object, Object> hashOperations = stringRedisTemplate.opsForHash();
        getUsers().forEach(user->{
            String userId = user.getId();
            String name = user.getName();
            String level = user.getLevel();
            listOperations.leftPush(USER_ID_KEY, userId);
            String userNameKey = MessageFormat.format(USER_NAME_KEY, userId);
            valueOperations.set(userNameKey, name);
            String userLevelKey = MessageFormat.format(USER_LEVEL_KEY, userId);
            valueOperations.set(userLevelKey + userId, level);
            String userKey = MessageFormat.format(USER_KEY, userId);
            hashOperations.put(userKey, "id", userId);
            hashOperations.put(userKey, "name", name);
            hashOperations.put(userKey, "level", level);
        });
    }

    @Test
    public void test_default() {
        List<User> users = getUsers();
        List<String> afterSort = stringRedisTemplate.sort(new ByDefault());
        Assert.assertNotNull(afterSort);
        for (int i = 0; i < afterSort.size(); i++) {
            Assert.assertEquals(users.get(i).getId(), afterSort.get(i));
        }
    }

    @Test
    public void test_by_and_get() {
        List<User> users = getUsers();
        users.sort(Comparator.comparing(User::getLevel));
        List<String> afterSort = stringRedisTemplate.sort(new ByUserLevelAndGetUserName());
        Assert.assertNotNull(afterSort);
        for (int i = 0; i < afterSort.size(); i++) {
            Assert.assertEquals(users.get(i).getName(), afterSort.get(i));
        }
    }

    @Test
    public void test_by_and_get_of_hash() {
        List<User> users = getUsers();
        users.sort(Comparator.comparing(User::getLevel));
        List<String> afterSort = stringRedisTemplate.sort(new ByUserLevelAndGetUserNameOfHash());
        Assert.assertNotNull(afterSort);
        for (int i = 0; i < afterSort.size(); i++) {
            Assert.assertEquals(users.get(i).getName(), afterSort.get(i));
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

    @NotNull
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
            return USER_ID_KEY;
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
            return USER_LEVEL_KEY + "*";
        }

        @Override
        public List<String> getGetPattern() {
            return Collections.singletonList(USER_NAME_KEY + "*");
        }
    }

    private static class ByUserLevelAndGetUserNameOfHash extends ByDefault {
        public static final String USER_KEY = MessageFormat.format(SortTest.USER_KEY, "*");

        @Nullable
        @Override
        public String getBy() {
            return USER_KEY + "->level";
        }

        @Override
        public List<String> getGetPattern() {
            return Collections.singletonList(USER_KEY + "->name");
        }
    }

    private static class GetMultiple extends ByDefault {
        @Override
        public List<String> getGetPattern() {
            return Arrays.asList(USER_NAME_KEY + "*", USER_LEVEL_KEY + "*");
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
