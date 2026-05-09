## ADDED Requirements

### Requirement: 登录成功同步跳转

登录成功后，系统 SHALL 在 React 渲染周期完成后再执行页面跳转，确保 Sidebar、Header 等布局组件正确渲染。

#### Scenario: 登录成功后正确跳转
- **WHEN** 用户在登录页面输入正确的用户名和密码并点击登录
- **THEN** 系统在当前渲染周期完成后，跳转到 Dashboard 页面，且 Sidebar 和 Header 组件正确显示

#### Scenario: 登录失败不跳转
- **WHEN** 用户在登录页面输入错误的密码并点击登录
- **THEN** 系统显示错误提示，且不执行页面跳转
