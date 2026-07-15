# Security Notes

Palm Window MCP exposes sensitive abilities: screenshots, accessibility actions, notifications, alarms, and usage state. Use it only on devices you own or devices where the user has explicitly opted in.

Recommended rules:

1. Generate a long random `LINJIAN_TOKEN` and never commit it.
2. Keep both the backend server and MCP endpoint private where possible.
3. Do not connect untrusted AI clients to your MCP endpoint.
4. Avoid screenshots or automation on payment, wallet, chat, password, or verification-code pages.
5. Require user confirmation before any high-impact action such as payment, ordering, deletion, sending messages, or changing account settings.
6. Turn off the Android service or revoke accessibility permission when not using it.



## iOS Lite 安全边界

iOS Lite 只支持用户主动上传状态或截图。请不要把它包装成静默监控、后台控制或绕过系统限制的工具。截图应由用户手动触发或通过明确配置的快捷指令上传。
