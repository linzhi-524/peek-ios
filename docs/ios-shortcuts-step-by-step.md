# iOS 快捷指令傻瓜教程

iOS Lite 需要你自己在快捷指令里做两个小按钮：

```text
1. 上传状态：把电量、时间、备注发给掌心窗后端
2. 上传截图：把你主动分享的截图发给掌心窗后端
```

这不是自动监控，也不是后台偷窥。每次状态或截图都由你主动触发。

## 准备三个东西

先准备：

```text
Server URL：你的后端裸地址
Token：LINJIAN_TOKEN
Device ID：ios-phone
```

Render 的 Server URL 长这样：

```text
https://linjian-peek-server-xxxx.onrender.com
```

Hugging Face 的 Server URL 长这样：

```text
https://你的用户名-你的Space名.hf.space
```

注意：Server URL 后面不要带 `/health`，不要带 `/mcp`。

## 快捷指令 1：上传状态

### 1. 新建快捷指令

打开 iPhone 的“快捷指令”App：

```text
右上角 + → 新建快捷指令 → 命名：掌心窗上传状态
```

### 2. 获取电池电量

添加动作：

```text
获取电池电量
```

它会得到一个百分比数字。

### 3. 获取当前日期

添加动作：

```text
当前日期
```

再添加：

```text
格式化日期
```

时间格式可以先用默认。想细一点可做两个格式：

```text
HH:mm
yyyy-MM-dd
```

嫌麻烦也可以先不格式化，直接传当前日期文本。

### 4. 准备 JSON 文本

添加动作：

```text
文本
```

填入：

```json
{
  "device_id": "ios-phone",
  "platform": "ios",
  "battery_percent": 电池电量变量,
  "local_time": "当前时间变量",
  "note": "在家"
}
```

快捷指令里变量需要点键盘上方的变量按钮插进去，不要真的写“电池电量变量”几个字。

初次测试可以用固定值：

```json
{
  "device_id": "ios-phone",
  "platform": "ios",
  "battery_percent": 80,
  "local_time": "14:30",
  "note": "测试"
}
```

固定值能成功后，再换成变量。

### 5. 获取 URL 内容

添加动作：

```text
获取 URL 内容
```

URL 填：

```text
你的 Server URL/api/ios/state
```

例子：

```text
https://linjian-peek-server-xxxx.onrender.com/api/ios/state
```

或：

```text
https://你的用户名-你的Space名.hf.space/api/ios/state
```

展开高级设置：

```text
方法：POST
请求正文：JSON / 文件 / 文本均可，推荐 JSON 或文本
```

Headers 添加两行：

```text
X-Auth-Token：你的 LINJIAN_TOKEN
Content-Type：application/json
```

正文选择刚刚那段 JSON 文本。

### 6. 测试

点运行。成功后，在浏览器打开：

Render：

```text
https://linjian-peek-server-xxxx.onrender.com/api/ios/state?device_id=ios-phone
```

Hugging Face：

```text
https://你的用户名-你的Space名.hf.space/api/ios/state?device_id=ios-phone
```

如果能看到刚刚上传的 JSON，说明成功。

## 快捷指令 2：上传截图

### 1. 新建快捷指令

命名：

```text
掌心窗上传截图
```

### 2. 打开分享表单

快捷指令详情里打开：

```text
在共享表单中显示
```

接收类型选择：

```text
图像
```

这样你截图后点分享，就能选择这个快捷指令。

### 3. 获取 URL 内容

添加动作：

```text
获取 URL 内容
```

URL 填：

```text
你的 Server URL/api/ios/screenshot
```

例子：

```text
https://linjian-peek-server-xxxx.onrender.com/api/ios/screenshot
```

或：

```text
https://你的用户名-你的Space名.hf.space/api/ios/screenshot
```

设置：

```text
方法：POST
请求正文：文件
文件：快捷指令输入
```

Headers：

```text
X-Auth-Token：你的 LINJIAN_TOKEN
Content-Type：image/jpeg
```

### 4. 测试

随便截一张图：

```text
截图 → 分享 → 掌心窗上传截图
```

然后让 MCP 调用：

```text
latest_screen
```

能看到图就成功。

## 最容易错的地方

### URL 写错

正确：

```text
https://xxx.onrender.com/api/ios/state
https://xxx.hf.space/api/ios/state
```

错误：

```text
https://xxx.onrender.com/health/api/ios/state
https://xxx.onrender.com/mcp/api/ios/state
```

### Token 写错

Header 名必须是：

```text
X-Auth-Token
```

不是：

```text
Authorization
Token
x-auth-token
```

大小写通常不敏感，但新手按 `X-Auth-Token` 写最稳。

### Content-Type 写错

上传状态：

```text
Content-Type = application/json
```

上传截图：

```text
Content-Type = image/jpeg
```

### Device ID 不一致

iOS Lite 默认：

```text
ios-phone
```

MCP 读取时也用：

```text
get_ios_state(device_id="ios-phone")
```

或者：

```text
get_life_state(device_id="ios-phone")
```

## 成功后的使用方式

平时你可以这样用：

```text
1. 点“掌心窗上传状态”
2. 对 AI 说：查一下我的 iOS 状态
3. 手动截图并分享到“掌心窗上传截图”
4. 对 AI 说：看一下我刚刚递的屏
```

这就是 iOS Lite 的核心：你主动递，AI 再看。
