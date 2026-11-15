# 三星国行安全组件一键关闭（LSPosed 模块）

本模块结合 **LSPosed 运行时 Hook** 与 **Root 一键冻结**，用于关闭三星国行 One UI 设备上的各类安全扫描、自动拦截程序及后台策略下发组件。

## 功能说明

1. **一键冻结可安全禁用的安全组件（Root）**：
   - `com.samsung.android.sm.devicesecurity.tcm`（设备安全 / 腾讯安全引擎）
   - `com.samsung.android.sm.devicesecurity`（设备安全 / McAfee 引擎）
   - `com.samsung.android.rampart`（自动拦截程序 Auto Blocker）
   - `com.samsung.android.spdclient` / `com.policydm`（安全策略更新）
   - `com.samsung.android.sm.policy`（智能管理器安全策略）
   - `com.samsung.klmsagent` / `com.samsung.android.knox.attestation`（Knox 代理与认证）
   - `com.samsung.android.app.omcagent`（OMC 云端配置代理，默认不批量关闭，可手动单独开关）

2. **LSPosed 运行时屏蔽（不破坏系统核心功能）**：
   - 作用域包含 `android`（系统框架）、`com.samsung.android.sm_cn`（国行智能管理器）、`com.samsung.android.packageinstaller`（三星安装器）、`com.samsung.android.rampart` 等。
   - 在系统框架层拦截 `SemCscFeature` 的国行安全检测开关，并阻止系统后台将已禁用的安全组件重新启用。
   - 在安装器中绕过第三方 APK 的云端安全扫描与风险拦截。

## 编译为 APK 的两种方式

### 方式一：GitHub Actions 云端自动编译（无需本地装环境）
1. 在 GitHub 新建一个仓库，将本压缩包解压后的全部文件上传到仓库根目录（包含隐藏文件夹 `.github`）。
2. 打开仓库的 **Actions** 标签页，运行 **Build LSPosed Module APK**。
3. 等待约 1–2 分钟编译完成后，在 **Artifacts** 中下载 `SamsungCNGuardDisabler-debug-apk` 即可得到 `.apk`。

### 方式二：Android Studio 本地编译
1. 使用 Android Studio 打开解压后的 `SamsungCNGuardDisabler` 文件夹。
2. 等待 Gradle 同步完成（需确保安装 Android SDK 35 与 JDK 17）。
3. 点击菜单 **Build -> Build Bundle(s) / APK(s) -> Build APK(s)** 生成安装包。

## 手机端使用步骤
1. 安装编译好的 APK。
2. 打开 **LSPosed 管理器 -> 模块 -> 三星国行安全关闭**，开启开关并保持默认勾选的作用域（`系统框架`、`智能管理器`、`安装包安装程序` 等）。
3. 重启手机使 LSPosed Hook 生效。
4. 打开桌面上的 **三星国行安全关闭** App，授予 Root 权限后点击 **一键关闭全部** 即可。
