<h1 align="center">
  <br>
  局域网文件传输系统
  <br>
</h1>

<p align="center">
  <strong>LAN File Transfer - 家庭局域网多设备文件快速传输解决方案</strong>
</p>

<p align="center">
  <a href="#功能特性">功能特性</a> •
  <a href="#系统架构">系统架构</a> •
  <a href="#项目结构">项目结构</a> •
  <a href="#快速开始">快速开始</a> •
  <a href="#技术栈">技术栈</a> •
  <a href="#截图预览">截图预览</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/version-1.0.0-blue.svg" alt="Version">
  <img src="https://img.shields.io/badge/license-Apache%202.0-green.svg" alt="License">
  <img src="https://img.shields.io/badge/python-3.9+-yellow.svg" alt="Python">
  <img src="https://img.shields.io/badge/platform-iOS%20%7C%20Android%20%7C%20HarmonyOS-lightgrey.svg" alt="Platform">
</p>

---

## 项目简介

**局域网文件传输系统** 是一个专为家庭局域网环境设计的跨平台文件传输解决方案。支持 iOS、Android、HarmonyOS 等多种设备，可快速传输照片、视频等大文件到家庭 NAS 或电脑。

### 为什么需要它？

- 手机存储空间告急，需要快速备份照片视频
- 不想依赖云服务，数据本地化更安全
- 局域网传输速度快，不受网速限制
- 支持超大文件（1GB+），传输不中断

---

## 功能特性

### 核心功能

| 功能 | 描述 |
|------|------|
| **分片上传** | 大文件（≥10MB）自动分片，每片 10MB，支持超大文件 |
| **断点续传** | 网络中断后继续上传，只传缺失的分片 |
| **秒传功能** | 基于文件 Hash 去重，相同文件秒级完成 |
| **多文件批量** | 支持同时选择多个文件，批量上传 |
| **实时进度** | 单文件和总体进度实时显示 |
| **历史记录** | 本地保存上传历史，方便查看 |

### 跨平台支持

- **iOS** - 支持 iPhone、iPad
- **Android** - 支持各品牌安卓手机
- **HarmonyOS** - 支持华为鸿蒙设备
- **Web** - 支持浏览器直接访问（规划中）

### 技术亮点

- H5 + 原生混合架构，一套前端代码适配多端
- JSBridge 原生通信，调用系统相册、WiFi 检测等能力
- 异步高性能后端，支持多设备并发上传
- Docker 容器化部署，一键启动

---

## 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                        客户端层                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │   iOS App    │  │ Android App  │  │ HarmonyOS App│       │
│  │  (Swift/OC)  │  │  (Kotlin)    │  │  (ArkTS)     │       │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘       │
│         │                 │                 │               │
│  ┌──────▼─────────────────▼─────────────────▼───────┐       │
│  │                   H5 前端                         │       │
│  │         Vue 3 + Vite + Vant 4 + Pinia            │       │
│  └──────────────────────┬───────────────────────────┘       │
│                         │ JSBridge                          │
│  ┌──────────────────────▼───────────────────────────┐       │
│  │                 原生能力层                         │       │
│  │   文件选择 | WiFi检测 | 本地存储 | 后台上传         │       │
│  └──────────────────────────────────────────────────┘       │
└─────────────────────────────┬───────────────────────────────┘
                              │ HTTP API
