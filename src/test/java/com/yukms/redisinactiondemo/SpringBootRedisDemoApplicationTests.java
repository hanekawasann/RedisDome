package com.yukms.redisinactiondemo;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SpringBootRedisDemoApplicationTests {
    @Autowired
    WebApplicationContext webContext;
    private MockMvc mockMvc;

    @Before
    public void setupMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webContext).build();
    }

    @Ignore
    public void test_homepage() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/readingList"))//
            .andExpect(MockMvcResultMatchers.status().isOk())//
            .andExpect(MockMvcResultMatchers.view().name("readingList"))//
            .andExpect(MockMvcResultMatchers.model().attributeExists("books"))//
            .andExpect(MockMvcResultMatchers.model().attribute("books", Matchers.is(Matchers.empty())));
    }
}
