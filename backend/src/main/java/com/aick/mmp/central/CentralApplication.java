package com.aick.mmp.central;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableJpaRepositories(basePackages = {
    "com.aick.mmp.central.repository",
    "com.aick.mmp.shared.repository",
    "com.aick.mmp.repository"
})
@EntityScan(basePackages = {
    "com.aick.mmp.shared.model",
    "com.aick.mmp.model"
})
@ComponentScan(basePackages = {
    "com.aick.mmp.central",
    "com.aick.mmp.shared",
    "com.aick.mmp.config",
    "com.aick.mmp.exception",
    "com.aick.mmp.util",
    "com.aick.mmp.service",
    "com.aick.mmp.controller",
    "com.aick.mmp.dto",
    "com.aick.mmp.repository"
})
@Profile("central")
@Slf4j
public class CentralApplication {

    public static void main(String[] args) {
        // Set the active profile to central if not already set
        System.setProperty("spring.profiles.active", "central");
        
        log.info("Starting AICK-MMP Central Server Application");
        SpringApplication.run(CentralApplication.class, args);
    }
}