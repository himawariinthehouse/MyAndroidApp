#!/bin/bash
set -e

echo "================================"
echo "MyAndroidApp APK 构建脚本"
echo "================================"
echo ""

# 1. 检查Java
echo "[1/5] 检查Java环境..."
if ! command -v java &> /dev/null; then
    echo "❌ Java未安装，正在安装..."
    apt-get update > /dev/null 2>&1
    apt-get install -y default-jre default-jdk > /dev/null 2>&1
    echo "✅ Java安装完成"
else
    JAVA_VERSION=$(java -version 2>&1 | grep version | cut -d'"' -f2)
    echo "✅ Java已安装: $JAVA_VERSION"
fi

# 2. 检查Android SDK
echo ""
echo "[2/5] 检查Android SDK..."
if [ -z "$ANDROID_HOME" ]; then
    echo "⚠️  ANDROID_HOME未设置"
    echo "需要安装Android SDK..."
    
    # 检查是否可以继续
    if [ ! -f "/usr/bin/sdkmanager" ]; then
        echo "❌ 需要在本地机器上安装Android SDK"
        echo "请按照以下步骤操作："
        echo "1. 下载 Android Command Line Tools"
        echo "2. 设置 ANDROID_HOME 环境变量"
        echo "3. 安装所需的SDK packages"
        exit 1
    fi
else
    echo "✅ Android SDK已配置: $ANDROID_HOME"
fi

# 3. 清理项目
echo ""
echo "[3/5] 清理项目..."
if [ -d "build" ]; then
    rm -rf build
    echo "✅ 清理完成"
else
    echo "✅ 无需清理"
fi

# 4. 验证gradle wrapper
echo ""
echo "[4/5] 验证Gradle Wrapper..."
if [ -f "gradlew" ]; then
    chmod +x gradlew
    echo "✅ Gradle Wrapper已就绪"
else
    echo "❌ Gradle Wrapper缺失"
    exit 1
fi

# 5. 构建APK
echo ""
echo "[5/5] 开始构建APK..."
echo "（这可能需要几分钟时间，请耐心等待）"
echo ""

if [ -z "$ANDROID_HOME" ]; then
    echo "❌ 无法继续构建，缺少Android SDK"
    echo ""
    echo "📝 Codespace中构建Android APP的解决方案："
    echo "1️⃣  使用本地Android Studio构建"
    echo "2️⃣  在支持Android开发的云环境中构建"
    echo "3️⃣  使用Docker容器进行构建"
    exit 1
else
    ./gradlew assembleDebug
    
    if [ $? -eq 0 ]; then
        echo ""
        echo "================================"
        echo "✅ APK构建成功！"
        echo "================================"
        echo ""
        APK_FILE=$(find . -name "app-debug.apk" -type f | head -1)
        if [ -f "$APK_FILE" ]; then
            APK_SIZE=$(ls -lh "$APK_FILE" | awk '{print $5}')
            echo "📦 APK文件位置: $APK_FILE"
            echo "📊 APK文件大小: $APK_SIZE"
            echo ""
            echo "🚀 安装到设备的命令:"
            echo "   adb install $APK_FILE"
        fi
    else
        echo ""
        echo "❌ APK构建失败"
        exit 1
    fi
fi
