package com.aick.mmp.central;

import com.aick.mmp.central.channel.NotificationSenderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.reactive.function.client.WebClient;

import static org.mockito.Mockito.mock;

/**
 * Test-specific Spring Boot application configuration for integration tests.
 * CentralApplication has @Profile("central") which prevents it from being used with the "test" profile.
 * Excludes problematic beans that depend on unavailable infrastructure (Kafka, Redis).
 * JPA repositories and entity scanning are handled by Spring Boot auto-configuration.
 */
@SpringBootApplication(exclude = {
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
@ComponentScan(basePackages = {
    "com.aick.mmp.central",
    "com.aick.mmp.shared"
}, excludeFilters = {
    @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.aick\\.mmp\\.central\\.config\\.SecurityConfig"
    ),
    @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.aick\\.mmp\\.central\\.channel\\..*"
    ),
    @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.aick\\.mmp\\.central\\.service\\.RecordingNotificationService"
    ),
    @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.aick\\.mmp\\.central\\.service\\.impl\\.StreamingServiceImpl"
    ),
    @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.aick\\.mmp\\.central\\.service\\.impl\\.CameraServiceImpl"
    ),
    @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.aick\\.mmp\\.central\\.service\\.impl\\.AnalyticsServiceImpl"
    ),
    @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.aick\\.mmp\\.central\\.controller\\.AnalyticsController"
    ),
    @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.aick\\.mmp\\.central\\.controller\\.StreamingController"
    ),
    @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.aick\\.mmp\\.central\\.controller\\.CameraController"
    ),
    @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.aick\\.mmp\\.central\\.service\\.impl\\.RecordingServiceImpl"
    ),
    @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.aick\\.mmp\\.central\\.service\\.impl\\.RecordingScheduleServiceImpl"
    ),
    @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.aick\\.mmp\\.central\\.service\\.impl\\.AlertServiceImpl"
    ),
    @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.aick\\.mmp\\.central\\.service\\.impl\\.MotionEventServiceImpl"
    ),
    @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.aick\\.mmp\\.central\\.service\\.impl\\.EscalationServiceImpl"
    ),
    @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.aick\\.mmp\\.central\\.controller\\.AlertRuleController"
    )
})
public class TestApplication {

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

    @Bean
    @Primary
    public SimpMessagingTemplate simpMessagingTemplate() {
        return mock(SimpMessagingTemplate.class);
    }

    @Bean
    @Primary
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    @Primary
    public NotificationSenderService notificationSenderService() {
        return mock(NotificationSenderService.class);
    }

    @SuppressWarnings("unchecked")
    @Bean
    @Primary
    public RedisTemplate<String, String> redisTemplate() {
        return mock(RedisTemplate.class);
    }
}
