# CFXMap

[中文](#中文)

CFXMap is a Web3 wallet project for the Conflux ecosystem. The repository already includes a working mobile app and backend services, rather than only early-stage placeholder directories.

Please note that **BSIM support has not been implemented yet and remains part of the future roadmap**.

## Current Features

### 1. Mobile App (`app/cfxmap`)

The repository already contains a `uni-app` based mobile application with the following features and pages:

- Authentication: login, registration, verification code, forgot password
- Wallet: wallet home, asset details, transaction history, transaction progress
- Transfer: send assets, receive QR code
- NFT: gallery, detail page, 3D viewer
- Community: community feed, post details, and posting flow, currently in internal testing
- Profile: profile center, security center, bind email, change username, change password, transaction password
- Wallet Management: import, export, remove, and manage wallets
- Other: Conflux guide page, force update page, API settings, custody wallet notice

### 2. Backend Services (`backend`)

The repository also includes a complete Java multi-module backend. The main application is `web3-admin`, with `web3-wallet` integrated as the wallet business module. Current backend capabilities include:

- App user authentication and registration/login
- Wallet account creation, binding, import, export, query, and removal
- Wallet asset and transaction record APIs
- Community APIs for posts, comments, topics, likes, and favorites, with community-related features currently in internal testing
- App version configuration and force update configuration
- Web3 guide configuration
- New user reward logic and scheduled jobs
- Admin dashboard statistics APIs

## Roadmap

- Add BSIM-related capabilities
- Continue improving on-chain wallet interaction and asset management experience

## 中文

CFXMap 是一个面向 Conflux 生态的 Web3 钱包项目。当前仓库已经包含可持续迭代的移动端应用与后端服务，不再只是早期的占位目录。

需要特别说明的是：**BSIM 相关能力目前尚未落地，仍属于后续规划功能。**

## 当前已有功能

### 1. 移动端 App（`app/cfxmap`）

项目内已有基于 `uni-app` 的应用工程，现阶段包含以下页面与业务能力：

- 登录注册：登录、注册、验证码校验、找回密码
- 钱包相关：钱包首页、资产详情、交易记录、交易进度
- 转账收款：发送资产、收款二维码
- NFT：NFT 画廊、详情页、3D 查看页
- 社区：社区列表、帖子详情、发帖，当前仍处于内部测试阶段
- 用户中心：个人中心、安全中心、绑定邮箱、修改用户名、修改密码、交易密码
- 钱包管理：钱包导入、导出、删除、管理
- 其他：Conflux 指南页、版本强更页、API 设置页、托管钱包提示页

### 2. 后端服务（`backend`）

仓库内已有完整的 Java 多模块后端工程，主应用为 `web3-admin`，并集成了 `web3-wallet` 业务模块。当前代码已覆盖以下主要能力：

- App 用户认证与注册登录
- 钱包账户创建、绑定、导入、导出、查询、删除
- 钱包资产与交易记录接口
- 社区帖子、评论、话题、点赞、收藏等接口，当前配套社区功能仍处于内部测试阶段
- App 版本配置与强制更新配置
- Web3 指南配置
- 新用户奖励相关逻辑与定时任务
- 管理端统计看板接口

## 后续规划

- 接入 BSIM 相关能力
- 持续完善链上钱包交互与资产管理体验
