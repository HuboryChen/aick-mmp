# Backend Unit Testing Specification

## ADDED Requirements

### Requirement: Service Layer Test Coverage

Service层测试覆盖率 SHALL 达到 50% 以上。

#### Scenario: AlertRuleService Coverage
- **WHEN** 运行JaCoCo覆盖率报告
- **THEN** AlertRuleService及其依赖的覆盖率 >= 50%

#### Scenario: EdgeNodeService Coverage
- **WHEN** 运行JaCoCo覆盖率报告
- **THEN** EdgeNodeService及其依赖的覆盖率 >= 50%

#### Scenario: StreamingService Coverage
- **WHEN** 运行JaCoCo覆盖率报告
- **THEN** StreamingService及其依赖的覆盖率 >= 50%

### Requirement: Repository Layer Test Coverage

Repository层测试覆盖率 SHALL 达到 60% 以上。

#### Scenario: CRUD Repository Coverage
- **WHEN** 测试所有Repository的CRUD操作
- **THEN** Repository方法覆盖率 >= 60%

### Requirement: Controller Layer Test Coverage

Controller层测试覆盖率 SHALL 达到 40% 以上。

#### Scenario: API Endpoint Coverage
- **WHEN** 测试所有API端点
- **THEN** Controller方法覆盖率 >= 40%

### Requirement: Test Quality Standards

测试用例 SHALL 遵循以下标准：

#### Scenario: Test Naming Convention
- **WHEN** 编写测试
- **THEN** 测试方法命名遵循 `should<Expected>When<Condition>` 模式

#### Scenario: Test Isolation
- **WHEN** 运行单元测试
- **THEN** 测试之间相互独立，不依赖执行顺序

#### Scenario: Meaningful Assertions
- **WHEN** 编写断言
- **THEN** 断言消息清晰描述预期行为

### Requirement: Coverage Reporting

覆盖率报告 SHALL 能被生成和分析。

#### Scenario: JaCoCo Report Generation
- **WHEN** 执行 `mvn verify`
- **THEN** 在 `target/site/jacoco/index.html` 生成覆盖率报告

#### Scenario: Coverage Baseline
- **WHEN** 首次运行测试
- **THEN** 记录当前覆盖率作为基线
