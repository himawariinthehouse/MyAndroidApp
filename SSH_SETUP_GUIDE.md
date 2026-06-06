# GitHub SSH 公钥配置指南

## 📌 快速开始

### 方法1️⃣：使用脚本（最简单）

```bash
# 进入项目目录
cd /workspaces/MyAndroidApp

# 使脚本可执行
chmod +x setup-ssh.sh

# 运行脚本生成并显示SSH公钥
./setup-ssh.sh
```

### 方法2️⃣：手动命令

```bash
# 生成 SSH 密钥（如果还没有）
ssh-keygen -t ed25519 -f ~/.ssh/id_ed25519 -N ""

# 显示公钥
cat ~/.ssh/id_ed25519.pub

# 复制到剪贴板
cat ~/.ssh/id_ed25519.pub | xclip -selection clipboard  # Linux
# 或
cat ~/.ssh/id_ed25519.pub | pbcopy  # macOS
```

---

## 🔧 详细步骤

### 步骤1️⃣：生成 SSH 密钥

#### 方式A：ED25519 (推荐，更安全)

```bash
ssh-keygen -t ed25519 -f ~/.ssh/id_ed25519 -C "your_email@example.com" -N ""
```

**参数说明：**
- `-t ed25519`：使用 ED25519 算法
- `-f ~/.ssh/id_ed25519`：密钥文件位置
- `-C "..."` ：注释（通常是邮箱）
- `-N ""`：不设置密码（直接回车）

#### 方式B：RSA (如果需要兼容性)

```bash
ssh-keygen -t rsa -b 4096 -f ~/.ssh/id_rsa -C "your_email@example.com" -N ""
```

### 步骤2️⃣：显示公钥

```bash
# 查看 ED25519 公钥
cat ~/.ssh/id_ed25519.pub

# 或 RSA 公钥
cat ~/.ssh/id_rsa.pub
```

输出示例：
```
ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIBX... your_email@example.com
```

### 步骤3️⃣：复制公钥

**Linux 用户：**
```bash
cat ~/.ssh/id_ed25519.pub | xclip -selection clipboard
# 或
cat ~/.ssh/id_ed25519.pub | xsel --clipboard --input
```

**macOS 用户：**
```bash
cat ~/.ssh/id_ed25519.pub | pbcopy
```

**Windows/Git Bash:**
```bash
cat ~/.ssh/id_ed25519.pub | clip
```

**手动复制：**
直接运行 `cat ~/.ssh/id_ed25519.pub` 然后手动选择和复制

### 步骤4️⃣：在 GitHub 中添加 SSH 公钥

1. **打开 GitHub 设置**
   - 访问 https://github.com/settings/keys
   - 或点击头像 → Settings → SSH and GPG keys

2. **添加新的 SSH 密钥**
   - 点击 "New SSH key"

3. **填写信息**
   - **Title**: 给这个密钥起个名字（例如：`MyAndroidApp-Dev`）
   - **Key type**: 保持默认（Authentication Key）
   - **Key**: 粘贴你的公钥内容

4. **保存**
   - 点击 "Add SSH key"
   - 输入 GitHub 密码确认

### 步骤5️⃣：测试连接

```bash
ssh -T git@github.com
```

**成功输出：**
```
Hi himawariinthehouse! You've successfully authenticated, but GitHub does not provide shell access.
```

---

## 🔐 安全建议

### ✅ 要做的事：

- ✅ 定期更换 SSH 密钥
- ✅ 在公共计算机上使用不同的密钥
- ✅ 为私钥添加密码保护（生产环境）
- ✅ 备份你的私钥
- ✅ 在 GitHub 上定期审查已连接的密钥

### ❌ 不要做的事：

- ❌ 不要分享你的私钥
- ❌ 不要将私钥上传到 GitHub
- ❌ 不要在公共地方显示你的私钥
- ❌ 不要使用弱密码保护私钥

---

## 📂 SSH 密钥文件位置

| 文件 | 用途 | 权限 |
|------|------|------|
| `~/.ssh/id_ed25519` | **私钥**（保密）| 600 |
| `~/.ssh/id_ed25519.pub` | 公钥（可共享）| 644 |
| `~/.ssh/config` | SSH 配置文件 | 600 |
| `~/.ssh/authorized_keys` | 授权密钥 | 600 |

---

## 🛠️ 故障排查

### Q: 连接时出现 "Permission denied"

```bash
# 检查权限
chmod 700 ~/.ssh
chmod 600 ~/.ssh/id_ed25519
chmod 644 ~/.ssh/id_ed25519.pub
```

### Q: 如何知道使用的是哪个密钥

```bash
# 查看所有 SSH 密钥的指纹
ssh-keygen -l -f ~/.ssh/id_ed25519.pub
```

### Q: 如何删除旧的 SSH 密钥

```bash
rm ~/.ssh/id_rsa
rm ~/.ssh/id_rsa.pub
```

### Q: 如何更改已有密钥的密码

```bash
ssh-keygen -p -f ~/.ssh/id_ed25519
```

### Q: 测试连接失败

```bash
# 详细调试输出
ssh -vvv git@github.com

# 检查 SSH 配置
ssh -G git@github.com
```

---

## 💡 高级配置

### 为不同服务使用不同密钥

编辑 `~/.ssh/config`：

```bash
# GitHub
Host github.com
    User git
    IdentityFile ~/.ssh/id_ed25519
    IdentitiesOnly yes

# Gitee (如果需要)
Host gitee.com
    User git
    IdentityFile ~/.ssh/id_gitee
    IdentitiesOnly yes

# 公司 GitLab
Host gitlab.company.com
    User git
    IdentityFile ~/.ssh/id_company
    IdentitiesOnly yes
```

### 自动启动 SSH Agent

在 `~/.bashrc` 或 `~/.zshrc` 中添加：

```bash
# SSH Agent 自启动
if [ -z "$(pgrep -f ssh-agent)" ]; then
    eval "$(ssh-agent -s)"
    ssh-add ~/.ssh/id_ed25519
fi
```

---

## 📚 相关资源

- [GitHub SSH 官方文档](https://docs.github.com/en/authentication/connecting-to-github-with-ssh)
- [SSH 密钥对介绍](https://www.ssh.com/ssh/public-key-authentication)
- [ED25519 算法说明](https://en.wikipedia.org/wiki/EdDSA)

---

## ✨ 总结

1. ✅ 使用 `ssh-keygen` 生成密钥
2. ✅ 复制公钥到 GitHub
3. ✅ 测试 `ssh -T git@github.com`
4. ✅ 开始使用 SSH 推送代码！

现在你可以使用 SSH 而不需要输入密码来推送代码了！🎉
