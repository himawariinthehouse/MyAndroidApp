#!/bin/bash

# 查看GitHub Actions构建状态
# 使用方法: ./check-build-status.sh [owner/repo]

OWNER_REPO="${1:-himawariinthehouse/MyAndroidApp}"

echo "================================"
echo "GitHub Actions 构建状态检查"
echo "================================"
echo ""
echo "📦 仓库: $OWNER_REPO"
echo ""

# 检查是否安装了gh CLI
if ! command -v gh &> /dev/null; then
    echo "❌ 未安装 GitHub CLI (gh)"
    echo "请访问: https://cli.github.com/ 获取安装说明"
    exit 1
fi

# 列出最近的5个构建
echo "📜 最近的5个构建:"
echo ""

gh run list \
    --repo "$OWNER_REPO" \
    --workflow build-apk.yml \
    --limit 5 \
    --json status,conclusion,createdAt,databaseId \
    --template '{{range .}}{{.databaseId | printf "#%d"}}: {{.status | printf "%-10s"}} {{.conclusion | printf "%-10s"}} - {{.createdAt | printf "%.10s"}}\n{{end}}'

echo ""
echo "================================"
echo "状态说明:"
echo "  ✅ success - 构建成功"
echo "  ❌ failure - 构建失败"
echo "  ⏳ in_progress - 构建进行中"
echo "  ⚪ queued - 等待中"
echo "================================"
echo ""

# 获取最新构建的详细信息
LATEST_RUN=$(gh run list \
    --repo "$OWNER_REPO" \
    --workflow build-apk.yml \
    --limit 1 \
    --json databaseId \
    --jq '.[0].databaseId')

if [ -n "$LATEST_RUN" ]; then
    echo "📊 最新构建详情 (#$LATEST_RUN):"
    echo ""
    gh run view "$LATEST_RUN" \
        --repo "$OWNER_REPO" \
        --json status,conclusion,createdAt,updatedAt
fi
