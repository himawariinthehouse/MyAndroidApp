#!/bin/bash

# Build APK script
cd /workspaces/MyAndroidApp

echo "检查构建工具..."

# 检查是否安装了Java
if ! command -v java &> /dev/null; then
    echo "安装Java..."
    apt-get update
    apt-get install -y default-jre default-jdk
fi

# 检查是否安装了Android SDK
if [ ! -d "$ANDROID_HOME" ]; then
    echo "需要安装Android SDK"
    echo "请在Android Studio中构建项目或手动配置Android SDK"
    exit 1
fi

# 运行gradle构建
echo "开始构建APK..."
./gradlew assembleDebug

echo "构建完成！APK文件位置："
find . -name "*.apk" -type f
