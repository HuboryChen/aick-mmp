package com.aick.test;

import com.aick.mmp.central.channel.NotificationSenderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
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
 * Focused Spring Boot test configuration for EdgeNodeService integration tests.
 * Marked as @SpringBootConfiguration so that when specified via
 * @SpringBootTest(classes = ServiceIntegrationTestConfig.class), Spring uses this
 * instead of auto-detecting TestApplication.
 */
@SpringBootConfiguration
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
@ComponentScan(basePackages = {
    "com.aick.mmp.central",
    "com.aick.mmp.shared"
}, excludeFilters = {
    // Exclude classes that define duplicate beans or depend on excluded infrastructure
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
        pattern = "com\\.aick\\.mmp\\.central\\.controller\\..*"
    ),
    @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.aick\\.mmp\\.central\\.engine\\..*"
    ),
    @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.aick\\.mmp\\.central\\.security\\..*"
    ),
    @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.aick\\.mmp\\.central\\.task\\..*"
    ),
    @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.aick\\.mmp\\.central\\.scheduler\\..*"
    ),
    @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.aick\\.mmp\\.central\\.websocket\\..*"
    ),
    @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.aick\\.mmp\\.central\\.TestApplication"
    ),
    @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.aick\\.test\\..*"
    ),
    // Exclude service implementations that require unavailable infrastructure
    @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.aick\\.mmp\\.central\\.service\\.impl\\.(?!EdgeNodeServiceImpl|NetworkMonitorServiceImpl|EdgeNodeFailoverServiceImpl|CdnNodeServiceImpl).*"
    ),
    // Exclude service interfaces that depend on excluded infrastructure
    @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.aick\\.mmp\\.central\\.service\\.Alert.*Service"
    ),
    @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.aick\\.mmp\\.central\\.service\\.RecordingNotificationService"
    ),
    @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.aick\\.mmp\\.central\\.service\\.AlertRuleTestService"
    ),
    @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.aick\\.mmp\\.central\\.service\\.JanusHealthIndicator"
    )
})
public class ServiceIntegrationTestConfig {

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
