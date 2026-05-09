# Backend Test Coverage Improvement Design

**Date**: 2026-05-09  
**Topic**: Backend Test Coverage Improvement  
**Version**: 1.0

## 1. Context

### Background
Current backend project has 322 Java source files with only 31 test files, achieving ~10% test coverage. The MVP version requires 80% coverage to ensure software quality and reduce delivery risks. This design outlines a comprehensive approach to systematically improve test coverage across all layers.

### Current Testing Framework
- JUnit 5 for testing
- Mockito for mocking dependencies
- AssertJ for fluent assertions
- Spring Boot Test for integration testing
- JaCoCo for code coverage reporting

### Target Coverage Goals
| Layer | Target Coverage |
|-------|----------------|
| Repository | 80% - Pure JPA operations |
| Service | 70% - Business logic, integrations |
| Controller | 60% - API endpoints, security |
| Utils | 90% - Helper methods |

## 2. Architecture Overview

### Testing Pyramid Approach
```
                ┌─ Controller Tests (60%) ─┐
                │  WebMvcTest, MockMvc     │
                └──────────────────────────┘
        ┌─ Service Tests (70%) ───────────┐
        │  @SpringBootTest, Real Database  │
        └────────────────────────────────┘
┌─ Repository Tests (80%) ──────────────┐
│  @DataJpaTest, In-memory H2 Database │
└───────────────────────────────────────┘
```

### Test Implementation Strategy
1. **Repository Layer**: JPA tests with in-memory H2 (unit level)
2. **Service Layer**: Integration tests with Spring Boot test context (business logic)
3. **Controller Layer**: Web MVC tests with HTTP assertions (API level)

## 3. Per-Service Test Package Structure

```
src/test/java/com/aick/mmp/central/
├── common/                           # Shared test utilities
│   ├── BaseIntegrationTest.java
│   ├── TestDataProvider.java
│   └── DatabaseCleaner.java
│
├── repository/                       # Repository tests (JPA)
│   ├── AlertRuleRepositoryTest.java
│   ├── CameraRepositoryTest.java
│   ├── EdgeNodeRepositoryTest.java
│   ├── RecordingRepositoryTest.java
│   ├── RegionRepositoryTest.java
│   └── CdnNodeRepositoryTest.java
│
├── service/                          # Service tests (Integration)
│   ├── alert/
│   │   ├── AlertRuleServiceIT.java
│   │   └── AlertRuleServiceIntegrationTest.java
│   ├── camera/
│   │   ├── CameraServiceIT.java
│   │   └── CameraServiceIntegrationTest.java
│   ├── edge/
│   │   ├── EdgeNodeServiceIT.java
│   │   └── EdgeNodeServiceIntegrationTest.java
│   ├── streaming/
│   │   ├── StreamingServiceIT.java
│   │   └── StreamingServiceIntegrationTest.java
│   ├── auth/
│   │   ├── AuthServiceIT.java
│   │   └── AuthServiceIntegrationTest.java
│   ├── region/
│   │   ├── RegionServiceIT.java
│   │   └── RegionServiceIntegrationTest.java
│   ├── recording/
│   │   ├── RecordingServiceIT.java
│   │   └── RecordingServiceIntegrationTest.java
│   ├── cdn/
│   │   ├── CdnNodeServiceIT.java
│   │   └── CdnNodeServiceIntegrationTest.java
│   ├── user/
│   │   ├── UserServiceIT.java
│   │   └── UserServiceIntegrationTest.java
│   └── system/
│       ├── SystemAppServiceIT.java
│       └── SystemAppServiceIntegrationTest.java
│
└── controller/                       # Controller tests (Web MVC)
    ├── alert/
    │   ├── AlertRuleControllerIT.java
    │   └── AlertRuleControllerIntegrationTest.java
    ├── camera/
    │   ├── CameraControllerIT.java
    │   └── CameraControllerIntegrationTest.java
    ├── edge/
    │   ├── EdgeNodeControllerIT.java
    │   └── EdgeNodeControllerIntegrationTest.java
    ├── streaming/
    │   ├── StreamingControllerIT.java
    │   └── StreamingControllerIntegrationTest.java
    ├── auth/
    │   ├── AuthControllerIT.java
    │   └── AuthControllerIntegrationTest.java
    ├── region/
    │   ├── RegionControllerIT.java
    │   └── RegionControllerIntegrationTest.java
    ├── recording/
    │   ├── RecordingControllerIT.java
    │   └── RecordingControllerIntegrationTest.java
    ├── cdn/
    │   ├── CdnNodeControllerIT.java
    │   └── CdnNodeControllerIntegrationTest.java
    └── user/
        ├── UserControllerIT.java
        └── UserControllerIntegrationTest.java
```

