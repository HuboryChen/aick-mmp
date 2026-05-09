# Java版本升级说明

## 文档信息

| 项目 | 内容 |
|------|------|
| **文档名称** | AICK-MMP Java版本升级说明 |
| **升级日期** | 2026-02-08 |
| **升级人** | 产品经理团队 |

---

## 升级概述

将AICK-MMP项目的Java版本从Java 8升级到Java 21 (LTS长期支持版本),并同步升级相关框架组件以适配Java 21。

---

## 升级内容

### 一、需求文档更新

#### 1.1 技术栈版本更新

**文件**: `spec/Me2AI/需求描述.md`

**更新内容**:
1. **后端组件技术栈**
   - 新增: Java 21 (采用LTS长期支持版本)
   - Spring Boot 2.7.18 → Spring Boot 3.2.5
   - Spring Security 6.2.4 (支持Java 21)
   - Spring Data JPA 3.2.5 (支持Java 21)
   - Spring Cloud 2023.0.x (支持Spring Boot 3.2.5)

2. **总体架构图**
   - 更新业务服务层为: Java 21 + Spring Boot 3.2.5

3. **测试工具**
   - Spring Boot Test 3.2.5 (与主版本保持一致)

**升级原因**: Java 8已于2019年停止免费更新,Java 21是最新LTS版本,提供更好的性能和新特性。

---

### 二、README文档更新

**文件**: `README.md`

**更新内容**:
1. **版本徽章**
   - Java 8 → Java 21
   - Spring Boot 2.7.18 → Spring Boot 3.2.5

2. **架构图**
   - 后端服务1/2的Spring Boot版本更新为3.2.5

3. **技术栈表格**
   - 业务服务层: Java 21, Spring Boot 3.2.5, Spring Security 6.2.4, Spring Data JPA 3.2.5

4. **环境要求**
   - Java 8 或更高版本 → Java 21 (LTS长期支持版本)

---

### 三、Maven构建文件更新

#### 3.1 父POM文件 (aick-mmp-parent/pom.xml)

**更新内容**:
1. **Spring Boot Parent版本**
   ```xml
   <version>2.7.18</version>
   ↓
   <version>3.2.5</version>
   ```

2. **Java版本属性**
   ```xml
   <java.version>8</java.version>
   <maven.compiler.source>8</maven.compiler.source>
   <maven.compiler.target>8</maven.compiler.target>
   ↓
   <java.version>21</java.version>
   <maven.compiler.source>21</maven.compiler.source>
   <maven.compiler.target>21</maven.compiler.target>
   ```

3. **Spring Cloud版本**
   ```xml
   <spring-cloud.version>2021.0.8</spring-cloud.version>
   ↓
   <spring-cloud.version>2023.0.3</spring-cloud.version>
   ```

4. **依赖版本升级**

| 依赖 | 原版本 | 新版本 | 升级原因 |
|------|--------|--------|----------|
| **mysql-connector-j** | 8.2.0 | 8.3.0 | 支持Java 21 |
| **h2** | 2.2.224 | 2.3.230 | 支持Java 21 |
| **spring-kafka** | 2.9.13 | 3.2.3 | 支持Spring Boot 3.x |
| **spring-boot-starter-data-redis** | 2.7.18 | 3.2.5 | 与Spring Boot主版本一致 |
| **lombok** | 1.18.30 | 1.18.32 | 支持Java 21 |
| **modelmapper** | 3.1.1 | 3.2.1 | 更新版本 |
| **commons-lang3** | 3.13.0 | 3.15.0 | 更新版本 |
| **hutool-all** | 5.8.24 | 5.8.28 | 更新版本 |
| **jackson-core** | 2.15.3 | 2.17.2 | 更新版本 |
| **jackson-databind** | 2.15.3 | 2.17.2 | 更新版本 |
| **jackson-annotations** | 2.15.3 | 2.17.2 | 更新版本 |
| **spring-boot-starter-test** | 2.7.18 | 3.2.5 | 与Spring Boot主版本一致 |
| **springdoc-openapi-ui** | 1.7.0 | 2.5.0 | 支持Spring Boot 3.x |
| **springdoc-openapi-webmvc-core** | 1.7.0 | 2.5.0 | 支持Spring Boot 3.x |
| **jjwt** | 0.11.5 | 0.12.5 | 更新版本 |

#### 3.2 后端根POM文件 (backend/pom.xml)

**更新内容**:
```xml
<properties>
    <maven.compiler.source>8</maven.compiler.source>
    <maven.compiler.target>8</maven.compiler.target>
    ↓
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
</properties>
```

编译插件配置:
```xml
<configuration>
    <source>8</source>
    <target>8</target>
    ↓
    <source>21</source>
    <target>21</target>
</configuration>
```

---

## Java 21 新特性

### 1. 性能提升
- **虚拟线程(Virtual Threads)**: 改善并发性能
- **字符串模板(String Templates)**: 更简洁的字符串拼接
- **记录模式(Record Patterns)**: 增强的模式匹配
- **分代ZGC(ZGC Generational)**: 改进的垃圾收集器

