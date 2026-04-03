# build-server-for-gradle (Gradle Build Server)

Gradle 的 Build Server Protocol (BSP) 实现，为 IDE 提供标准化构建信息接口。

## 项目定位

- **仓库**: https://github.com/microsoft/build-server-for-gradle
- **构建工具**: Gradle (多模块)
- **当前分支**: develop
- **运行时要求**: JDK 17+

## 模块结构

| 模块 | 用途 |
|------|------|
| **server/** | BSP 服务器核心实现 |
| **plugin/** | Gradle 插件 — 提取项目构建信息 |
| **model/** | 数据模型 |

## 核心架构

- **入口**: `com.microsoft.java.bs.core.Launcher`
- **通信**: stdin/stdout 或 named pipes
- **协议**: Build Server Protocol (BSP)

## 支持的 BSP 请求

| 请求 | 用途 |
|------|------|
| `build/initialize`, `build/shutdown`, `build/exit` | 生命周期 |
| `buildTarget/sources`, `buildTarget/resources` | 源码/资源路径 |
| `buildTarget/outputPaths` | 输出路径 |
| `buildTarget/dependencyModules`, `buildTarget/dependencySources` | 依赖信息 |
| `buildTarget/compile`, `buildTarget/cleanCache` | 编译操作 |
| `buildTarget/javacOptions` | 编译器选项 |
| `buildTarget/test` | 测试执行 |
| `workspace/buildTargets`, `workspace/reload` | 工作区管理 |

## 依赖关系

**依赖**: Gradle Tooling API
**被依赖**: vscode-gradle (通过 BSP 客户端)
