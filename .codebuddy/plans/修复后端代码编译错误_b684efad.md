---
name: 修复后端代码编译错误
overview: 修复后端代码中存在的48个编译错误，包括包名错误、缺失的类和方法、Spring Security API变更等问题
todos:
  - id: fix-package-names
    content: 修复三个shared DTO文件的包名声明（EdgeNodeDTO、UserDTO、CameraDTO）
    status: completed
  - id: fix-batch-operation-dto
    content: 扩展BatchOperationDTO添加操作类型枚举和所需字段
    status: completed
    dependencies:
      - fix-package-names
  - id: fix-repositories
    content: 修复Repository接口，添加缺失的方法定义
    status: completed
  - id: fix-security-config
    content: 更新SecurityConfig使用Spring Security 6的requestMatchers API
    status: completed
  - id: fix-service-impls
    content: 修复CameraServiceImpl和EdgeNodeServiceImpl中的方法调用错误
    status: completed
    dependencies:
      - fix-repositories
  - id: fix-controllers
    content: 修复CameraController和UserController中的枚举引用和DTO方法调用
    status: completed
    dependencies:
      - fix-batch-operation-dto
---

## 用户需求

修复后端代码中的大量编译错误

## 错误分类

### 1. 包名错误（3个文件）

- `EdgeNodeDTO.java`、`UserDTO.java`、`CameraDTO.java` 包名声明为 `com.aick.mmp.dto`，应该是 `com.aick.mmp.shared.dto`

### 2. BatchOperationDTO缺少字段

- 缺少 `operation`、`cameraIds`、`edgeNodeId`、`role` 字段及相关枚举

### 3. DTO缺少内部枚举定义

- CameraDTO需要 `CameraStatus` 枚举
- UserDTO需要 `UserRole` 和 `UserStatus` 枚举

### 4. Repository方法缺失

- `EdgeNodeRepository` 缺少只接受status参数的方法
- `CameraRepository` 缺少 `findByEdgeNodeIdIsNull()` 方法

### 5. EdgeNode模型字段问题

- 没有 `getDiskUsage()` 方法（存在 `storageUsage`）
- CameraServiceImpl中使用了不存在的 `getDiskUsage()`

### 6. Spring Security 6 API变更

- `antMatchers()` 已废弃，需改为 `requestMatchers()`

### 7. EdgeNodeServiceImpl第341行

- findAll方法使用lambda表达式类型不匹配

### 8. 其他导入和类型问题

- Map、HashMap等类型未导入
- 类型转换问题（Double vs Integer）

## 技术栈

- Java 21
- Spring Boot 3.2.5
- Spring Security 6.2.4
- Spring Data JPA

## 修复策略

### 1. 包名修复

修改三个DTO文件的包名声明从 `com.aick.mmp.dto` 改为 `com.aick.mmp.shared.dto`

### 2. BatchOperationDTO扩展

添加操作类型枚举 `BatchOperationType`，并添加所需字段：

- operation: BatchOperationType
- cameraIds: List<Long>
- edgeNodeId: Long
- role: String

### 3. DTO枚举修复

在CameraDTO和UserDTO中引用实体类的枚举类型，而非重新定义

### 4. Repository扩展

- EdgeNodeRepository添加 `List<EdgeNode> findByStatus(EdgeNode.NodeStatus status)` 方法
- CameraRepository添加 `List<Camera> findByEdgeNodeIdIsNull()` 方法

### 5. CameraServiceImpl修复

- 使用 `storageUsage` 替代不存在的 `getDiskUsage()`
- 修正类型转换问题

### 6. SecurityConfig更新

将 `antMatchers()` 替换为Spring Security 6的 `requestMatchers()`

### 7. EdgeNodeServiceImpl修复

修正第341行findAll方法的lambda表达式使用方式，改用JpaSpecificationExecutor的正确API

## 受影响文件列表

```
backend/aick-mmp-shared/src/main/java/com/aick/mmp/shared/dto/
├── EdgeNodeDTO.java      # [MODIFY] 修复包名
├── UserDTO.java          # [MODIFY] 修复包名
└── CameraDTO.java        # [MODIFY] 修复包名

backend/aick-mmp-central/src/main/java/com/aick/mmp/central/dto/
└── BatchOperationDTO.java # [MODIFY] 添加操作枚举和字段

backend/aick-mmp-central/src/main/java/com/aick/mmp/central/repository/
├── EdgeNodeRepository.java # [MODIFY] 添加方法重载
└── CameraRepository.java   # [MODIFY] 添加方法

backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/impl/
├── CameraServiceImpl.java   # [MODIFY] 修复方法调用
└── EdgeNodeServiceImpl.java # [MODIFY] 修复Specification查询

backend/aick-mmp-central/src/main/java/com/aick/mmp/central/config/
└── SecurityConfig.java      # [MODIFY] 更新Security API

backend/aick-mmp-central/src/main/java/com/aick/mmp/central/controller/
├── CameraController.java    # [MODIFY] 修复枚举引用
└── UserController.java      # [MODIFY] 修复枚举引用
```