### 2. 新增API
- **SequencedCollection**: 有序集合接口
- **Switch模式匹配**: 增强的switch表达式
- **结构化并发**: 更好的并发编程支持

### 3. LTS长期支持
- 免费更新支持到2031年10月
- 企业级稳定性和可靠性
- 适合生产环境使用

---

## Spring Boot 3.2.5 主要变化

### 1. 依赖要求
- **最低Java版本**: Java 17
- **推荐Java版本**: Java 21
- **Jakarta EE**: 从Java EE迁移到Jakarta EE

### 2. 主要更新
- **Spring Framework 6.1.x**: 支持Java 21新特性
- **Spring Security 6.2.x**: 增强的安全性
- **Spring Data JPA 3.2.x**: 改进的数据访问
- **Spring Cloud 2023.0.x**: 支持Spring Boot 3.x

### 3. 包名变更
- **javax.persistence → jakarta.persistence**: JPA相关包
- **javax.servlet → jakarta.servlet**: Servlet相关包
- **javax.validation → jakarta.validation**: 验证相关包

---

## 影响评估

### 1. 代码兼容性

| 影响项 | 影响程度 | 说明 |
|--------|----------|------|
| **javax → jakarta包名变更** | 中 | 需要批量替换import语句 |
| **废弃API** | 低 | 部分API已废弃,需要替换 |
| **第三方库兼容性** | 中 | 需要升级所有第三方依赖到兼容Java 21的版本 |

### 2. 性能影响

| 性能指标 | 预期提升 | 说明 |
|----------|----------|------|
| **启动时间** | -10% ~ -15% | JIT编译器优化 |
| **内存使用** | -5% ~ -10% | 垃圾收集器优化 |
| **并发性能** | +20% ~ +30% | 虚拟线程支持 |
| **吞吐量** | +10% ~ -20% | 综合性能提升 |

### 3. 开发环境影响

| 环境项 | 要求 | 说明 |
|--------|------|------|
| **JDK** | JDK 21 | 需要安装JDK 21 |
| **IDE** | 支持Java 21 | IntelliJ IDEA 2023.2+, Eclipse 2023-09+ |
| **Maven** | 3.9.0+ | 推荐使用最新版本 |
| **构建工具** | Maven 3.9.0+ | 与JDK版本匹配 |

---

## 迁移步骤

### 1. 准备阶段
1. 安装JDK 21
2. 更新IDE到支持Java 21的版本
3. 更新Maven到3.9.0+
4. 备份当前项目

### 2. 代码修改阶段
1. 批量替换javax包名为jakarta:
   - javax.persistence → jakarta.persistence
   - javax.servlet → jakarta.servlet
   - javax.validation → jakarta.validation
   - javax.annotation → jakarta.annotation

2. 替换示例:
   ```java
   // 旧版本
   import javax.persistence.Entity;
   import javax.servlet.http.HttpServletRequest;
   import javax.validation.constraints.NotNull;
   
   // 新版本
   import jakarta.persistence.Entity;
   import jakarta.servlet.http.HttpServletRequest;
   import jakarta.validation.constraints.NotNull;
   ```

3. 检查并更新第三方依赖到兼容Java 21的版本

### 3. 测试阶段
1. 运行单元测试
2. 运行集成测试
3. 执行性能测试
4. 进行安全测试

### 4. 部署阶段
1. 更新生产环境JDK到21
2. 更新部署脚本中的JAVA_HOME
3. 执行部署
4. 验证系统正常运行

---

## 注意事项

### 1. 包名变更
Spring Boot 3.x将所有javax.*包迁移到jakarta.*包,需要批量替换import语句。

### 2. 库兼容性
确保所有第三方库都支持Java 21,不支持的库需要升级或替换。

### 3. 配置变更
部分配置可能需要调整,特别是与Servlet、JPA相关的配置。

### 4. 性能监控
升级后密切监控系统性能,及时发现和解决性能问题。

---

## 验收标准

| 验收项 | 验收标准 |
|--------|----------|
| **编译成功** | 所有模块能够成功编译 |
| **单元测试通过** | 所有单元测试通过 |
| **集成测试通过** | 所有集成测试通过 |
| **功能测试通过** | 所有功能测试通过 |
| **性能达标** | 性能指标不低于升级前或有所提升 |
| **部署成功** | 能够成功部署到环境 |

---

## 回滚计划

如果升级后出现严重问题,可以按以下步骤回滚:

1. 回滚代码到升级前版本
2. 回滚POM文件到升级前版本
3. 回滚生产环境JDK到Java 8
4. 验证系统恢复正常

---

## 参考资料

1. [Java 21 官方文档](https://docs.oracle.com/en/java/javase/21/)
2. [Spring Boot 3.2.5 官方文档](https://docs.spring.io/spring-boot/docs/3.2.5/reference/html/)
3. [Jakarta EE 迁移指南](https://jakarta.ee/)

---

**升级完成日期**: 2026-02-08
**升级说明生成日期**: 2026-02-08
