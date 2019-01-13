package com.yukms.redis;

import java.io.Serializable;
import java.util.Objects;

import com.yukms.BaseRedisServiceTest;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * @author yukms 2019/1/10.
 */
public class SaveObjectTest extends BaseRedisServiceTest {
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Test
    public void test_save_object() {
        IdCard idCard = new IdCard("123");
        People people = new People("yukms");
        ValueOperations<Object, Object> valueOperations = redisTemplate.opsForValue();
        valueOperations.set(idCard, people);
        Assert.assertEquals(people, valueOperations.get(idCard));
    }

    public static class IdCard implements Serializable {
        private static final long serialVersionUID = 1485782686505577786L;
        private String number;

        public IdCard(String number) {
            this.number = number;
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) { return true; }
            if (o == null || getClass() != o.getClass()) { return false; }
            IdCard idCard = (IdCard) o;
            return Objects.equals(number, idCard.number);
        }

        @Override
        public int hashCode() {
            return Objects.hash(number);
        }
    }

    public static class People implements Serializable {
        private static final long serialVersionUID = -6046359503734611641L;
        private String name;

        public People(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) { return true; }
            if (o == null || getClass() != o.getClass()) { return false; }
            People people = (People) o;
            return Objects.equals(name, people.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }
}
