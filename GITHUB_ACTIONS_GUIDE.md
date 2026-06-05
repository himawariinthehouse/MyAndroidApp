# GitHub Actions - 自动APK构建指南

## 📋 工作流说明

`build-apk.yml` 工作流配置了以下功能：

### 触发条件
- ✅ 推送到 `main` 或 `develop` 分支
- ✅ 提交 Pull Request 到 `main` 或 `develop` 分支
- ✅ 手动触发工作流（Workflow Dispatch）

### 构建步骤

#### Job 1: 构建 Debug APK
1. 检出代码
2. 设置 Java 11 环境
3. 验证 Gradle Wrapper
4. 构建 Debug APK
5. 上传 APK 作为 Artifact（保留30天）
6. 显示 APK 信息

#### Job 2: 构建 Release APK（依赖于Job 1）
1. 检出代码
2. 设置 Java 11 环境
3. 验证 Gradle Wrapper
4. 构建 Release APK
5. 上传 APK 作为 Artifact（保留30天）
6. 生成构建报告

## 🚀 使用方法

### 1️⃣ 推送代码自动触发
```bash
git add .
git commit -m "update code"
git push origin main
```

### 2️⃣ 手动触发构建
在 GitHub 仓库页面：
1. 点击 **Actions** 标签
2. 左侧选择 **Build APK** 工作流
3. 点击 **Run workflow**
4. 选择分支后点击 **Run workflow**

### 3️⃣ 查看构建结果
1. 进入 **Actions** 标签
2. 点击最新的工作流运行
3. 查看构建日志和 Artifacts

## 📥 下载 APK

### 方式1：从工作流中下载
1. Actions → Build APK → 最新运行
2. 下载 **debug-apk** 或 **release-apk** artifact

### 方式2：命名行下载
```bash
# 使用 GitHub CLI
gh run download <run-id> -n debug-apk

# 或查看所有可下载的artifacts
gh run view <run-id>
```

## 🔧 配置说明

### 修改触发分支
编辑 `.github/workflows/build-apk.yml`：
```yaml
on:
  push:
    branches: [ main, develop, feature/* ]  # 添加更多分支
```

### 修改 Java 版本
```yaml
- name: ☕ 设置JDK 版本
  uses: actions/setup-java@v3
  with:
    java-version: '11'  # 修改版本号
```

### 修改 APK 保留期限
```yaml
retention-days: 30  # 改为其他数字，单位：天
```

## 🔐 Release APK 签名配置

如果需要签名 Release APK，请按以下步骤操作：

### 1. 生成签名密钥
```bash
keytool -genkey -v -keystore release.keystore \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias release-key -storepass <password> \
  -keypass <password>
```

### 2. 添加到 GitHub Secrets
1. 仓库 Settings → Secrets → New repository secret
2. 名称: `KEYSTORE_FILE`，值: Base64编码的keystore文件
3. 添加其他secrets: `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`

### 3. 更新 gradle.properties
```properties
android.injected.signing.store.file=release.keystore
android.injected.signing.store.password=<password>
android.injected.signing.key.alias=release-key
android.injected.signing.key.password=<password>
```

### 4. 更新工作流配置（可选）
在构建Release APK前添加密钥解码步骤

## 📊 工作流状态

### 查看状态
- ✅ 绿色：构建成功
- ❌ 红色：构建失败
- ⏳ 黄色：构建进行中

### 查看日志
1. 点击工作流运行
2. 展开失败的步骤查看详细日志

## 🆘 常见问题

### Q: 构建失败"找不到gradlew"
A: 确保 `gradlew` 文件已提交到仓库
```bash
chmod +x gradlew
git add gradlew
git commit -m "add gradlew"
```

### Q: 如何查看构建时间
A: 在工作流运行页面查看每个步骤的执行时间

### Q: APK 文件有多大
A: 在工作流日志中查看 "获取APK信息" 步骤的输出

### Q: 如何加速构建
A: 工作流已配置 Gradle 缓存，首次构建较慢，后续构建会更快

## 📚 相关资源

- [GitHub Actions 官方文档](https://docs.github.com/en/actions)
- [Setup Java 官方 Action](https://github.com/actions/setup-java)
- [Upload Artifact 官方 Action](https://github.com/actions/upload-artifact)
- [Android Gradle 官方文档](https://developer.android.com/studio/build)

## 💡 最佳实践

1. ✅ 在开发分支测试工作流
2. ✅ 定期检查工作流日志
3. ✅ 为不同分支配置不同的构建目标
4. ✅ 使用 GitHub Environments 管理不同的签名密钥
5. ✅ 在工作流中添加代码质量检查（lint, test）

---

**下一步**：提交文件到 GitHub 后，推送代码将自动触发工作流！🚀
