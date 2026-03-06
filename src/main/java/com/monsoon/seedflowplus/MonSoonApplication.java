package com.monsoon.seedflowplus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MonSoonApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonSoonApplication.class, args);
    }

}
