---
name: 修复Lombok @Builder字段初始化警告
overview: 为所有使用@Builder注解且包含字段初始化表达式的类添加@Builder.Default注解，确保字段默认值在Builder模式下正常工作
todos:
  - id: fix-streamsession
    content: 修复StreamSession.java的6个字段，添加@Builder.Default注解
    status: completed
  - id: fix-camera
    content: 修复Camera.java的enabled字段，添加@Builder.Default注解
    status: completed
  - id: fix-user
    content: 修复User.java的enabled和loginFailedCount字段，添加@Builder.Default注解
    status: completed
  - id: fix-edgenode
    content: 修复EdgeNode.java的systemMetrics和enabled字段，添加@Builder.Default注解
    status: completed
---

## 需求概述

修复项目中所有Lombok @Builder注解忽略字段初始化表达式的警告。

## 核心问题

当实体类使用@Builder注解时，如果字段有初始化表达式（如`private boolean enabled = true`），Lombok会忽略这些初始值。要让初始值作为Builder模式下的默认值生效，必须添加@Builder.Default注解。

## 受影响的文件

经过代码探索，发现以下4个实体类共11个字段需要修复：

1. **StreamSession.java** - 6个字段
2. **Camera.java** - 1个字段
3. **User.java** - 2个字段
4. **EdgeNode.java** - 2个字段

这是纯注解添加的技术修复，不涉及业务逻辑变更。

## 技术方案

为所有带有初始化表达式的@Builder类字段添加@Builder.Default注解。

## 修复策略

- 在每个需要修复的字段上方添加`@Builder.Default`注解
- 保持现有的初始化表达式不变
- 确保import语句中包含`lombok.Builder`（已有的@Builder注解确保了这一点）

## 实施要点

- 修改是注解级别的，不改变字段值或类型
- 不需要修改构造器、getter/setter或其他方法
- 修复后Builder模式创建对象时将使用这些默认值