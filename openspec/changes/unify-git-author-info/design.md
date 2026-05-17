## Context

项目仓库当前git作者信息不统一：
- 59个commit使用 `chenk-n <chenk-n@glodon.com>`
- 2个commit使用 `HuboryChen <2574164099@qq.com>`
- 全局配置使用 `chenk-n <chenk-n@glodon.com>`

目标：统一为 `Hubory <2574164099@qq.com>`

当前仓库状态：ahead of origin/main by 51 commits，尚未push。

## Goals / Non-Goals

**Goals:**
- 统一全局git配置为 `Hubory` / `2574164099@qq.com`
- 重写全部61个commit的author信息
- 安全推送到远程

**Non-Goals:**
- 不修改committer信息（由GitHub网页创建的commit保留其committer）
- 不涉及其他远程仓库或fork

## Decisions

### 方案选择：git-filter-repo vs git-filter-branch

| 方案 | 优点 | 缺点 |
|------|------|------|
| git-filter-repo | 现代、快速、推荐方案 | 需要安装 |
| git-filter-branch | 内置、无需安装 | 慢、已废弃 |

**决定**：使用 `git-filter-repo`（当前仓库较新，适合使用现代工具）

### 执行命令

```bash
# 1. 安装git-filter-repo（如果未安装）
brew install git-filter-repo  # macOS
# 或从 https://github.com/newren/git-filter-repo 获取

# 2. 设置新作者环境变量
export GIT_AUTHOR_NAME="Hubory"
export GIT_AUTHOR_EMAIL="2574164099@qq.com"
export GIT_COMMITTER_NAME="Hubory"
export GIT_COMMITTER_EMAIL="2574164099@qq.com"

# 3. 更新全局git配置
git config --global user.name "Hubory"
git config --global user.email "2574164099@qq.com"

# 4. 重写历史（替换作者）
git filter-repo --mailmap << 'EOF'
Hubory <2574164099@qq.com> <chenk-n@glodon.com>
Hubory <2574164099@qq.com> <chenk-n@glodon.com>
EOF

# 5. 验证结果
git log --format='%an <%ae>' | sort | uniq -c | sort -rn

# 6. 添加远程并强制推送
git remote add origin <your-repo-url>  # 如果需要
git push --force --all
git push --force --tags
```

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|----------|
| commit hash全部改变 | 这是预期行为，记录在案 |
| force push覆盖远程 | 当前ahead状态，损失可控 |
| 协作者已有本地分支 | 需要通知他们重新clone |
| GitHub PR/Issue关联丢失 | 确认后可接受 |

## Open Questions

1. **远程仓库URL是什么？** 需要用户提供
2. **是否有其他协作者需要通知？** 建议执行前确认
