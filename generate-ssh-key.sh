#!/bin/bash

# SSH 密钥生成脚本
# 使用方法: ./generate-ssh-key.sh [email]

EMAIL="${1:-$(git config user.email)}"

if [ -z "$EMAIL" ]; then
    EMAIL="user@example.com"
fi

echo "================================"
echo "SSH 密钥生成工具"
echo "================================"
echo ""

# 1. 检查 .ssh 目录
if [ ! -d "$HOME/.ssh" ]; then
    echo "📂 创建 .ssh 目录..."
    mkdir -p "$HOME/.ssh"
    chmod 700 "$HOME/.ssh"
fi

# 2. 检查是否已存在密钥
if [ -f "$HOME/.ssh/id_ed25519" ]; then
    echo "⚠️  ED25519 密钥已存在"
    echo "📝 现有公钥内容:"
    echo "---"
    cat "$HOME/.ssh/id_ed25519.pub"
    echo "---"
    echo ""
    read -p "是否要重新生成? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "✅ 已取消"
        exit 0
    fi
fi

# 3. 生成 ED25519 密钥
echo "🔐 正在生成 ED25519 SSH 密钥..."
echo "📧 Email: $EMAIL"
echo ""

ssh-keygen -t ed25519 -f "$HOME/.ssh/id_ed25519" -C "$EMAIL" -N ""

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ SSH 密钥生成成功！"
    echo ""
    
    # 设置正确的权限
    chmod 600 "$HOME/.ssh/id_ed25519"
    chmod 644 "$HOME/.ssh/id_ed25519.pub"
    
    # 显示公钥
    echo "================================"
    echo "📋 您的 SSH 公钥："
    echo "================================"
    echo ""
    cat "$HOME/.ssh/id_ed25519.pub"
    echo ""
    echo "================================"
    echo ""
    
    # 显示密钥指纹
    echo "🔍 密钥指纹:"
    ssh-keygen -l -f "$HOME/.ssh/id_ed25519.pub"
    echo ""
    
    # 提示下一步
    echo "📌 下一步操作："
    echo "1. 复制上面的公钥内容"
    echo "2. 添加到 GitHub:"
    echo "   - 访问 https://github.com/settings/keys"
    echo "   - 点击 'New SSH key'"
    echo "   - 粘贴公钥内容"
    echo ""
    echo "3. 配置 Git:"
    echo "   git config --global user.email \"$EMAIL\""
    echo "   git config --global user.name \"Your Name\""
    echo ""
    echo "4. 测试连接:"
    echo "   ssh -T git@github.com"
    echo ""
else
    echo ""
    echo "❌ SSH 密钥生成失败"
    exit 1
fi
