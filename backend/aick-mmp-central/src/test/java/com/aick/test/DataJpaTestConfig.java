package com.aick.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.Mockito.mock;

/**
 * Minimal Spring Boot configuration for @DataJpaTest tests.
 * Uses @Configuration instead of @SpringBootApplication to avoid being auto-detected
 * as a @SpringBootConfiguration (which would conflict with TestApplication).
 * Does NOT use @ComponentScan — JPA beans are auto-configured by @DataJpaTest.
 */
@Configuration
@EnableAutoConfiguration(exclude = {
    KafkaAutoConfiguration.class,
    RedisAutoConfiguration.class
})
@EntityScan(basePackages = {
    "com.aick.mmp.shared.model",
    "com.aick.mmp.central.entity"
})
@EnableJpaRepositories(basePackages = {
    "com.aick.mmp.central.repository"
})
public class DataJpaTestConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        return mock(JavaMailSender.class);
    }
}
