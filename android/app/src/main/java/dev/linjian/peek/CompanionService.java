package dev.linjian.peek;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.provider.AlarmClock;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class CompanionService extends Service {
    private static final String CHANNEL_ID = "linjian_peek_service";
    private static final String REMINDER_CHANNEL_ID = "linjian_peek_reminders";
    private static final int NOTIFICATION_ID = 20260715;
    private static volatile boolean running = false;

    private String serverUrl;
    private String token;
    private Handler pollHandler;
    private HandlerThread pollThread;

    public static boolean isRunning() { return running; }
    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) { stopSelf(); return START_NOT_STICKY; }
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification("已启动，等待掌心窗命令"));
        if (intent != null) {
            serverUrl = ScreenshotService.normalizeUrl(intent.getStringExtra("server_url"));
            token = intent.getStringExtra("token");
        }
        if (serverUrl == null || token == null) {
            serverUrl = ScreenshotService.normalizeUrl(AppPrefs.server(this));
            token = AppPrefs.token(this);
        }
        if (serverUrl == null || token == null || serverUrl.isEmpty() || token.isEmpty()) {
            DebugState.append(this, "服务启动失败：服务器地址或 Token 为空");
            stopSelf(); return START_NOT_STICKY;
        }
        DebugState.append(this, "掌心窗 v0.1.8 服务已启动，目标：" + serverUrl);
        if (!running) { running = true; startPolling(); } else DebugState.append(this, "服务已在运行，继续轮询");
        return START_STICKY;
    }

    private void startPolling() {
        pollThread = new HandlerThread("LinjianUnifiedPoll");
        pollThread.start();
        pollHandler = new Handler(pollThread.getLooper());
        DebugState.append(this, "前台轮询线程已启动");
        pollHandler.post(this::pollLoop);
    }

    private void pollLoop() {
        if (!running) return;
        try {
            uploadState(serverUrl, token);
            String body = pollServer();
            if (body != null && body.length() > 0) handleCommandBody(this, body, serverUrl, token);
        } catch (Exception e) { DebugState.append(this, "轮询异常：" + ScreenshotService.shortMsg(e)); }
        if (running) pollHandler.postDelayed(this::pollLoop, Math.max(700, AppPrefs.interval(this)));
    }

    private String pollServer() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(serverUrl + "/api/poll?device_id=" + java.net.URLEncoder.encode(AppPrefs.device(this), "UTF-8")).openConnection();
        conn.setConnectTimeout(10000); conn.setReadTimeout(15000); conn.setRequestMethod("GET"); conn.setRequestProperty("X-Auth-Token", token);
        try {
            int code = conn.getResponseCode(); String body = ScreenshotService.readBody(conn, code);
            if (code == 200) {
                if (body.contains("\"command\": null") || body.contains("\"command\":null")) return "";
                DebugState.append(this, "轮询成功：收到命令包"); return body;
            } else DebugState.append(this, "轮询失败：HTTP " + code + " " + ScreenshotService.clip(body));
            return "";
        } finally { conn.disconnect(); }
    }

    public static void handleCommandBody(Context ctx, String body, String serverUrl, String token) {
        try {
            JSONObject obj = new JSONObject(body);
            Object raw = obj.opt("command");
            if (raw == null || raw == JSONObject.NULL) return;
            if (raw instanceof String) {
                String s = (String) raw;
                if ("peek".equals(s)) executeCommand(ctx, "", "peek", "", "", 0,0,0,0,0,0,350,0,0,"掌心窗", "掌心窗截图", true, serverUrl, token);
                return;
            }
            JSONObject cmd = (JSONObject) raw;
            String id = cmd.optString("id", "");
            String action = cmd.optString("action", "noop");
            String app = cmd.optString("app", "");
            String pkg = cmd.optString("package", "");
            float x = (float) cmd.optDouble("x", 0);
            float y = (float) cmd.optDouble("y", 0);
            float x1 = (float) cmd.optDouble("x1", 0);
            float y1 = (float) cmd.optDouble("y1", 0);
            float x2 = (float) cmd.optDouble("x2", 0);
            float y2 = (float) cmd.optDouble("y2", 0);
            long duration = cmd.optLong("duration", 350);
            int hour = cmd.optInt("hour", -1);
            int minute = cmd.optInt("minute", -1);
            String title = cmd.optString("title", "掌心窗提醒");
            String message = cmd.optString("message", "掌心窗闹钟");
            boolean vibrate = cmd.optBoolean("vibrate", true);
            boolean skipUi = cmd.optBoolean("skip_ui", true);
            executeCommand(ctx, id, action, app, pkg, x, y, x1, y1, x2, y2, duration, hour, minute, title, message, vibrate, serverUrl, token, skipUi);
        } catch (Exception e) { DebugState.append(ctx, "命令解析异常：" + ScreenshotService.shortMsg(e)); }
    }

    private static void executeCommand(Context ctx, String id, String action, String app, String pkg, float x, float y, float x1, float y1, float x2, float y2, long duration, int hour, int minute, String title, String message, boolean vibrate, String serverUrl, String token) {
        executeCommand(ctx, id, action, app, pkg, x, y, x1, y1, x2, y2, duration, hour, minute, title, message, vibrate, serverUrl, token, true);
    }

    private static void executeCommand(Context ctx, String id, String action, String app, String pkg, float x, float y, float x1, float y1, float x2, float y2, long duration, int hour, int minute, String title, String message, boolean vibrate, String serverUrl, String token, boolean skipUi) {
        boolean ok = false; String result = "";
        try {
            ScreenshotService svc = ScreenshotService.getInstance();
            if ("peek".equals(action)) {
                if (svc != null) { svc.doScreenshot(serverUrl, token); ok = true; result = "screenshot requested"; }
                else result = "accessibility service not ready";
            } else if ("open_app".equals(action)) {
                if (pkg == null || pkg.length() == 0) pkg = AppPrefs.packageForApp(ctx, app);
                ok = openPackage(ctx, pkg); result = ok ? "opened " + pkg : "cannot open " + pkg;
            } else if ("home".equals(action)) { ok = svc != null && svc.doHome(); result = "home";
            } else if ("back".equals(action)) { ok = svc != null && svc.doBack(); result = "back";
            } else if ("recents".equals(action)) { ok = svc != null && svc.doRecents(); result = "recents";
            } else if ("tap".equals(action)) { ok = svc != null && svc.doTap(x, y); result = "tap";
            } else if ("swipe".equals(action)) { ok = svc != null && svc.doSwipe(x1, y1, x2, y2, duration); result = "swipe";
            } else if ("set_alarm".equals(action)) { ok = setAlarm(ctx, hour, minute, message, vibrate, skipUi); result = ok ? "alarm " + hour + ":" + minute : "cannot set alarm";
            } else if ("send_notification".equals(action)) { ok = showReminderNotification(ctx, title, message); result = ok ? "notification sent" : "notification permission missing";
            } else { ok = true; result = "noop"; }
        } catch (Exception e) { result = ScreenshotService.shortMsg(e); }
        DebugState.append(ctx, "执行命令 " + action + "：" + result);
        try { reportCommand(ctx, serverUrl, token, id, ok, result); uploadState(serverUrl, token, ctx); } catch (Exception ignored) { }
    }

    private static boolean openPackage(Context ctx, String pkg) {
        if (pkg == null || pkg.trim().isEmpty()) return false;
        try { PackageManager pm = ctx.getPackageManager(); Intent i = pm.getLaunchIntentForPackage(pkg.trim()); if (i == null) return false; i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); ctx.startActivity(i); return true; } catch (Exception e) { return false; }
    }

    public static boolean showReminderNotification(Context ctx, String title, String message) {
        try {
            if (Build.VERSION.SDK_INT >= 33 && ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false;
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(REMINDER_CHANNEL_ID, "掌心窗提醒", NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription("来自掌心窗的生活提醒与轻通知");
                nm.createNotificationChannel(channel);
            }
            Intent open = new Intent(ctx, MainActivity.class);
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pi = PendingIntent.getActivity(ctx, 0, open, Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
            String safeTitle = (title == null || title.trim().isEmpty()) ? "掌心窗提醒" : title.trim();
            String safeMessage = (message == null || message.trim().isEmpty()) ? "看一眼这里。" : message.trim();
            Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(ctx, REMINDER_CHANNEL_ID) : new Notification.Builder(ctx);
            Notification n = builder
                    .setContentTitle(safeTitle)
                    .setContentText(safeMessage)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build();
            nm.notify((int)(System.currentTimeMillis() % Integer.MAX_VALUE), n);
            return true;
        } catch (Exception e) { return false; }
    }

    private static boolean setAlarm(Context ctx, int hour, int minute, String message, boolean vibrate, boolean skipUi) {
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return false;
        try {
            Intent i = new Intent(AlarmClock.ACTION_SET_ALARM);
            i.putExtra(AlarmClock.EXTRA_HOUR, hour);
            i.putExtra(AlarmClock.EXTRA_MINUTES, minute);
            i.putExtra(AlarmClock.EXTRA_MESSAGE, message == null || message.length() == 0 ? "掌心窗闹钟" : message);
            i.putExtra(AlarmClock.EXTRA_VIBRATE, vibrate);
            i.putExtra(AlarmClock.EXTRA_SKIP_UI, skipUi);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (i.resolveActivity(ctx.getPackageManager()) == null) return false;
            ctx.startActivity(i);
            return true;
        } catch (Exception e) { return false; }
    }

    private static void uploadState(String serverUrl, String token, Context ctx) throws Exception {
        JSONObject state = LifeState.collect(ctx);
        postJson(serverUrl + "/api/device/state", token, state);
        ActiveReminder.evaluate(ctx, state);
    }
    private static void uploadState(String serverUrl, String token) throws Exception {
        Context ctx = ScreenshotService.getInstance();
        if (ctx == null) return;
        uploadState(serverUrl, token, ctx);
    }

    private static void reportCommand(Context ctx, String serverUrl, String token, String id, boolean ok, String result) throws Exception {
        if (id == null || id.length() == 0) return;
        JSONObject report = new JSONObject(); report.put("device_id", AppPrefs.device(ctx)); report.put("command_id", id); report.put("ok", ok); report.put("result", result);
        postJson(serverUrl + "/api/device/report", token, report);
    }

    private static String postJson(String urlStr, String token, JSONObject obj) throws Exception {
        HttpURLConnection conn = (HttpURLConnection)new URL(urlStr).openConnection(); conn.setRequestMethod("POST"); conn.setRequestProperty("Content-Type", "application/json; charset=utf-8"); conn.setRequestProperty("X-Auth-Token", token); conn.setDoOutput(true);
        byte[] data = obj.toString().getBytes(StandardCharsets.UTF_8); try (OutputStream os = conn.getOutputStream()) { os.write(data); }
        InputStream is = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(); if (is != null) { byte[] buf = new byte[1024]; int n; while ((n = is.read(buf)) > 0) bos.write(buf,0,n); }
        return new String(bos.toByteArray(), "UTF-8");
    }

    private void createNotificationChannel() { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { NotificationManager nm = getSystemService(NotificationManager.class); NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "掌心窗", NotificationManager.IMPORTANCE_LOW); channel.setDescription("掌心窗正在等待你授权的截图与手机动作请求"); nm.createNotificationChannel(channel); NotificationChannel reminder = new NotificationChannel(REMINDER_CHANNEL_ID, "掌心窗提醒", NotificationManager.IMPORTANCE_DEFAULT); reminder.setDescription("来自掌心窗的生活提醒与轻通知"); nm.createNotificationChannel(reminder); } }
    private Notification buildNotification(String text) { Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this); return builder.setContentTitle("掌心窗运行中").setContentText(text).setSmallIcon(android.R.drawable.ic_menu_view).setOngoing(true).build(); }
    @Override public void onDestroy() { running = false; DebugState.append(this, "服务已销毁/停止"); if (pollThread != null) pollThread.quitSafely(); super.onDestroy(); }
}