## 4. Test Implementation Priority

### P0 Priority - MVP Core Services

1. **AlertRuleService**
   - Repository: JPA operations (CRUD, soft delete)
   - Service: Business rules, threshold evaluation, status toggling
   - Controller: API validation, authorization checks
   - **Rationale**: Core alert functionality for video surveillance

2. **EdgeNodeService**
   - Repository: Node status tracking, heartbeat management
   - Service: Registration logic, health checks, failover
   - Controller: Node management API
   - **Rationale**: Edge node management is fundamental to distributed architecture

3. **StreamingService**
   - Service: Session management, Janus client integration
   - Controller: Stream control endpoints
   - **Rationale**: Real-time video streaming is core feature

4. **AuthService**
   - Service: JWT token management, authentication
   - Controller: Login/logout, token refresh
   - **Rationale**: Security foundation for all operations

5. **CameraService**
   - Service: Camera lifecycle, batch operations, status management
   - Controller: CRUD operations, batch operations
   - **Rationale**: Core business asset management

### P1 Priority - Supporting Services

6. **RegionService** - Regional management
7. **CdnNodeService** - CDN node management
8. **RecordingService** - Recording management
9. **UserService** - User management
10. **SystemAppService** - System app registration

### P2 Priority - Additional Services

11. Other utility and supporting services as time permits

## 5. Test Implementation Guidelines

### Repository Layer Tests (`@DataJpaTest`)
```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("AlertRule Repository Tests")
public class AlertRuleRepositoryTest {
    
    @Autowired
    private TestEntityManager em;
    
    @Autowired
    private AlertRuleRepository repository;
    
    @Test
    @DisplayName("should save and retrieve alert rule")
    void shouldSaveAndRetrieveAlertRule() {
        // Given
        AlertRule rule = createTestAlertRule();
        
        // When
        AlertRule saved = repository.save(rule);
        AlertRule retrieved = repository.findById(saved.getId()).orElse(null);
        
        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getName()).isEqualTo(rule.getName());
    }
}
```

### Service Layer Tests (`@SpringBootTest`)
```java
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("AlertRule Service Integration Tests")
public class AlertRuleServiceIntegrationTest extends BaseIntegrationTest {
    
    @Autowired
    private AlertRuleService alertRuleService;
    
    @Autowired
    private AlertRuleRepository alertRuleRepository;
    
    @Test
    @DisplayName("should create alert rule with valid data")
    @Transactional
    void shouldCreateAlertRuleWithValidData() {
        // Given
        CreateAlertRuleRequest request = createValidRequest();
        
        // When
        AlertRuleDTO result = alertRuleService.createAlertRule(request);
        
        // Then
        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo(request.getName());
        
        // Verify database state
        AlertRule saved = alertRuleRepository.findById(result.getId()).orElse(null);
        assertThat(saved).isNotNull();
        assertThat(saved.getEnabled()).isTrue();
    }
}
```

