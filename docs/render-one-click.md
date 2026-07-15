# Render 一键部署掌心窗 iOS Lite

这份教程尽量按“照着点就行”的方式写。Render 会创建两个服务：

```text
linjian-peek-server：后端。iOS 快捷指令 / Android App 填这个地址。
linjian-peek-mcp：MCP。ChatGPT / MCP 客户端填这个地址 + /mcp。
```

## 0. 部署前确认

你的 GitHub 仓库根目录必须能看到这些文件：

```text
render.yaml
server/linjian_server.py
mcp/server.js
mcp/package.json
README.md
```

如果 `render.yaml` 不在根目录，Render 一键部署会找不到配置。

## 1. 打开一键部署页面

点 README 顶部的 **Deploy to Render** 按钮。

也可以手动打开：

```text
https://dashboard.render.com/blueprint/new?repo=https://github.com/linzhi-524/linjian-peek-public
```

如果你用的是自己的仓库，把 `repo=` 后面的地址换成你自己的仓库地址。

例子：

```text
https://dashboard.render.com/blueprint/new?repo=https://github.com/你的用户名/你的仓库名
```

## 2. Render 页面怎么填

进入 Blueprint 页面后：

```text
Blueprint Name：随便，例如 linjian-peek
Branch：main
Blueprint Path：留空
```

`Blueprint Path` 不要填 `server`，不要填 `mcp`，留空就行，因为 `render.yaml` 在仓库根目录。

然后点部署。

## 3. 等两个服务部署完成

部署完成后你会看到两个 Web Service：

```text
linjian-peek-server
linjian-peek-mcp
```

两个都变成 Live / Running 后再继续。

如果第一次访问很慢，正常。Render Free 会冷启动，可能要等几十秒。

## 4. 复制 Server 地址

打开 `linjian-peek-server`，复制它的地址，例如：

```text
https://linjian-peek-server-xxxx.onrender.com
```

这个地址叫 **Server URL**。

它要填到 iOS 快捷指令里：

```text
上传状态 URL = Server URL + /api/ios/state
上传截图 URL = Server URL + /api/ios/screenshot
```

例子：

```text
https://linjian-peek-server-xxxx.onrender.com/api/ios/state
https://linjian-peek-server-xxxx.onrender.com/api/ios/screenshot
```

检查 Server 是否活着：

```text
https://linjian-peek-server-xxxx.onrender.com/health
```

看到 `ok: true` 就说明后端通了。

## 5. 复制 MCP 地址

打开 `linjian-peek-mcp`，复制它的地址，例如：

```text
https://linjian-peek-mcp-xxxx.onrender.com
```

MCP 客户端要填：

```text
https://linjian-peek-mcp-xxxx.onrender.com/mcp
```

旧版 SSE 客户端填：

```text
https://linjian-peek-mcp-xxxx.onrender.com/sse
```

检查 MCP 是否活着：

```text
https://linjian-peek-mcp-xxxx.onrender.com/health
```

看到 `ok: true`、`has_url: true`、`has_token: true` 就说明 MCP 配好了。

## 6. 找 Token

进入 Render 的 `linjian-peek-server` 服务：

```text
Environment → LINJIAN_TOKEN
```

复制这一串值。

它要填到：

```text
iOS 快捷指令 Header：X-Auth-Token
Android App Token
MCP 服务环境变量，render.yaml 已自动引用，不用手填
```

不要把 Token 发到公开平台。它相当于房卡。

如果 Render 没显示自动生成的 Token，自己生成一串：

```bash
python3 - <<'PY'
import secrets
print(secrets.token_urlsafe(32))
PY
```

然后把 `linjian-peek-server` 的 `LINJIAN_TOKEN` 改成这串，保存后 redeploy。

## 7. iOS 快捷指令上传状态怎么填

快捷指令里用“获取 URL 内容”。

```text
URL：https://linjian-peek-server-xxxx.onrender.com/api/ios/state
方法：POST
请求正文：JSON
Header 1：X-Auth-Token = 你的 LINJIAN_TOKEN
Header 2：Content-Type = application/json
```

JSON 最小可用版：

```json
{
  "device_id": "ios-phone",
  "platform": "ios",
  "battery_percent": 80,
  "local_time": "14:30",
  "note": "在家"
}
```

快捷指令里可以把 `battery_percent` 换成“获取电池电量”的变量，把 `local_time` 换成当前日期格式化后的变量。

## 8. iOS 快捷指令上传截图怎么填

建议流程：

```text
手动截图 → 分享 → 选择快捷指令 → 快捷指令把图片 POST 到后端
```

快捷指令设置：

```text
接收分享表单：图片
URL：https://linjian-peek-server-xxxx.onrender.com/api/ios/screenshot
方法：POST
请求正文：文件 / 快捷指令输入
Header 1：X-Auth-Token = 你的 LINJIAN_TOKEN
Header 2：Content-Type = image/jpeg
```

上传成功后，ChatGPT / MCP 里用 `latest_screen` 读取最近截图。

## 9. ChatGPT / MCP 添加方式

在支持 MCP 的客户端里添加：

```text
名称：掌心窗 iOS Lite
类型：HTTP / Streamable HTTP MCP
URL：https://linjian-peek-mcp-xxxx.onrender.com/mcp
```

旧客户端只支持 SSE 时用：

```text
https://linjian-peek-mcp-xxxx.onrender.com/sse
```

## 10. 常见问题

### 部署页面说找不到 render.yaml

检查 `render.yaml` 是否在仓库根目录，不要放在 `docs/`、`server/` 或 `mcp/` 里面。

### Server 的 `/health` 打不开

Render Free 可能在冷启动，等 30 秒再刷新。还不行就看 Logs。

### MCP 的 `/health` 里 `has_token: false`

说明 `LINJIAN_TOKEN` 没传到 MCP。重新部署 Blueprint，或者检查 `render.yaml` 里 MCP 是否引用了 server 的 `LINJIAN_TOKEN`。

### 快捷指令返回 401 / Unauthorized

Token 不一致。检查：

```text
Header 名必须是 X-Auth-Token
Header 值必须和 Render server 的 LINJIAN_TOKEN 一模一样
不要多空格
不要复制到引号
```

### 快捷指令上传了，但 ChatGPT 看不到

先打开：

```text
https://linjian-peek-server-xxxx.onrender.com/api/ios/state?device_id=ios-phone
```

如果这里有内容，说明后端收到了。再检查 MCP 地址是不是填的 `linjian-peek-mcp`，不是 `linjian-peek-server`。

### Render 要绑卡

换 Hugging Face Spaces 路线，见 `docs/huggingface-spaces.md`。
