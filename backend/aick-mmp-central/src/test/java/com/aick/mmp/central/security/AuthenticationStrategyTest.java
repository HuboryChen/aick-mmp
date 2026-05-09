package com.aick.mmp.central.security;

import com.aick.mmp.central.security.strategy.*;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthenticationStrategyFactory
 * Tests the factory+strategy pattern for authentication
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationStrategyFactoryTest {

    @Mock
    private HttpServletRequest request;

    private JwtAuthenticationStrategy jwtStrategy;
    private AkskAuthenticationStrategy akskStrategy;
    private AnonymousAuthenticationStrategy anonymousStrategy;
    private AuthenticationStrategyFactory factory;

    private static final String TEST_TOKEN = "test-jwt-token";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "password";
    private static final String TEST_ACCESS_KEY = "ak_test12345678901234567890123456";
    private static final String TEST_SECRET_KEY = "sk_test12345678901234567890123456";
    private static final String TEST_TIMESTAMP = "2026-04-05T10:00:00Z";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    private void createFactoryWithStrategies() {
        jwtStrategy = mock(JwtAuthenticationStrategy.class);
        akskStrategy = mock(AkskAuthenticationStrategy.class);
        anonymousStrategy = mock(AnonymousAuthenticationStrategy.class);

        when(jwtStrategy.getName()).thenReturn("JWT");
        when(jwtStrategy.getPriority()).thenReturn(1);
        when(akskStrategy.getName()).thenReturn("AKSK");
        when(akskStrategy.getPriority()).thenReturn(2);
        when(anonymousStrategy.getName()).thenReturn("ANONYMOUS");
        when(anonymousStrategy.getPriority()).thenReturn(3);

        factory = new AuthenticationStrategyFactory(List.of(anonymousStrategy, jwtStrategy, akskStrategy));
    }

    @Test
    @DisplayName("Should authenticate with JWT strategy when Authorization header present")
    void testJwtStrategyPriority() {
        createFactoryWithStrategies();

        Authentication expectedAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                TEST_USERNAME, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        when(request.getHeader("Authorization")).thenReturn("Bearer " + TEST_TOKEN);
        when(jwtStrategy.supports(request)).thenReturn(true);
        when(jwtStrategy.authenticate(request)).thenReturn(expectedAuth);

        Authentication result = factory.authenticate(request);

        assertNotNull(result);
        assertEquals(TEST_USERNAME, result.getName());
        verify(jwtStrategy).supports(request);
        verify(jwtStrategy).authenticate(request);
    }

    @Test
    @DisplayName("Should skip JWT and try AK/SK when JWT returns null")
    void testAkskFallbackWhenJwtFails() {
        createFactoryWithStrategies();

        Authentication expectedAuth = mock(Authentication.class);
        when(expectedAuth.getName()).thenReturn(TEST_ACCESS_KEY);

        when(request.getHeader("Authorization")).thenReturn(null);
        when(jwtStrategy.supports(request)).thenReturn(true);
        when(jwtStrategy.authenticate(request)).thenReturn(null);
        when(akskStrategy.supports(request)).thenReturn(true);
        when(akskStrategy.authenticate(request)).thenReturn(expectedAuth);

        Authentication result = factory.authenticate(request);

        assertNotNull(result);
        assertEquals(TEST_ACCESS_KEY, result.getName());
        verify(jwtStrategy).authenticate(request);
        verify(akskStrategy).authenticate(request);
    }

    @Test
    @DisplayName("Should use anonymous when all strategies fail")
    void testAnonymousFallbackWhenAllFail() {
        createFactoryWithStrategies();

        Authentication anonymousAuth = mock(Authentication.class);

        when(jwtStrategy.supports(request)).thenReturn(true);
        when(jwtStrategy.authenticate(request)).thenReturn(null);
        when(akskStrategy.supports(request)).thenReturn(true);
        when(akskStrategy.authenticate(request)).thenReturn(null);
        when(anonymousStrategy.supports(request)).thenReturn(true);
        when(anonymousStrategy.authenticate(request)).thenReturn(anonymousAuth);

        Authentication result = factory.authenticate(request);

        assertNotNull(result);
        verify(anonymousStrategy).authenticate(request);
    }

    @Test
    @DisplayName("Should return strategies in priority order")
    void testStrategiesOrderedByPriority() {
        createFactoryWithStrategies();

        List<AuthenticationStrategy> strategies = factory.getStrategies();

        assertEquals(3, strategies.size());
        assertEquals("JWT", strategies.get(0).getName());
        assertEquals("AKSK", strategies.get(1).getName());
        assertEquals("ANONYMOUS", strategies.get(2).getName());
    }

    @Test
    @DisplayName("Should get specific strategy by name")
    void testGetStrategyByName() {
        createFactoryWithStrategies();

        assertNotNull(factory.getStrategy("JWT"));
        assertNotNull(factory.getStrategy("AKSK"));
        assertNotNull(factory.getStrategy("ANONYMOUS"));
        assertNull(factory.getStrategy("UNKNOWN"));
    }
}

/**
 * Unit tests for JwtAuthenticationStrategy
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationStrategyTest {

    @Mock
    private com.aick.mmp.shared.util.JwtUtil jwtUtil;

    @Mock
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    private JwtAuthenticationStrategy strategy;

    private static final String TEST_TOKEN = "test-jwt-token";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "password";

    @BeforeEach
    void setUp() {
        strategy = new JwtAuthenticationStrategy(jwtUtil, userDetailsService);
    }

    @Test
    @DisplayName("Should support request with Bearer token")
    void testSupportsWithBearerToken() {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + TEST_TOKEN);

        assertTrue(strategy.supports(request));
    }

    @Test
    @DisplayName("Should not support request without Authorization header")
    void testDoesNotSupportWithoutAuthHeader() {
        when(request.getHeader("Authorization")).thenReturn(null);

        assertFalse(strategy.supports(request));
    }

    @Test
    @DisplayName("Should authenticate with valid JWT token")
    void testAuthenticateWithValidToken() {
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(TEST_USERNAME)
                .password(TEST_PASSWORD)
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .build();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + TEST_TOKEN);
        when(jwtUtil.validateToken(TEST_TOKEN)).thenReturn(true);
        when(jwtUtil.getUsernameFromToken(TEST_TOKEN)).thenReturn(TEST_USERNAME);
        when(userDetailsService.loadUserByUsername(TEST_USERNAME)).thenReturn(userDetails);

        Authentication result = strategy.authenticate(request);

        assertNotNull(result);
        assertEquals(TEST_USERNAME, result.getName());
    }

    @Test
    @DisplayName("Should return null for invalid token")
    void testAuthenticateWithInvalidToken() {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(jwtUtil.validateToken("invalid-token")).thenReturn(false);

        Authentication result = strategy.authenticate(request);

        assertNull(result);
    }
}
