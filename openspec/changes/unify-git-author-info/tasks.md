## 1. 准备工作

- [ ] 1.1 检查 git-filter-repo 是否已安装，如未安装则安装
- [ ] 1.2 确认远程仓库URL

## 2. 更新全局Git配置

- [ ] 2.1 设置 GIT_AUTHOR_NAME="Hubory"
- [ ] 2.2 设置 GIT_AUTHOR_EMAIL="2574164099@qq.com"
- [ ] 2.3 设置 GIT_COMMITTER_NAME="Hubory"
- [ ] 2.4 设置 GIT_COMMITTER_EMAIL="2574164099@qq.com"
- [ ] 2.5 执行 git config --global user.name "Hubory"
- [ ] 2.6 执行 git config --global user.email "2574164099@qq.com"

## 3. 重写Git历史

- [ ] 3.1 确认当前工作区无未提交的更改
- [ ] 3.2 备份远程引用（可选）
- [ ] 3.3 执行 git filter-repo --mailmap 重写author信息
- [ ] 3.4 验证所有commit的author已统一

## 4. 推送到远程

- [ ] 4.1 确认远程仓库已添加
- [ ] 4.2 执行 git push --force --all 推送所有分支
- [ ] 4.3 执行 git push --force --tags 推送所有tags
- [ ] 4.4 验证远程仓库commit信息
