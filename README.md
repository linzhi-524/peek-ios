# 掌心窗 Palm Window MCP · v0.1.9 iOS Lite 一键部署版

[![Deploy to Render](https://render.com/images/deploy-to-render-button.svg)](https://dashboard.render.com/blueprint/new?repo=https://github.com/linzhi-524/peek-ios)

> 如果你不是用 `linzhi-524/peek-ios` 这个仓库，请把按钮链接里的 `repo=` 后面换成你自己的 GitHub 仓库地址。

这是掌心窗的 **iOS Lite + Android 兼容**版本。它保留原来的 `server/` + `mcp/` 架构，并新增适合 iPhone 的快捷指令方案。

它由几部分组成：

- `server/`：Python 后端，保存手机状态、截图、命令队列，也接收 iOS 快捷指令上传的状态和截图。
- `mcp/`：Node MCP 服务，把后端能力暴露给 ChatGPT / MCP 客户端。
- `ios-shortcuts/`：iOS 快捷指令配置蓝图，不是可直接导入的 `.shortcut` 文件，但照着填即可。
- `android/`：Android 掌心窗 App 构建脚本，仍保留给 Android 用户使用。
- `render.yaml`：Render 一键部署配置。
- `Dockerfile` + `start_hf.sh`：Hugging Face Spaces Docker 部署配置。

## 先看这个：iOS Lite 能做什么

iOS Lite 和 Android 完整版不一样。iPhone 普通快捷指令不能静默读屏、不能后台随便截图、不能跨 App 自动点击，所以 iOS Lite 的核心是：**用户主动递屏，AI 读取你主动上传的内容。**

能做：

- 用快捷指令上传电量、时间、备注等状态。
- 手动截图后，通过分享菜单上传截图。
- ChatGPT / MCP 读取最近 iOS 状态：`get_ios_state`。
- ChatGPT / MCP 读取最近主动上传的截图：`latest_screen`。

不能做：

- 不能静默截图。
- 不能后台读当前 App。
- 不能自动点击 / 滑动 / 跨 App 控制 iPhone。
- 不建议尝试越狱、私有 API 或绕过系统限制。

## 路线 A：Render 一键部署

适合：已经有 GitHub 仓库，想点按钮部署的人。

### 1. 上传代码到 GitHub

把这个 ZIP 解压后的全部文件放进你的 GitHub 仓库根目录。根目录应该能看到：

```text
README.md
render.yaml
server/linjian_server.py
mcp/server.js
mcp/package.json
ios-shortcuts/
docs/
```

### 2. 点 Deploy to Render

点 README 顶部按钮，或打开：

```text
https://dashboard.render.com/blueprint/new?repo=https://github.com/linzhi-524/peek-ios
```

如果你用自己的仓库，把最后的仓库地址换成你自己的。

Render 页面这样填：

```text
Blueprint Name：随便取，例如 peek-ios
Branch：main
Blueprint Path：留空
```

点部署后会自动创建两个服务：

```text
peek-ios-server  → 给 iOS 快捷指令 / Android App 填的后端地址
peek-ios-mcp     → 给 ChatGPT / MCP 客户端填的 MCP 地址
```

### 3. 复制两个地址

打开 `peek-ios-server`，复制它的 `.onrender.com` 地址，例如：

```text
https://peek-ios-server-xxxx.onrender.com
```

这个地址用于：

```text
iOS 快捷指令 Server URL
Android App Server URL
```

打开 `peek-ios-mcp`，复制它的 `.onrender.com` 地址，然后加 `/mcp`：

```text
https://peek-ios-mcp-xxxx.onrender.com/mcp
```

旧版 SSE 客户端可用：

```text
https://peek-ios-mcp-xxxx.onrender.com/sse
```

### 4. 找 Token

进入 Render 的 `peek-ios-server` 服务：

```text
Environment → LINJIAN_TOKEN
```

复制这串 Token。它是你的手机、后端、MCP 之间的通行证。

不要公开，不要截图发小红书，不要提交进仓库。

### 5. iOS 快捷指令怎么填

上传状态快捷指令：

```text
URL：https://peek-ios-server-xxxx.onrender.com/api/ios/state
方法：POST
Header：X-Auth-Token = 你的 LINJIAN_TOKEN
Header：Content-Type = application/json
Body：JSON
```

Body 最小示例：

```json
{
  "device_id": "ios-phone",
  "platform": "ios",
  "battery_percent": "快捷指令里获取的电量",
  "local_time": "当前时间",
  "note": "在家 / 在学校 / 正在听歌"
}
```

上传截图快捷指令：

```text
URL：https://peek-ios-server-xxxx.onrender.com/api/ios/screenshot
方法：POST
Header：X-Auth-Token = 你的 LINJIAN_TOKEN
Header：Content-Type = image/jpeg
Body：快捷指令输入的图片
```

更详细的 iOS 快捷指令步骤见：[`docs/ios-shortcuts-step-by-step.md`](docs/ios-shortcuts-step-by-step.md)。

### 6. ChatGPT / MCP 客户端怎么填

```text
MCP 地址：https://peek-ios-mcp-xxxx.onrender.com/mcp
旧 SSE：https://peek-ios-mcp-xxxx.onrender.com/sse
```

常用工具：

```text
get_ios_state：读取 iOS 快捷指令上传的状态
get_life_state：读取生活状态，iOS 默认 device_id=ios-phone
latest_screen：读取你主动上传的最近截图
linjian_status：检查后端和 MCP 是否通
```

完整 Render 教程见：[`docs/render-one-click.md`](docs/render-one-click.md)。

## 路线 B：Hugging Face Spaces 部署

适合：Render 提示绑卡、不想绑卡、或者想用一个地址跑完的人。

Hugging Face 版把 `server` 和 `mcp` 合在一个 Docker Space 里：

```text
iOS 快捷指令 Server URL：https://你的用户名-你的Space名.hf.space
MCP URL：https://你的用户名-你的Space名.hf.space/mcp
旧 SSE：https://你的用户名-你的Space名.hf.space/sse
```

最短步骤：

```text
1. Hugging Face → New Space
2. SDK 选 Docker
3. Hardware 选 CPU Basic / Free
4. 上传本仓库全部文件
5. Settings → Variables and secrets
6. 添加 Secret：LINJIAN_TOKEN=自己的长随机密钥
7. 等 Build 完成
8. 打开 /health 和 /mcp_health 检查
```

完整 Hugging Face 傻瓜教程见：[`docs/huggingface-spaces.md`](docs/huggingface-spaces.md)。

## 安全边界

掌心窗涉及手机状态、截图和控制能力，请务必只用于你本人授权的设备。

- 不要公开 `LINJIAN_TOKEN`。
- 不要把 MCP 地址和 Token 一起发出去。
- 不要连接不是自己授权的手机。
- iOS Lite 截图必须由用户主动上传。
- 支付、验证码、钱包、私聊等敏感内容不要随便递屏。
- 支付、下单、删除、发送消息等高风险动作，应始终由用户最终确认。

## 常见检查地址

Render 双服务：

```text
Server 健康检查：https://peek-ios-server-xxxx.onrender.com/health
MCP 健康检查：https://peek-ios-mcp-xxxx.onrender.com/health
MCP 地址：https://peek-ios-mcp-xxxx.onrender.com/mcp
```

Hugging Face 单服务：

```text
后端健康检查：https://你的用户名-你的Space名.hf.space/health
MCP 健康检查：https://你的用户名-你的Space名.hf.space/mcp_health
MCP 地址：https://你的用户名-你的Space名.hf.space/mcp
```

## iOS Lite 文档

- iOS Lite 说明：[`docs/ios-lite.md`](docs/ios-lite.md)
- iOS 快捷指令傻瓜教程：[`docs/ios-shortcuts-step-by-step.md`](docs/ios-shortcuts-step-by-step.md)
- Render 一键部署：[`docs/render-one-click.md`](docs/render-one-click.md)
- Hugging Face Spaces：[`docs/huggingface-spaces.md`](docs/huggingface-spaces.md)
