package dev.linjian.peek;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ScreenshotService extends AccessibilityService {
    private static volatile ScreenshotService instance;
    private static volatile String currentPackage = "";
    private static volatile String screenText = "";
    private final Executor executor = Executors.newSingleThreadExecutor();
    private Handler watchdog;
    private HandlerThread backgroundPollThread;
    private Handler backgroundPollHandler;

    public static ScreenshotService getInstance() { return instance; }
    public static boolean ready() { return instance != null; }
    public static String currentPackage() { return currentPackage == null ? "" : currentPackage; }
    public static String screenText() { return screenText == null ? "" : screenText; }

    private final Runnable watchdogTick = new Runnable() {
        @Override public void run() {
            try {
                SharedPreferences prefs = getSharedPreferences(AppPrefs.PREFS, MODE_PRIVATE);
                String url = prefs.getString(AppPrefs.KEY_SERVER, "");
                String tk = prefs.getString(AppPrefs.KEY_TOKEN, "");
                boolean userStopped = prefs.getBoolean("user_stopped", false);
                if (!CompanionService.isRunning() && !userStopped && !url.isEmpty() && !tk.isEmpty()) {
                    DebugState.append(ScreenshotService.this, "看门狗：尝试重启前台服务");
                    Intent i = new Intent(ScreenshotService.this, CompanionService.class);
                    i.putExtra("server_url", url);
                    i.putExtra("token", tk);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i); else startService(i);
                }
            } catch (Exception e) {
                DebugState.append(ScreenshotService.this, "看门狗异常：" + shortMsg(e));
            }
            if (watchdog != null) watchdog.postDelayed(this, 60000);
        }
    };

    private final Runnable backgroundPollTick = new Runnable() {
        @Override public void run() {
            try {
                SharedPreferences prefs = getSharedPreferences(AppPrefs.PREFS, MODE_PRIVATE);
                String url = normalizeUrl(prefs.getString(AppPrefs.KEY_SERVER, ""));
                String tk = prefs.getString(AppPrefs.KEY_TOKEN, "");
                boolean userStopped = prefs.getBoolean("user_stopped", true);
                if (!userStopped && !url.isEmpty() && !tk.isEmpty()) {
                    String body = pollServerFromAccessibility(url, tk);
                    if (body != null && body.length() > 0) CompanionService.handleCommandBody(ScreenshotService.this, body, url, tk);
                }
            } catch (Exception e) {
                DebugState.append(ScreenshotService.this, "无障碍后台轮询异常：" + shortMsg(e));
            }
            if (backgroundPollHandler != null) backgroundPollHandler.postDelayed(this, Math.max(700, AppPrefs.interval(ScreenshotService.this)));
        }
    };

    @Override public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        DebugState.append(this, "无障碍服务已连接：截图/读屏/手势可用");
        watchdog = new Handler(Looper.getMainLooper());
        watchdog.postDelayed(watchdogTick, 15000);
        startBackgroundPolling();
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        CharSequence pkg = event.getPackageName();
        if (pkg != null) currentPackage = pkg.toString();
        int t = event.getEventType();
        if (t == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || t == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || t == AccessibilityEvent.TYPE_VIEW_SCROLLED) updateScreenText();
    }
    @Override public void onInterrupt() { DebugState.append(this, "无障碍服务被中断"); }

    @Override public void onDestroy() {
        DebugState.append(this, "无障碍服务已断开");
        instance = null;
        if (watchdog != null) { watchdog.removeCallbacksAndMessages(null); watchdog = null; }
        if (backgroundPollHandler != null) { backgroundPollHandler.removeCallbacksAndMessages(null); backgroundPollHandler = null; }
        if (backgroundPollThread != null) { backgroundPollThread.quitSafely(); backgroundPollThread = null; }
        super.onDestroy();
    }

    private void startBackgroundPolling() {
        if (backgroundPollThread != null) return;
        backgroundPollThread = new HandlerThread("LinjianAccessibilityPoll");
        backgroundPollThread.start();
        backgroundPollHandler = new Handler(backgroundPollThread.getLooper());
        DebugState.append(this, "无障碍后台轮询已启动 v0.1.8");
        backgroundPollHandler.postDelayed(backgroundPollTick, 1000);
    }

    private String pollServerFromAccessibility(String serverUrl, String token) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(serverUrl + "/api/poll?device_id=" + java.net.URLEncoder.encode(AppPrefs.device(this), "UTF-8")).openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("X-Auth-Token", token);
        try {
            int code = conn.getResponseCode();
            String body = readBody(conn, code);
            if (code == 200) {
                if (body.contains("\"command\": null") || body.contains("\"command\":null")) return "";
                DebugState.append(this, "无障碍后台轮询：收到命令包");
                return body;
            } else {
                DebugState.append(this, "无障碍后台轮询失败：HTTP " + code + " " + clip(body));
            }
            return "";
        } finally { conn.disconnect(); }
    }

    private void updateScreenText() {
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            StringBuilder sb = new StringBuilder();
            collect(root, sb, 0);
            screenText = sb.length() > 2400 ? sb.substring(0, 2400) : sb.toString();
            if (root != null) root.recycle();
        } catch (Exception ignored) { }
    }

    private void collect(AccessibilityNodeInfo node, StringBuilder sb, int depth) {
        if (node == null || sb.length() > 2600 || depth > 12) return;
        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();
        if (text != null && text.length() > 0) sb.append(text).append(" | ");
        else if (desc != null && desc.length() > 0) sb.append(desc).append(" | ");
        for (int i = 0; i < node.getChildCount(); i++) collect(node.getChild(i), sb, depth + 1);
    }

    public boolean doBack() { return performGlobalAction(GLOBAL_ACTION_BACK); }
    public boolean doHome() { return performGlobalAction(GLOBAL_ACTION_HOME); }
    public boolean doRecents() { return performGlobalAction(GLOBAL_ACTION_RECENTS); }

    public boolean doTap(float x, float y) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false;
        Path p = new Path(); p.moveTo(x, y);
        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(p, 0, 80);
        return dispatchGesture(new GestureDescription.Builder().addStroke(stroke).build(), null, null);
    }

    public boolean doSwipe(float x1, float y1, float x2, float y2, long durationMs) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false;
        Path p = new Path(); p.moveTo(x1, y1); p.lineTo(x2, y2);
        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(p, 0, Math.max(80, durationMs));
        return dispatchGesture(new GestureDescription.Builder().addStroke(stroke).build(), null, null);
    }

    public void doScreenshot(String serverUrl, String token) {
        if (Build.VERSION.SDK_INT < 30) { DebugState.append(this, "截图失败：Android 版本低于 11"); return; }
        final String finalUrl = normalizeUrl(serverUrl);
        DebugState.append(this, "开始调用系统截图 API");
        takeScreenshot(Display.DEFAULT_DISPLAY, executor, new TakeScreenshotCallback() {
            @Override public void onSuccess(ScreenshotResult result) {
                try {
                    DebugState.append(ScreenshotService.this, "系统截图成功，开始编码");
                    Bitmap hardwareBitmap = Bitmap.wrapHardwareBuffer(result.getHardwareBuffer(), result.getColorSpace());
                    if (hardwareBitmap == null) { DebugState.append(ScreenshotService.this, "截图失败：Bitmap 为空"); return; }
                    Bitmap bitmap = hardwareBitmap.copy(Bitmap.Config.ARGB_8888, false);
                    hardwareBitmap.recycle(); result.getHardwareBuffer().close();
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out); bitmap.recycle();
                    byte[] data = out.toByteArray();
                    DebugState.append(ScreenshotService.this, "截图编码完成：" + data.length + " bytes");
                    if (data.length > 100) uploadScreenshot(data, finalUrl, token); else DebugState.append(ScreenshotService.this, "上传取消：截图数据太小");
                } catch (Exception e) { DebugState.append(ScreenshotService.this, "截图处理异常：" + shortMsg(e)); }
            }
            @Override public void onFailure(int errorCode) { DebugState.append(ScreenshotService.this, "系统截图失败：errorCode=" + errorCode + "（可尝试关闭再开启无障碍）"); }
        });
    }

    private void uploadScreenshot(byte[] data, String serverUrl, String token) {
        try {
            DebugState.append(this, "开始上传截图到 /api/screenshot");
            HttpURLConnection conn = (HttpURLConnection) new URL(serverUrl + "/api/screenshot").openConnection();
            conn.setRequestMethod("POST"); conn.setDoOutput(true);
            conn.setRequestProperty("X-Auth-Token", token);
            conn.setRequestProperty("Content-Type", "image/jpeg");
            conn.setRequestProperty("Content-Length", String.valueOf(data.length));
            conn.setConnectTimeout(15000); conn.setReadTimeout(30000);
            OutputStream os = conn.getOutputStream(); os.write(data); os.flush(); os.close();
            int code = conn.getResponseCode(); String body = readBody(conn, code);
            if (code >= 200 && code < 300) DebugState.append(this, "上传成功：HTTP " + code + " " + clip(body));
            else DebugState.append(this, "上传失败：HTTP " + code + " " + clip(body));
            conn.disconnect();
        } catch (Exception e) { DebugState.append(this, "上传异常：" + shortMsg(e)); }
    }

    public static String normalizeUrl(String url) { if (url == null) return ""; url = url.trim(); while (url.endsWith("/")) url = url.substring(0, url.length() - 1); return url; }
    static String readBody(HttpURLConnection conn, int code) { try { InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream(); if (is == null) return ""; ByteArrayOutputStream bos = new ByteArrayOutputStream(); byte[] buf = new byte[1024]; int n; while ((n = is.read(buf)) > 0) bos.write(buf, 0, n); return new String(bos.toByteArray(), "UTF-8"); } catch (Exception e) { return ""; } }
    static String clip(String s) { if (s == null) return ""; s = s.replace('\n', ' ').replace('\r', ' '); return s.length() > 90 ? s.substring(0, 90) + "…" : s; }
    static String shortMsg(Exception e) { String msg = e.getClass().getSimpleName(); if (e.getMessage() != null) msg += ": " + e.getMessage(); return clip(msg); }
}