### Controller Layer Tests (`@WebMvcTest`)
```java
@WebMvcTest(AlertRuleController.class)
@AutoConfigureMockMvc
@DisplayName("AlertRule Controller Tests")
public class AlertRuleControllerIT {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private AlertRuleService alertRuleService;
    
    @Test
    @DisplayName("should return 201 when creating alert rule")
    void shouldReturn201WhenCreatingAlertRule() throws Exception {
        // Given
        CreateAlertRuleRequest request = createValidRequest();
        AlertRuleDTO response = createValidResponse();
        
        when(alertRuleService.createAlertRule(any())).thenReturn(response);
        
        // When & Then
        mockMvc.perform(post("/api/alert-rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(response.getId()));
    }
}
```

## 6. Test Data Management

### TestDataProvider Component
```java
@Component
public class TestDataProvider {
    
    // AlertRule test data builders
    public AlertRule createAlertRule(...) { 
        return AlertRule.builder()
                .name("Test Alert Rule")
                .type("THRESHOLD")
                .enabled(true)
                .build();
    }
    
    // EdgeNode test data builders
    public EdgeNode createEdgeNode(...) { 
        return EdgeNode.builder()
                .nodeName("Test Edge Node")
                .nodeCode("TEST-001")
                .status(EdgeNode.NodeStatus.ONLINE)
                .build();
    }
    
    // Common test scenarios
    public TestScenario getComplexScenario() { 
        // Multi-entity test data setup
    }
}
```

### Database Cleaner Utility
- Automatic transaction rollback for service tests using `@Transactional`
- Manual cleanup for multi-service interaction tests
- Test isolation between test methods

## 7. Test Naming Conventions

| Test Type | Suffix | Description |
|-----------|--------|-------------|
| Unit Tests | `Test.java` | Simple tests with mocks |
| Integration Tests | `IT.java` | Integration tests with Spring context |
| Service Tests | `ServiceIntegrationTest.java` | Business logic tests |
| Controller Tests | `ControllerIT.java` | Web layer tests |

## 8. CI/CD Integration

### Test Execution Strategy
- **Fast Unit Tests** (Repository): Run on every commit
- **Service Integration Tests**: Run on pull requests
- **Full Test Suite**: Run on merge to main
- **Coverage Report**: Generate JaCoCo report after test execution

### Maven Configuration
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.7</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## 9. Testing Best Practices

### Service Layer Best Practices
- Use `@DataJpaTest` with H2 for repository tests
- Use `@SpringBootTest` for service integration tests
- Mock external services (Kafka, Redis) but use real database
- Test business rules without HTTP layer
- Use `@TestConfiguration` for custom test beans

### Controller Layer Best Practices
- Use `@WebMvcTest` for focused controller testing
- Mock service layer using `@MockBean`
- Test HTTP status codes, response formats, security
- Include request validation tests
- Use `@AutoConfigureMockMvc` for MockMvc setup

### Common Patterns
- Use `@Transactional` for service tests (rollback after each test)
- Use `@Sql` for complex test data setup when needed
- Use AssertJ for fluent assertions
- Use `@DisplayName` for readable test names

## 10. Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Test flakiness due to external dependencies | Mock all external services, use in-memory databases |
| Slow test execution due to complex setup | Use parallel test execution, limit integration tests |
| Test brittleness from implementation details | Test behavior, not implementation |
| Database state contamination between tests | Use transaction rollback or explicit cleanup |
| Coverage gaps in complex business logic | Focus integration tests on critical paths |

## 11. Success Metrics

### Coverage Targets
- **Overall**: 80% line coverage
- **P0 Services**: 90% coverage
- **Repository Layer**: 80% coverage
- **Controller Layer**: 60% coverage

### Quality Metrics
- **Test Stability**: >95% pass rate
- **Test Speed**: <2 seconds per test average
- **Code Coverage**: Continuous improvement toward targets

---

**This design provides a comprehensive framework for systematically improving backend test coverage while maintaining testability and ensuring critical business logic is thoroughly tested.**