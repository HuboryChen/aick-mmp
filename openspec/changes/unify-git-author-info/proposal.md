## Why

统一项目仓库的git提交作者信息，确保所有历史commit和未来提交都使用统一的身份标识（Hubory <2574164099@qq.com>）。当前仓库存在两种作者身份混杂，影响提交历史的可读性和可维护性。

## What Changes

- **全局git配置更新**：将user.name设为"Hubory"，user.email设为"2574164099@qq.com"
- **历史commit重写**：使用git-filter-repo重写全部61个commit的作者信息
- **force push**：将重写后的历史强制推送到远程

## Capabilities

### New Capabilities
- `git-author-unification`: 统一git作者信息的完整操作流程

### Modified Capabilities
（无）

## Impact

- **Git历史**：所有commit的author信息将被重写，commit hash将全部改变
- **远程分支**：需要force push，当前ahead of origin/main by 51 commits
- **协作者影响**：其他协作者需要重新clone仓库（如果已拉取）
- **GitHub关联**：PR/Issue关联可能受影响
