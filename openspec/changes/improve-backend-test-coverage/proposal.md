## Why

根据任务规划，MVP版本要求后端单元测试覆盖率达到80%，但当前覆盖率仅约10%（31个测试文件，322个Java源文件）。测试覆盖率不足严重影响软件质量和交付风险，需要立即补充测试用例。

## What Changes

- 新增Service层单元测试，覆盖所有核心业务逻辑
- 新增Repository层单元测试，验证数据访问层
- 新增Controller层单元测试，验证API端点
- 新增工具类单元测试（加密、签名、JWT等）
- 建立测试覆盖率基线和目标

## Capabilities

### New Capabilities

- `backend-unit-testing`: 后端单元测试规范和覆盖率目标定义

### Modified Capabilities

<!-- 无需修改现有capabilities -->

## Impact

- **影响范围**: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/`、`repository/`、`controller/`
- **依赖**: Spring Boot Test, JUnit 5, Mockito, AssertJ
- **测试文件位置**: `backend/aick-mmp-central/src/test/java/com/aick/mmp/central/`
