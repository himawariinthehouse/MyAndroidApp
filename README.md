# MyAndroidApp

一个Android应用示例，使用Kotlin和Jetpack Compose实现。应用包含一个导航主页，提供三个按钮分别跳转到交通、时间和健康三个功能页面。

![Build APK](https://github.com/himawariinthehouse/MyAndroidApp/actions/workflows/build-apk.yml/badge.svg)

## 🎯 快速开始

## 项目结构

```
MyAndroidApp/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/myandroidapp/
│   │   │   │   ├── MainActivity.kt                    # 主Activity
│   │   │   │   ├── navigation/
│   │   │   │   │   ├── Screen.kt                      # 导航路由定义
│   │   │   │   │   └── AppNavigation.kt               # 导航配置
│   │   │   │   ├── screens/
│   │   │   │   │   ├── HomeScreen.kt                  # 主页面
│   │   │   │   │   └── Screens.kt                     # 三个功能页面
│   │   │   │   └── ui/
│   │   │   │       └── theme/
│   │   │   │           └── Theme.kt                   # 应用主题
│   │   │   ├── res/
│   │   │   │   ├── values/
│   │   │   │   │   ├── colors.xml                     # 颜色资源
│   │   │   │   │   ├── strings.xml                    # 字符串资源
│   │   │   │   │   └── themes.xml                     # 主题样式
│   │   │   │   └── xml/
│   │   │   │       ├── backup_rules.xml               # 备份规则
│   │   │   │       └── data_extraction_rules.xml      # 数据提取规则
│   │   │   └── AndroidManifest.xml                    # 应用清单
│   │   └── test/                                      # 测试
│   ├── build.gradle.kts                               # App模块构建配置
│   └── proguard-rules.pro                             # ProGuard配置
├── build.gradle.kts                                   # 项目构建配置
├── settings.gradle.kts                                # 项目设置
├── gradle.properties                                  # Gradle属性
└── README.md

## 功能特性

- **主页导航**：包含三个按钮，分别链接到不同的功能页面
- **交通页面**：交通相关功能展示
- **时间页面**：时间相关功能展示
- **健康页面**：健康相关功能展示
- **返回导航**：每个功能页面都提供返回按钮

## 技术栈

- **Kotlin**：编程语言
- **Jetpack Compose**：现代UI框架
- **Navigation Compose**：页面导航管理
- **Material 3**：Google Material Design组件库
- **Android API 24+**：最低支持的Android版本

## 构建说明

### 前置条件

- Android Studio 2022.1 或更高版本
- Android SDK 34（目标版本）
- Kotlin插件支持
- Java 11 或更高版本

### 方式1：本地构建（推荐）

1. **导入项目**
   ```bash
   # 在Android Studio中打开此项目
   ```

2. **同步Gradle**
   - Android Studio会自动同步Gradle文件

3. **构建APK**
   ```bash
   # 使用Gradle构建
   ./gradlew assembleDebug      # 构建Debug APK
   ./gradlew assembleRelease    # 构建Release APK
   ```

4. **运行应用**
   - 连接Android设备或启动模拟器
   - 在Android Studio中点击"运行"按钮
   - 或使用命令：`./gradlew installDebug`

### 方式2：GitHub Actions 自动构建 ⭐

项目已配置 GitHub Actions 工作流，可自动构建APK：

1. **自动触发构建**
   ```bash
   git push origin main  # 推送代码自动触发构建
   ```

2. **手动触发构建**
   - 访问 GitHub 仓库 → Actions → Build APK
   - 点击 "Run workflow" 按钮

3. **下载APK**
   - 工作流完成后，进入 Actions 页面
   - 在运行记录中下载 artifacts（debug-apk 或 release-apk）

📖 详细说明请参考 [GitHub Actions 构建指南](./GITHUB_ACTIONS_GUIDE.md)

## 应用使用说明

1. **启动应用**：打开应用进入主页
2. **点击按钮**：
   - 点击"交通"按钮进入交通页面
   - 点击"时间"按钮进入时间页面
   - 点击"健康"按钮进入健康页面
3. **返回**：点击页面左上角的返回箭头按钮返回主页

## 扩展建议

- 在各个页面添加具体的功能实现
- 添加数据库支持存储用户数据
- 实现实时数据更新
- 添加用户认证功能
- 集成API接口获取实时数据

## 项目配置

- **应用包名**：com.example.myandroidapp
- **最低SDK版本**：24
- **目标SDK版本**：34
- **Kotlin版本**：1.9.10
- **Compose版本**：2023.10.01

## 许可证

MIT License