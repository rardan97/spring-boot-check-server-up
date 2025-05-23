package com.blackcode.app_check_server;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class AppCheckServerApplicationTests {

	@Autowired
    private ApplicationContext applicationContext;

	@Test
    void contextLoads() {
        assertNotNull(applicationContext);
    }

}
