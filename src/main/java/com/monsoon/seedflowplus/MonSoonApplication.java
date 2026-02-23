package com.monsoon.seedflowplus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class MonSoonApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonSoonApplication.class, args);
    }

}
