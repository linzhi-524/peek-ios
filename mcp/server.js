import express from "express";
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StreamableHTTPServerTransport } from "@modelcontextprotocol/sdk/server/streamableHttp.js";
import { SSEServerTransport } from "@modelcontextprotocol/sdk/server/sse.js";
import { z } from "zod";
import { createProxyMiddleware } from "http-proxy-middleware";

const PORT = Number(process.env.PORT || 8787);
const LINJIAN_URL = (process.env.LINJIAN_URL || "").replace(/\/$/, "");
const LINJIAN_TOKEN = process.env.LINJIAN_TOKEN || "";
const DEFAULT_DEVICE = process.env.LINJIAN_DEFAULT_DEVICE || "ios-phone";
const ENABLE_PHONE_PROXY = process.env.LINJIAN_HF_PROXY === "1" || process.env.LINJIAN_ENABLE_PROXY === "1";
const LINJIAN_PROXY_TARGET = (process.env.LINJIAN_PROXY_TARGET || process.env.LINJIAN_INTERNAL_URL || "http://127.0.0.1:8513").replace(/\/$/, "");

function requireConfig() {
  if (!LINJIAN_URL) throw new Error("Missing env LINJIAN_URL, for example https://linjian-peek.onrender.com");
  if (!LINJIAN_TOKEN) throw new Error("Missing env LINJIAN_TOKEN");
}

async function linjianFetch(path, options = {}) {
  requireConfig();
  const res = await fetch(`${LINJIAN_URL}${path}`, {
    ...options,
    headers: { "X-Auth-Token": LINJIAN_TOKEN, ...(options.headers || {}) }
  });
  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new Error(`Linjian server HTTP ${res.status}: ${text || res.statusText}`);
  }
  return res;
}

