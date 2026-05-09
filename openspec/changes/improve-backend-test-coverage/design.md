## Context

### 背景
当前后端项目共322个Java源文件，仅有31个测试文件，覆盖率约10%。根据任务规划要求达到80%覆盖率，需要系统性地补充测试用例。

### 当前测试状态
```
backend/aick-mmp-central/src/test/java/com/aick/mmp/central/
├── controller/     (2个测试)
├── integration/    (9个测试)
├── model/          (1个测试)
├── security/       (1个测试)
├── service/        (14个测试)
└── service/recording/ (4个测试)
```

### 已有测试框架
- JUnit 5
- Mockito (Mock依赖)
- AssertJ (断言)
- Spring Boot Test (集成测试)

## Goals / Non-Goals

**Goals:**
- Service层测试覆盖率 > 50%
- Repository层测试覆盖率 > 60%
- Controller层测试覆盖率 > 40%
- 工具类测试覆盖率 > 80%
- 确保核心业务逻辑被充分测试

**Non-Goals:**
- 不包含E2E测试
- 不包含性能测试
- 不强制要求私有方法测试
- 不追求100%覆盖率

## Decisions

### 1. 测试优先级策略

**决策**: 按模块优先级排序补充测试

| 优先级 | 模块 | 理由 |
|--------|------|------|
| P0 | AlertRuleService | 核心告警逻辑 |
| P0 | EdgeNodeService | 边缘节点管理 |
| P0 | StreamingService | 视频流控制 |
| P1 | RegionService | 区域管理 |
| P1 | CdnNodeService | CDN节点管理 |
| P1 | RecordingService | 录像管理 |
| P2 | 其他Service | 辅助功能 |

### 2. 测试数据策略

**决策**: 使用 @MockBean 和 @DataJpaTest 分离测试

```java
// Service层测试 - 使用Mock
@ExtendWith(MockitoExtension.class)
class AlertRuleServiceTest {
    @Mock private AlertRuleRepository repository;
    @InjectMocks private AlertRuleServiceImpl service;
}

// Repository层测试 - 使用内存数据库
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AlertRuleRepositoryTest {
    @Autowired private TestEntityManager em;
}
```

### 3. 覆盖率工具

**决策**: 使用 JaCoCo 进行覆盖率报告

pom.xml配置已有，无需额外配置。

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| 测试编写耗时较长 | 按优先级分批完成 |
| Mock过度导致测试脆弱 | 优先使用真实对象测试 |
| 测试与实现耦合 | 通过接口测试而非实现细节 |