┌─────────────────────────────▼───────────────────────────────┐
│                        服务端层                              │
│  ┌──────────────────────────────────────────────────┐       │
│  │              FastAPI 后端服务                     │       │
│  │   ┌─────────────────────────────────────────┐    │       │
│  │   │   API 路由层                            │    │       │
│  │   │   - 文件上传接口                        │    │       │
│  │   │   - 健康检查接口                        │    │       │
│  │   │   - 服务器信息接口                      │    │       │
│  │   └─────────────────────────────────────────┘    │       │
│  │   ┌─────────────────────────────────────────┐    │       │
│  │   │   业务服务层                            │    │       │
│  │   │   - 分片上传服务                        │    │       │
│  │   │   - 文件存储服务                        │    │       │
│  │   └─────────────────────────────────────────┘    │       │
│  └──────────────────────────────────────────────────┘       │
│  ┌──────────────────────────────────────────────────┐       │
│  │              文件存储系统                         │       │
│  │   uploads/ (完成文件) | temp/ (临时分片)          │       │
│  └──────────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────┘
```

---

## 项目结构

```
局域网文件传输/
├── 后端服务工程/              # Python FastAPI 后端
│   ├── app/                  # 应用代码
│   │   ├── api/             # API 路由
│   │   ├── services/        # 业务逻辑
│   │   ├── models/          # 数据模型
│   │   └── utils/           # 工具函数
│   ├── Dockerfile           # Docker 镜像
│   ├── docker-compose.yml   # Docker 编排
│   └── requirements.txt     # Python 依赖
│
├── H5工程/                   # Vue.js 前端
│   ├── src/
│   │   ├── views/           # 页面组件
│   │   ├── components/      # 公共组件
│   │   ├── stores/          # Pinia 状态管理
│   │   ├── utils/           # 工具函数
│   │   └── api/             # API 接口
│   ├── package.json
│   └── vite.config.ts
│
├── iOS客户端/                # iOS 原生工程
│   └── LanFileTransfer/
│       ├── Core/            # 核心模块
│       ├── Modules/         # 功能模块
│       ├── H5/              # 内嵌 H5 资源
│       └── Podfile          # CocoaPods 依赖
│
├── 安卓客户端/               # Android 原生工程
│   └── app/
│       ├── src/main/java/   # Kotlin 代码
│       ├── src/main/assets/ # H5 资源
│       └── build.gradle.kts
│
├── 鸿蒙客户端/               # HarmonyOS 工程
│   └── LanFileTransfer/
│       ├── entry/           # 入口模块
│       └── AppScope/        # 应用配置

```

---

## 快速开始

### 1. 启动后端服务

#### 方式一：Docker（推荐）

```bash
cd 后端服务工程

# 构建并启动
docker-compose -p lan-file-transfer up -d

# 查看日志
docker-compose -p lan-file-transfer logs -f
```

#### 方式二：Python 虚拟环境

```bash
cd 后端服务工程

# 创建虚拟环境
python -m venv .venv
source .venv/bin/activate  # macOS/Linux
# .venv\Scripts\activate   # Windows

# 安装依赖
pip install -r requirements.txt

# 启动服务
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

### 2. 访问服务

启动后访问以下地址：

| 服务 | 地址 |
|------|------|
| API 文档 (Swagger) | http://localhost:8000/docs |
| API 文档 (ReDoc) | http://localhost:8000/redoc |
| 健康检查 | http://localhost:8000/api/health |
| 服务器信息 | http://localhost:8000/api/server-info |

### 3. 构建客户端

#### H5 前端

```bash
cd H5工程

# 安装依赖
npm install

# 开发模式
npm run dev

# 生产构建
npm run build
```

#### iOS 客户端

```bash
cd iOS客户端/LanFileTransfer

# 安装依赖
pod install

# 用 Xcode 打开 .xcworkspace 文件
open LanFileTransfer.xcworkspace
```

#### Android 客户端

使用 Android Studio 打开 `安卓客户端` 目录，等待 Gradle 同步完成后运行。

#### 鸿蒙客户端

使用 DevEco Studio 打开 `鸿蒙客户端/LanFileTransfer` 目录。

---

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Python | 3.9+ | 编程语言 |
| FastAPI | 0.115+ | Web 框架 |
| Uvicorn | 0.34+ | ASGI 服务器 |
| aiofiles | 24.1+ | 异步文件 IO |
| Docker | 20.10+ | 容器化部署 |

### H5 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue.js | 3.5+ | 前端框架 |
| Vite | 7.0+ | 构建工具 |
| TypeScript | 5.9+ | 类型系统 |
| Vant | 4.9+ | 移动端 UI 组件 |
| Pinia | 3.0+ | 状态管理 |

### 移动端

| 平台 | 技术 | 说明 |
|------|------|------|
| iOS | Objective-C | WebView + JSBridge |
| Android | Kotlin | WebView + JSBridge |
| HarmonyOS | ArkTS | Web 组件 + JSBridge |

---

## 开发计划

- [x] 后端服务 - FastAPI + Docker
- [x] H5 前端 - Vue 3 + Vant 4
- [x] iOS 客户端 - WebView + JSBridge
- [x] Android 客户端 - WebView + JSBridge
- [x] 鸿蒙客户端 - Web 组件 + JSBridge

---


## 许可证

本项目采用 Apache 2.0 许可证 - 详见 [LICENSE](LICENSE) 文件

---

## 作者

**董思远**

---
