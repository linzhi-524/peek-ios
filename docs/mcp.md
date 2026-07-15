# 掌心窗 MCP 接入 GPT

这个目录提供一个独立的 MCP 服务，用来把《掌心窗》接进支持 MCP 的客户端。

## 架构

手机 App → 掌心窗 server → 掌心窗 MCP → GPT

- 手机 App：负责在你授权后截图并上传。
- 掌心窗 server：保存最近截图、接收 peek 命令。
- 掌心窗 MCP：给 GPT 暴露工具 `peek_screen`、`latest_screen`、`linjian_status`。

## Render 部署 MCP

新建一个 Render Web Service，连接同一个 GitHub 仓库。

配置：

```text
Root Directory: mcp
Build Command: npm install
Start Command: npm start
```

环境变量：

```text
LINJIAN_URL=https://你的掌心窗server.onrender.com
LINJIAN_TOKEN=和掌心窗server完全一样的token
```

部署完成后会得到一个 MCP 服务地址，例如：

```text
https://linjian-peek-mcp.onrender.com
```

## 接到 GPT

优先填：

```text
https://linjian-peek-mcp.onrender.com/mcp
```

如果客户端要求 SSE 地址，填：

```text
https://linjian-peek-mcp.onrender.com/sse
```

## 可用工具

### peek_screen

向手机端请求一张新截图，等待上传后返回图片。

参数：

```json
{"wait_seconds": 25}
```

### latest_screen

不敲门，直接读取最近一张截图。

### linjian_status

检查 MCP 与掌心窗 server 是否连通。

## 注意

- 这个 MCP 服务也要保密，因为它持有 `LINJIAN_TOKEN`。
- GPT 连接时不要把 token 写进 URL。
- 手机 App 必须处于启动状态，并且已开启无障碍截图权限。
- Render 免费服务休眠后，第一次请求可能要等几十秒。
