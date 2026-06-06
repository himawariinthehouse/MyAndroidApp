#!/bin/bash

# 快速显示或生成 SSH 公钥

echo "🔑 SSH 公钥管理工具"
echo ""

# 检查是否存在 ED25519 密钥
if [ -f "$HOME/.ssh/id_ed25519.pub" ]; then
    echo "✅ 已找到现有 SSH 公钥"
    echo ""
    echo "📋 您的 SSH 公钥："
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    cat "$HOME/.ssh/id_ed25519.pub"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    
    # 尝试复制到剪贴板
    if command -v pbcopy &> /dev/null; then
        cat "$HOME/.ssh/id_ed25519.pub" | pbcopy
        echo "✅ 已复制到剪贴板（macOS）"
    elif command -v xclip &> /dev/null; then
        cat "$HOME/.ssh/id_ed25519.pub" | xclip -selection clipboard
        echo "✅ 已复制到剪贴板（Linux）"
    elif command -v xsel &> /dev/null; then
        cat "$HOME/.ssh/id_ed25519.pub" | xsel --clipboard --input
        echo "✅ 已复制到剪贴板（Linux）"
    else
        echo "💡 提示：手动复制上面的公钥内容"
    fi
else
    echo "❌ 未找到 SSH 公钥"
    echo ""
    echo "📝 正在生成新的 SSH 密钥..."
    
    # 创建 .ssh 目录
    mkdir -p "$HOME/.ssh"
    chmod 700 "$HOME/.ssh"
    
    # 生成密钥
    ssh-keygen -t ed25519 -f "$HOME/.ssh/id_ed25519" -C "$(git config user.email || echo 'user@example.com')" -N ""
    
    if [ $? -eq 0 ]; then
        echo ""
        echo "✅ SSH 密钥生成成功！"
        echo ""
        echo "📋 您的 SSH 公钥："
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        cat "$HOME/.ssh/id_ed25519.pub"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        
        # 复制到剪贴板
        if command -v xclip &> /dev/null; then
            cat "$HOME/.ssh/id_ed25519.pub" | xclip -selection clipboard
            echo "✅ 已复制到剪贴板"
        else
            echo "💡 提示：手动复制上面的公钥内容"
        fi
    else
        echo "❌ SSH 密钥生成失败"
        exit 1
    fi
fi

echo ""
echo "📌 接下来："
echo "1. 访问 https://github.com/settings/keys"
echo "2. 点击 'New SSH key'"
echo "3. 粘贴公钥内容"
