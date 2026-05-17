## ADDED Requirements

### Requirement: Git全局配置更新

系统 SHALL 将全局git用户名设置为 "Hubory"
系统 SHALL 将全局git邮箱设置为 "2574164099@qq.com"

#### Scenario: 更新全局git配置
- **WHEN** 执行 git config 命令更新全局配置
- **THEN** ~/.gitconfig 中 user.name 为 "Hubory"
- **AND** ~/.gitconfig 中 user.email 为 "2574164099@qq.com"

### Requirement: 历史Commit作者重写

系统 SHALL 重写全部61个commit的author信息
系统 SHALL 将所有 author 从 chenk-n@glodon.com 替换为 2574164099@qq.com

#### Scenario: 重写历史author
- **WHEN** 执行 git-filter-repo 重写历史
- **THEN** git log 显示所有 author 为 "Hubory <2574164099@qq.com>"

### Requirement: 远程推送

系统 SHALL 将重写后的本地仓库强制推送到远程
系统 SHALL 推送所有分支和tags

#### Scenario: 强制推送到远程
- **WHEN** 执行 git push --force --all && git push --force --tags
- **THEN** 远程仓库所有commit的author统一为 Hubory
