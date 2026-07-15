# 掌心窗 iOS Lite v0.1

iOS Lite 不是 Android 掌心窗的全量复刻。iOS 普通 App / 快捷指令不能像 Android 无障碍服务那样后台读屏、静默截图或跨 App 自动点击。因此 iOS Lite 采用更安全的路线：**用户主动递屏，快捷指令主动上传，MCP 负责读取。**

## 能做什么

- 通过快捷指令上传轻量状态：电量、时间、备注、是否充电等。
- 用户主动截图后，通过分享菜单 / 快捷指令上传最新截图。
- 后端保存 `ios-phone` 的最近状态。
- MCP 通过 `get_ios_state` 或 `get_life_state(device_id="ios-phone")` 读取状态。
- MCP 通过 `latest_screen` 读取用户刚刚主动上传的截图。

## 不承诺什么

- 不承诺静默截图。
- 不承诺后台读当前 App。
- 不承诺自动点击 / 滑动 / 跨 App 控制。
- 不建议尝试越狱、私有 API 或绕过系统权限。

## 推荐部署路线

### Render 一键部署

适合已经有 GitHub 仓库的人。

```text
1. 把 ZIP 解压后的全部文件上传到 GitHub 仓库根目录
2. 点 README 顶部 Deploy to Render
3. Render 自动创建 server 和 mcp 两个服务
4. iOS 快捷指令填 server 地址
5. ChatGPT / MCP 填 mcp 地址 + /mcp
```

详细教程：[`docs/render-one-click.md`](render-one-click.md)

### Hugging Face Spaces

适合 Render 要绑卡、或者只想要一个地址的人。

```text
1. New Space
2. SDK 选 Docker
3. 上传全部文件
4. Settings 添加 Secret：LINJIAN_TOKEN
5. iOS 快捷指令填 Space 裸地址
6. ChatGPT / MCP 填 Space 地址 + /mcp
```

详细教程：[`docs/huggingface-spaces.md`](huggingface-spaces.md)

## 后端接口

### 上传 iOS 状态

`POST /api/ios/state`

Headers:

```http
X-Auth-Token: YOUR_TOKEN
Content-Type: application/json
```

Body 示例：

```json
{
  "device_id": "ios-phone",
  "platform": "ios",
  "battery_percent": 76,
  "charging": true,
  "local_time": "15:30",
  "local_date": "2026-07-14",
  "note": "在学校，正在听歌"
}
```

### 上传 iOS 截图

`POST /api/ios/screenshot`

Headers:

```http
X-Auth-Token: YOUR_TOKEN
Content-Type: image/jpeg
```

Body：图片文件二进制。上传后可用 MCP 的 `latest_screen` 读取。

## 快捷指令建议

仓库中的 `ios-shortcuts/*.example.json` 是蓝图，不是 Apple 官方 `.shortcut` 文件。照着字段在快捷指令里配置即可。

推荐做两个快捷指令：

1. **上传状态**：获取电量 -> 当前日期 -> 文本 JSON -> 获取 URL 内容。
2. **上传截图**：接收分享表单图片 -> 获取 URL 内容 POST 到 `/api/ios/screenshot`。

傻瓜教程：[`docs/ios-shortcuts-step-by-step.md`](ios-shortcuts-step-by-step.md)

## MCP 工具

- `get_ios_state`：读取 iOS Lite 最近状态，默认 `device_id="ios-phone"`。
- `get_life_state`：读取生活状态；iOS 可传 `device_id="ios-phone"`。
- `latest_screen`：读取最近主动上传的截图。
- `linjian_status`：检查后端和 MCP 配置。

Android 相关工具仍保留，但 iOS Lite 不支持自动点击、滑动和后台截图。

## 核心原则

iOS Lite 的重点不是控制手机，而是让用户能主动把当前状态或屏幕递给 AI。它更像“递屏版掌心窗”：能力更少，但边界更清楚。
