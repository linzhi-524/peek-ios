# Hugging Face Spaces 部署掌心窗 iOS Lite（傻瓜版）

这条路线适合 Render 要绑卡、或者只想用一个地址的人。

Render 版有两个地址：

```text
server 地址：给 iOS 快捷指令 / Android App
mcp 地址：给 ChatGPT / MCP 客户端
```

Hugging Face Spaces 版只有一个地址：

```text
https://你的用户名-你的Space名.hf.space
```

填法：

```text
iOS 快捷指令 Server URL：https://你的用户名-你的Space名.hf.space
MCP URL：https://你的用户名-你的Space名.hf.space/mcp
旧 SSE：https://你的用户名-你的Space名.hf.space/sse
```

## 1. 创建 Space

打开 Hugging Face，登录账号后：

```text
右上角头像 / + New Space
```

页面这样填：

```text
Space name：随便，例如 palm-window
License：随便，例如 MIT
SDK：Docker
Hardware：CPU Basic / Free
Visibility：建议先 Private 或 Protected
```

重点：**SDK 必须选 Docker**。不要选 Gradio，不要选 Streamlit。

创建后进入 Space 页面。

## 2. 上传文件

把这个 ZIP 解压。把解压后的全部文件上传到 Space。

Space 根目录必须能看到：

```text
README.md
Dockerfile
start_hf.sh
server/linjian_server.py
mcp/server.js
mcp/package.json
ios-shortcuts/
docs/
```

不要只上传 `server`，不要只上传 `mcp`，要上传整个项目。

也可以用 git 推送到 Space，但新手直接网页上传就行。

## 3. 设置 Token

进入 Space：

```text
Settings → Variables and secrets
```

添加 Secret：

```text
Name：LINJIAN_TOKEN
Value：自己生成的一串长密钥
```

可以用这个命令生成：

```bash
python3 - <<'PY'
import secrets
print(secrets.token_urlsafe(32))
PY
```

不会生成也可以自己写一串很长的随机字符，例如至少 32 位。不要用 `123456`，不要用生日。

再添加 Variable：

```text
LINJIAN_DEFAULT_DEVICE = ios-phone
LINJIAN_KEEP = 3
```

`LINJIAN_TOKEN` 不要公开。它就是钥匙。

## 4. 等 Build 完成

上传文件和设置 Secret 后，Hugging Face 会开始 Build。

页面里看到 Running / Built successfully 后，再测试。

第一次启动可能比较慢，免费 Space 会睡眠，醒来也可能慢几十秒。

## 5. 测试后端

打开：

```text
https://你的用户名-你的Space名.hf.space/health
```

成功时大概会看到：

```json
{
  "ok": true,
  "service": "linjian-unified",
  "name": "掌心窗"
}
```

这个 `/health` 是 Python 后端健康检查。

## 6. 测试 MCP

打开：

```text
https://你的用户名-你的Space名.hf.space/mcp_health
```

成功时大概会看到：

```json
{
  "ok": true,
  "service": "linjian-unified-mcp",
  "proxy_enabled": true,
  "has_token": true
}
```

如果 `has_token: false`，说明 Secret 没设置好，或者设置后没有重启 Space。

## 7. iOS 快捷指令填法

上传状态快捷指令：

```text
URL：https://你的用户名-你的Space名.hf.space/api/ios/state
方法：POST
Header：X-Auth-Token = 你的 LINJIAN_TOKEN
Header：Content-Type = application/json
Body：JSON
```

JSON 示例：

```json
{
  "device_id": "ios-phone",
  "platform": "ios",
  "battery_percent": 76,
  "local_time": "15:30",
  "note": "在学校，正在听歌"
}
```

上传截图快捷指令：

```text
URL：https://你的用户名-你的Space名.hf.space/api/ios/screenshot
方法：POST
Header：X-Auth-Token = 你的 LINJIAN_TOKEN
Header：Content-Type = image/jpeg
Body：快捷指令输入的图片
```

上传截图后，MCP 里用 `latest_screen` 读取。

## 8. ChatGPT / MCP 客户端填法

新版 MCP：

```text
https://你的用户名-你的Space名.hf.space/mcp
```

旧 SSE：

```text
https://你的用户名-你的Space名.hf.space/sse
```

建议先调用：

```text
linjian_status
get_ios_state
latest_screen
```

## 9. 常见问题

### `/health` 打不开

可能还在 Build，或者 Space 睡眠。先打开 Space 页面等它醒来，再刷新 `/health`。

### `/health` 能打开，但快捷指令 401

Token 错了。检查：

```text
Header 名：X-Auth-Token
Header 值：必须和 Space Secret 的 LINJIAN_TOKEN 完全一致
```

### `/mcp_health` 显示 `proxy_enabled: false`

说明没有启用 Hugging Face 代理。检查 Space Variables：

```text
LINJIAN_HF_PROXY = 1
```

本 ZIP 的 Dockerfile 已默认设置，如果你手动改过环境变量，再加回来。

### ChatGPT 添加 MCP 后工具失败

先打开：

```text
https://你的用户名-你的Space名.hf.space/mcp_health
```

确认：

```text
has_token: true
proxy_enabled: true
```

再确认 MCP 地址填的是 `/mcp`，不是 `/health`。

### 上传状态后读不到

先在浏览器打开：

```text
https://你的用户名-你的Space名.hf.space/api/ios/state?device_id=ios-phone
```

如果这里能看到状态，说明快捷指令上传成功。然后再查 MCP 配置。

### 免费 Space 数据会丢吗

可能会。免费容器重启后，本地临时数据可能丢失。重新用快捷指令上传一次状态或截图即可。

### 可以公开 Space 吗

不建议。掌心窗涉及状态、截图和控制能力。新手建议 Private / Protected。

## 小红书简短版

```text
Render 要绑卡就用 Hugging Face：
1. New Space
2. SDK 选 Docker
3. Hardware 选 CPU Basic Free
4. 上传 ZIP 解压后的全部文件
5. Settings 添加 Secret：LINJIAN_TOKEN=自己的长密钥
6. 等 Build 完成
7. /health 测后端，/mcp_health 测 MCP
8. iOS 快捷指令填 Space 地址 + /api/ios/state 或 /api/ios/screenshot
9. ChatGPT MCP 填 Space 地址 + /mcp
```
