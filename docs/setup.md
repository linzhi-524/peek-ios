# 掌心窗安装测试流程

## 1. 部署服务器

在 server 目录准备 `.env`：

```env
LINJIAN_TOKEN=你的长随机token
LINJIAN_HOST=0.0.0.0
LINJIAN_PORT=8513
LINJIAN_KEEP=3
```

启动：

```bash
python3 linjian_server.py
```

公网使用时务必通过 HTTPS 访问。Render 上可以直接把 Start Command 写成：

```bash
cd server && python3 linjian_server.py
```

环境变量在 Render 后台添加。

## 2. 打 APK

最省心：上传 GitHub 后跑 Actions → Build Android APK。

## 3. 手机端

- 安装 APK。
- 打开掌心窗，填服务器地址和 token。
- 点“打开无障碍设置”，开启掌心窗截图服务。
- 回 App，点“启动”。
- 点“测试截图上传”确认 server/data/screenshots 里出现图片。

## 4. 电脑端请求截图

```bash
cd server
python3 peek.py --wait --save latest.jpg
```

如果失败，优先检查：

- 服务器 URL 是否是 `https://域名`，不要多余斜杠。
- token 是否完全一致。
- 手机无障碍是否打开。
- 手机端是否点了启动。
- Render 是否休眠。
