package com.yukms.redisinactiondemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
public class SpringBootRedisDemoApplication {

    @RequestMapping("/")
    public String homePage() {
        return "Hello World !";
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(SpringBootRedisDemoApplication.class, args);
    }
}