async function postCommand(payload) {
  const res = await linjianFetch("/api/command", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
  return await res.json();
}

async function latestInfo() {
  const res = await linjianFetch("/api/latest.json");
  return await res.json();
}

async function latestMtime() {
  try { const info = await latestInfo(); return Number(info.mtime || 0); } catch { return 0; }
}

async function fetchLatestImage() {
  const res = await linjianFetch("/api/latest");
  const mimeType = res.headers.get("content-type")?.split(";")[0] || "image/jpeg";
  const ab = await res.arrayBuffer();
  const buf = Buffer.from(ab);
  return { mimeType, data: buf.toString("base64"), bytes: buf.byteLength };
}

function makeServer() {
  const server = new McpServer({ name: "掌心窗", version: "0.1.9-ios-lite" });

  server.tool(
    "peek_screen",
    "向 Android 掌心窗手机端请求新截图。iOS Lite 无法被动截图，请用户用快捷指令主动上传截图后再使用 latest_screen。",
    { wait_seconds: z.number().int().min(3).max(60).default(25).describe("等待手机上传新截图的秒数，默认 25。Render 免费实例刚醒时可以调大。") },
    async ({ wait_seconds = 25 }) => {
      const before = await latestMtime();
      await postCommand({ action: "peek", device_id: DEFAULT_DEVICE });
      const deadline = Date.now() + wait_seconds * 1000;
      while (Date.now() < deadline) {
        await new Promise((resolve) => setTimeout(resolve, 1000));
        const info = await latestInfo().catch(() => null);
        if (info && Number(info.mtime || 0) > before) {
          const img = await fetchLatestImage();
          return { content: [
            { type: "text", text: `掌心窗已收到新截图：${info.filename || "latest"}，大小约 ${info.size || img.bytes} bytes。` },
            { type: "image", data: img.data, mimeType: img.mimeType }
          ] };
        }
      }
      return { content: [{ type: "text", text: `等待 ${wait_seconds} 秒后还没有收到新截图。请检查：手机 App 是否点了启动、无障碍权限是否开启、服务器地址和 Token 是否一致、Render 是否刚从休眠中醒来。` }], isError: true };
    }
  );

  server.tool("latest_screen", "不敲门，直接读取服务器里最近一次掌心窗截图。", {}, async () => {
    const info = await latestInfo(); const img = await fetchLatestImage();
    return { content: [
      { type: "text", text: `最近截图：${info.filename || "latest"}，时间戳 ${info.mtime || "unknown"}。` },
      { type: "image", data: img.data, mimeType: img.mimeType }
    ] };
  });

  server.tool("linjian_status", "检查掌心窗后端是否在线，以及 MCP 是否配置了 LINJIAN_URL 和 LINJIAN_TOKEN。", {}, async () => {
    requireConfig();
    const health = await fetch(`${LINJIAN_URL}/health`).then((r) => r.json()).catch((e) => ({ ok: false, error: String(e) }));
    const latest = await latestInfo().catch(() => null);
    return { content: [{ type: "text", text: JSON.stringify({ ok: true, linjian_url: LINJIAN_URL, health, has_latest: Boolean(latest), latest }, null, 2) }] };
  });

  server.tool("get_phone_state", "读取手机最近状态。返回 current_package、screen_text、accessibility_ready。", { device_id: z.string().default(DEFAULT_DEVICE) }, async ({ device_id = DEFAULT_DEVICE }) => {
    const res = await linjianFetch(`/api/device/state?device_id=${encodeURIComponent(device_id)}`);
    const data = await res.json();
    return { content: [{ type: "text", text: JSON.stringify(data, null, 2) }] };
  });



  server.tool("get_life_state", "读取掌心窗生活状态层：Android 或 iOS Lite 均可。iOS Lite 由快捷指令主动上传电量、充电、时间、备注等；默认不截图。", { device_id: z.string().default(DEFAULT_DEVICE) }, async ({ device_id = DEFAULT_DEVICE }) => {
    const res = await linjianFetch(`/api/life_state?device_id=${encodeURIComponent(device_id)}`);
    const data = await res.json();
    return { content: [{ type: "text", text: JSON.stringify(data, null, 2) }] };
  });

  server.tool("get_ios_state", "读取 iOS Lite 通过快捷指令主动上传的最近状态。默认 device_id 为 ios-phone。", { device_id: z.string().default("ios-phone") }, async ({ device_id = "ios-phone" }) => {
    const res = await linjianFetch(`/api/ios/state?device_id=${encodeURIComponent(device_id)}`);
    const data = await res.json();
    return { content: [{ type: "text", text: JSON.stringify(data, null, 2) }] };
  });

  server.tool("list_known_apps", "列出预置 App 包名白名单，包括小红书、微信、QQ、抖音、ChatGPT、Speedcat。", {}, async () => {
    const res = await fetch(`${LINJIAN_URL}/api/known_apps`).then((r) => r.json()).catch(() => ({ ok: true, apps: { "小红书": "com.xingin.xhs", "ChatGPT": "com.openai.chatgpt" } }));
    return { content: [{ type: "text", text: JSON.stringify(res, null, 2) }] };
  });

  server.tool("send_phone_command", "发送手机控制命令。action 可用 open_app/home/back/recents/tap/swipe/noop/set_alarm/send_notification。", {
    action: z.string(), app: z.string().default(""), package: z.string().default(""), device_id: z.string().default(DEFAULT_DEVICE),
    x: z.number().default(0), y: z.number().default(0), x1: z.number().default(0), y1: z.number().default(0), x2: z.number().default(0), y2: z.number().default(0), duration: z.number().int().default(350)
  }, async (args) => {
    const result = await postCommand({ ...args, payload: args });
    return { content: [{ type: "text", text: JSON.stringify({ ...result, safety_note: "命令已排队，手机执行器下一次轮询时执行。" }, null, 2) }] };
  });

  server.tool("open_app", "打开指定 App。app 可填 小红书/微信/QQ/抖音/ChatGPT/Speedcat，或直接传 package。", { app: z.string().default(""), package: z.string().default(""), device_id: z.string().default(DEFAULT_DEVICE) }, async ({ app = "", package: pkg = "", device_id = DEFAULT_DEVICE }) => {
    const result = await postCommand({ action: "open_app", app, package: pkg, device_id });
    return { content: [{ type: "text", text: JSON.stringify(result, null, 2) }] };
  });

  server.tool("phone_home", "让手机回到桌面。", { device_id: z.string().default(DEFAULT_DEVICE) }, async ({ device_id = DEFAULT_DEVICE }) => ({ content: [{ type: "text", text: JSON.stringify(await postCommand({ action: "home", device_id }), null, 2) }] }));
  server.tool("phone_back", "让手机执行返回。", { device_id: z.string().default(DEFAULT_DEVICE) }, async ({ device_id = DEFAULT_DEVICE }) => ({ content: [{ type: "text", text: JSON.stringify(await postCommand({ action: "back", device_id }), null, 2) }] }));
  server.tool("phone_recents", "打开手机最近任务。", { device_id: z.string().default(DEFAULT_DEVICE) }, async ({ device_id = DEFAULT_DEVICE }) => ({ content: [{ type: "text", text: JSON.stringify(await postCommand({ action: "recents", device_id }), null, 2) }] }));



  server.tool("send_notification", "发送一条手机系统通知提醒。只在用户明确要求时使用。", {
    title: z.string().default("掌心窗提醒"), message: z.string().default("看一眼这里。"), device_id: z.string().default(DEFAULT_DEVICE)
  }, async ({ title = "掌心窗提醒", message = "看一眼这里。", device_id = DEFAULT_DEVICE }) => {
    const result = await postCommand({ action: "send_notification", device_id, payload: { title, message } });
    return { content: [{ type: "text", text: JSON.stringify({ ...result, note: "若手机未弹出通知，请在系统设置中允许掌心窗发送通知。" }, null, 2) }] };
  });

  server.tool("set_alarm", "设置系统闹钟。只在用户明确要求时使用。hour 为 0-23，minute 为 0-59。", {
    hour: z.number().int().min(0).max(23), minute: z.number().int().min(0).max(59), message: z.string().default("掌心窗闹钟"), vibrate: z.boolean().default(true), skip_ui: z.boolean().default(true), device_id: z.string().default(DEFAULT_DEVICE)
  }, async ({ hour, minute, message = "掌心窗闹钟", vibrate = true, skip_ui = true, device_id = DEFAULT_DEVICE }) => {
    const result = await postCommand({ action: "set_alarm", device_id, payload: { hour, minute, message, vibrate, skip_ui } });
    return { content: [{ type: "text", text: JSON.stringify({ ...result, note: "部分手机系统可能仍会弹出闹钟 App 确认界面。" }, null, 2) }] };
  });

  return server;
}

const app = express();

// Hugging Face Spaces only exposes one external port.
// When LINJIAN_HF_PROXY=1, the Node service also proxies phone/iOS backend routes
// (/health and /api/*) to the internal Python server. Render MCP services do not enable
// this flag, so their /health remains the MCP health check.
if (ENABLE_PHONE_PROXY) {
  const phoneServerProxy = createProxyMiddleware({
    target: LINJIAN_PROXY_TARGET,
    changeOrigin: false,
    ws: false,
    proxyTimeout: 120000,
    timeout: 120000
  });
  app.use(["/api", "/health"], phoneServerProxy);
}

app.use(express.json({ limit: "32mb" }));
app.get("/", (_req, res) => res.type("text/plain").send("掌心窗 unified MCP is running. Use /mcp for Streamable HTTP, or /sse for SSE."));
app.get("/health", (_req, res) => res.json({ ok: true, service: "linjian-unified-mcp", version: "0.1.9-ios-lite", has_url: Boolean(LINJIAN_URL), has_token: Boolean(LINJIAN_TOKEN) }));
app.get("/mcp_health", (_req, res) => res.json({ ok: true, service: "linjian-unified-mcp", version: "0.1.9-ios-lite-deploy", linjian_url: LINJIAN_URL, proxy_enabled: ENABLE_PHONE_PROXY, proxy_target: LINJIAN_PROXY_TARGET, has_token: Boolean(LINJIAN_TOKEN) }));
app.post("/mcp", async (req, res) => {
  try { const server = makeServer(); const transport = new StreamableHTTPServerTransport({ sessionIdGenerator: undefined }); res.on("close", () => transport.close()); await server.connect(transport); await transport.handleRequest(req, res, req.body); }
  catch (err) { console.error(err); if (!res.headersSent) res.status(500).json({ jsonrpc: "2.0", error: { code: -32603, message: String(err?.message || err) }, id: null }); }
});
app.get("/mcp", (_req, res) => res.status(405).json({ ok: false, error: "Use POST /mcp for Streamable HTTP MCP." }));
const sseTransports = new Map();
app.get("/sse", async (_req, res) => {
  try { const transport = new SSEServerTransport("/messages", res); sseTransports.set(transport.sessionId, transport); res.on("close", () => { sseTransports.delete(transport.sessionId); transport.close(); }); await makeServer().connect(transport); }
  catch (err) { console.error(err); if (!res.headersSent) res.status(500).end(String(err?.message || err)); }
});
app.post("/messages", async (req, res) => { const sessionId = req.query.sessionId; const transport = sseTransports.get(sessionId); if (!transport) return res.status(404).send("No SSE transport for sessionId"); await transport.handlePostMessage(req, res, req.body); });
app.listen(PORT, "0.0.0.0", () => { console.log(`掌心窗 unified MCP listening on 0.0.0.0:${PORT}`); console.log(`LINJIAN_URL=${LINJIAN_URL || "<missing>"}`); });
