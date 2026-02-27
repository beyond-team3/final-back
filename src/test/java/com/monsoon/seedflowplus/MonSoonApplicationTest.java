package com.monsoon.seedflowplus;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MonSoonApplicationTest {

    @Autowired
    private Environment env;

    @Test
    void contextLoads() {

    }

    @Test
    void printProfile() {
        System.out.println("### Active Profiles: " + Arrays.toString(env.getActiveProfiles()));
    }

}
