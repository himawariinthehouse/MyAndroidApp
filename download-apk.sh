#!/bin/bash

# 从GitHub Actions下载最新的APK
# 使用方法: ./download-apk.sh [owner/repo] [artifact-name] [save-path]

OWNER_REPO="${1:-himawariinthehouse/MyAndroidApp}"
ARTIFACT_NAME="${2:-debug-apk}"
SAVE_PATH="${3:-./}"

echo "🔍 查找最新的APK构建..."
echo "📦 仓库: $OWNER_REPO"
echo "🎯 Artifact: $ARTIFACT_NAME"
echo ""

# 检查是否安装了gh CLI
if ! command -v gh &> /dev/null; then
    echo "❌ 未安装 GitHub CLI (gh)"
    echo "请访问: https://cli.github.com/"
    echo "或运行: brew install gh  (macOS)"
    echo "或运行: apt-get install gh  (Linux)"
    exit 1
fi

# 获取最新的成功运行
echo "⏳ 获取最新的构建信息..."
LATEST_RUN=$(gh run list \
    --repo "$OWNER_REPO" \
    --workflow build-apk.yml \
    --status success \
    --limit 1 \
    --json databaseId \
    --jq '.[0].databaseId')

if [ -z "$LATEST_RUN" ]; then
    echo "❌ 未找到成功的构建"
    echo "请先在 GitHub 上运行工作流"
    exit 1
fi

echo "✅ 找到最新构建: #$LATEST_RUN"
echo ""

# 列出该运行的所有artifacts
echo "📂 可用的 Artifacts:"
gh run view "$LATEST_RUN" \
    --repo "$OWNER_REPO" \
    --json artifacts \
    --jq '.artifacts[].name'

echo ""
echo "⬇️  下载中: $ARTIFACT_NAME"

# 下载指定的artifact
gh run download "$LATEST_RUN" \
    --repo "$OWNER_REPO" \
    --name "$ARTIFACT_NAME" \
    --dir "$SAVE_PATH"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ 下载成功！"
    echo "📂 保存位置: $SAVE_PATH/$ARTIFACT_NAME"
    echo ""
    
    # 显示APK文件信息
    APK_FILE=$(find "$SAVE_PATH/$ARTIFACT_NAME" -name "*.apk" -type f | head -1)
    if [ -f "$APK_FILE" ]; then
        APK_SIZE=$(du -h "$APK_FILE" | cut -f1)
        echo "📦 APK文件: $(basename "$APK_FILE")"
        echo "📊 文件大小: $APK_SIZE"
        echo ""
        echo "🚀 安装到设备:"
        echo "   adb install \"$APK_FILE\""
    fi
else
    echo "❌ 下载失败"
    exit 1
fi
