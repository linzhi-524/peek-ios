package dev.linjian.peek;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.AlarmClock;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

public class MainActivity extends Activity {
    private TextView statusText, debugText, lifeStatusText;
    private Button toggleButton, accessibilityButton, usageAccessButton, testButton, openXhsButton, homeButton, backButton, alarmTestButton, notifyTestButton, refreshLifeButton;
    private CheckBox remindersEnabled, batteryRuleEnabled, screenRuleEnabled, waterRuleEnabled, restRuleEnabled, cycleEnabled;
    private Button tabSettings, tabSee, tabControl, tabLife, tabDebug;
    private View sectionSettings, sectionSee, sectionControl, sectionLife, sectionDebug;
    private EditText serverUrl, tokenInput, deviceInput, intervalInput, cityInput, weatherInput;
    private EditText batteryThresholdInput, screenThresholdInput, waterIntervalInput, restIntervalInput;
    private EditText lastPeriodStartInput, cycleLengthInput, periodLengthInput, cycleRemindBeforeInput;
    private boolean serviceRunning = false;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private final Runnable refreshTick = new Runnable() {
        @Override public void run() { serviceRunning = CompanionService.isRunning(); updateUI(); uiHandler.postDelayed(this, 1000); }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        debugText = findViewById(R.id.debugText);
        lifeStatusText = findViewById(R.id.lifeStatusText);
        toggleButton = findViewById(R.id.toggleButton);
        accessibilityButton = findViewById(R.id.accessibilityButton);
        usageAccessButton = findViewById(R.id.usageAccessButton);
        testButton = findViewById(R.id.testButton);
        openXhsButton = findViewById(R.id.openXhsButton);
        homeButton = findViewById(R.id.homeButton);
        backButton = findViewById(R.id.backButton);
        alarmTestButton = findViewById(R.id.alarmTestButton);
        notifyTestButton = findViewById(R.id.notifyTestButton);
        refreshLifeButton = findViewById(R.id.refreshLifeButton);
        tabSettings = findViewById(R.id.tabSettings);
        tabSee = findViewById(R.id.tabSee);
        tabControl = findViewById(R.id.tabControl);
        tabLife = findViewById(R.id.tabLife);
        tabDebug = findViewById(R.id.tabDebug);
        sectionSettings = findViewById(R.id.sectionSettings);
        sectionSee = findViewById(R.id.sectionSee);
        sectionControl = findViewById(R.id.sectionControl);
        sectionLife = findViewById(R.id.sectionLife);
        sectionDebug = findViewById(R.id.sectionDebug);
        serverUrl = findViewById(R.id.serverUrl);
        tokenInput = findViewById(R.id.tokenInput);
        deviceInput = findViewById(R.id.deviceInput);
        intervalInput = findViewById(R.id.intervalInput);
        cityInput = findViewById(R.id.cityInput);
        weatherInput = findViewById(R.id.weatherInput);
        remindersEnabled = findViewById(R.id.remindersEnabled);
        batteryRuleEnabled = findViewById(R.id.batteryRuleEnabled);
        screenRuleEnabled = findViewById(R.id.screenRuleEnabled);
        waterRuleEnabled = findViewById(R.id.waterRuleEnabled);
        restRuleEnabled = findViewById(R.id.restRuleEnabled);
        cycleEnabled = findViewById(R.id.cycleEnabled);
        batteryThresholdInput = findViewById(R.id.batteryThresholdInput);
        screenThresholdInput = findViewById(R.id.screenThresholdInput);
        waterIntervalInput = findViewById(R.id.waterIntervalInput);
        restIntervalInput = findViewById(R.id.restIntervalInput);
        lastPeriodStartInput = findViewById(R.id.lastPeriodStartInput);
        cycleLengthInput = findViewById(R.id.cycleLengthInput);
        periodLengthInput = findViewById(R.id.periodLengthInput);
        cycleRemindBeforeInput = findViewById(R.id.cycleRemindBeforeInput);

        SharedPreferences prefs = getSharedPreferences(AppPrefs.PREFS, MODE_PRIVATE);
        serverUrl.setText(prefs.getString(AppPrefs.KEY_SERVER, ""));
        tokenInput.setText(prefs.getString(AppPrefs.KEY_TOKEN, ""));
        deviceInput.setText(prefs.getString(AppPrefs.KEY_DEVICE, "my-phone"));
        intervalInput.setText(String.valueOf(prefs.getInt(AppPrefs.KEY_INTERVAL, 1500)));
        cityInput.setText(prefs.getString(AppPrefs.KEY_CITY, ""));
        weatherInput.setText(prefs.getString(AppPrefs.KEY_WEATHER_NOTE, ""));
        remindersEnabled.setChecked(prefs.getBoolean(AppPrefs.KEY_ACTIVE_REMINDERS, true));
        batteryRuleEnabled.setChecked(prefs.getBoolean(AppPrefs.KEY_RULE_BATTERY, true));
        screenRuleEnabled.setChecked(prefs.getBoolean(AppPrefs.KEY_RULE_SCREEN, true));
        waterRuleEnabled.setChecked(prefs.getBoolean(AppPrefs.KEY_RULE_WATER, false));
        restRuleEnabled.setChecked(prefs.getBoolean(AppPrefs.KEY_RULE_REST, true));
        cycleEnabled.setChecked(prefs.getBoolean(AppPrefs.KEY_CYCLE_ENABLED, false));
        batteryThresholdInput.setText(String.valueOf(prefs.getInt(AppPrefs.KEY_BATTERY_THRESHOLD, 20)));
        screenThresholdInput.setText(String.valueOf(prefs.getInt(AppPrefs.KEY_SCREEN_THRESHOLD_MIN, 240)));
        waterIntervalInput.setText(String.valueOf(prefs.getInt(AppPrefs.KEY_WATER_INTERVAL_MIN, 120)));
        restIntervalInput.setText(String.valueOf(prefs.getInt(AppPrefs.KEY_REST_INTERVAL_MIN, 90)));
        lastPeriodStartInput.setText(prefs.getString(AppPrefs.KEY_LAST_PERIOD_START, ""));
        cycleLengthInput.setText(String.valueOf(prefs.getInt(AppPrefs.KEY_CYCLE_LENGTH, 30)));
        periodLengthInput.setText(String.valueOf(prefs.getInt(AppPrefs.KEY_PERIOD_LENGTH, 6)));
        cycleRemindBeforeInput.setText(String.valueOf(prefs.getInt(AppPrefs.KEY_CYCLE_REMIND_BEFORE, 3)));

        DebugState.append(this, "掌心窗 v0.1.8 主动提醒层已打开");
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 13);
        serviceRunning = CompanionService.isRunning();
        updateUI();

        accessibilityButton.setOnClickListener(v -> openAccessibilitySettings());
        usageAccessButton.setOnClickListener(v -> openUsageAccessSettings());
        toggleButton.setOnClickListener(v -> { if (serviceRunning) stopCompanionService(); else startCompanionService(); });
        refreshLifeButton.setOnClickListener(v -> { saveSettings(); updateUI(); Toast.makeText(this, "已刷新生活状态", Toast.LENGTH_SHORT).show(); });
        testButton.setOnClickListener(v -> testScreenshot());
        openXhsButton.setOnClickListener(v -> openPackage(AppPrefs.packageForApp(this, "小红书")));
        homeButton.setOnClickListener(v -> { ScreenshotService svc = ScreenshotService.getInstance(); toast(svc != null && svc.doHome()); });
        backButton.setOnClickListener(v -> { ScreenshotService svc = ScreenshotService.getInstance(); toast(svc != null && svc.doBack()); });
        alarmTestButton.setOnClickListener(v -> testAlarm());
        notifyTestButton.setOnClickListener(v -> testNotification());
        tabSettings.setOnClickListener(v -> showTab("settings"));
        tabSee.setOnClickListener(v -> showTab("see"));
        tabControl.setOnClickListener(v -> showTab("control"));
        tabLife.setOnClickListener(v -> showTab("life"));
        tabDebug.setOnClickListener(v -> showTab("debug"));
        showTab("life");
    }

    private void showTab(String tab) {
        sectionSettings.setVisibility("settings".equals(tab) ? View.VISIBLE : View.GONE);
        sectionSee.setVisibility("see".equals(tab) ? View.VISIBLE : View.GONE);
        sectionControl.setVisibility("control".equals(tab) ? View.VISIBLE : View.GONE);
        sectionLife.setVisibility("life".equals(tab) ? View.VISIBLE : View.GONE);
        sectionDebug.setVisibility("debug".equals(tab) ? View.VISIBLE : View.GONE);
        setTabSelected(tabSettings, "settings".equals(tab));
        setTabSelected(tabSee, "see".equals(tab));
        setTabSelected(tabControl, "control".equals(tab));
        setTabSelected(tabLife, "life".equals(tab));
        setTabSelected(tabDebug, "debug".equals(tab));
    }

    private void setTabSelected(Button b, boolean selected) {
        b.setTextColor(selected ? 0xFFFFFFFF : 0xFF6A7B76);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) b.setBackgroundResource(selected ? R.drawable.pill_primary : R.drawable.pill_soft);
        else b.setBackgroundColor(selected ? 0xFF7AC7B7 : 0xFFFFEFF4);
    }

    @Override protected void onResume() { super.onResume(); serviceRunning = CompanionService.isRunning(); updateUI(); uiHandler.removeCallbacks(refreshTick); uiHandler.post(refreshTick); }
    @Override protected void onPause() { uiHandler.removeCallbacks(refreshTick); super.onPause(); }

    private void saveSettings() {
        getSharedPreferences(AppPrefs.PREFS, MODE_PRIVATE).edit()
                .putString(AppPrefs.KEY_SERVER, serverUrl.getText().toString().trim())
                .putString(AppPrefs.KEY_TOKEN, tokenInput.getText().toString().trim())
                .putString(AppPrefs.KEY_DEVICE, deviceInput.getText().toString().trim().isEmpty() ? "my-phone" : deviceInput.getText().toString().trim())
                .putInt(AppPrefs.KEY_INTERVAL, parseInterval(intervalInput.getText().toString().trim()))
                .putString(AppPrefs.KEY_CITY, cityInput.getText().toString().trim())
                .putString(AppPrefs.KEY_WEATHER_NOTE, weatherInput.getText().toString().trim())
                .putBoolean(AppPrefs.KEY_ACTIVE_REMINDERS, remindersEnabled.isChecked())
                .putBoolean(AppPrefs.KEY_RULE_BATTERY, batteryRuleEnabled.isChecked())
                .putInt(AppPrefs.KEY_BATTERY_THRESHOLD, parseInt(batteryThresholdInput.getText().toString().trim(), 20, 5, 80))
                .putBoolean(AppPrefs.KEY_RULE_SCREEN, screenRuleEnabled.isChecked())
                .putInt(AppPrefs.KEY_SCREEN_THRESHOLD_MIN, parseInt(screenThresholdInput.getText().toString().trim(), 240, 30, 1440))
                .putBoolean(AppPrefs.KEY_RULE_WATER, waterRuleEnabled.isChecked())
                .putInt(AppPrefs.KEY_WATER_INTERVAL_MIN, parseInt(waterIntervalInput.getText().toString().trim(), 120, 30, 720))
                .putBoolean(AppPrefs.KEY_RULE_REST, restRuleEnabled.isChecked())
                .putInt(AppPrefs.KEY_REST_INTERVAL_MIN, parseInt(restIntervalInput.getText().toString().trim(), 90, 30, 720))
                .putBoolean(AppPrefs.KEY_CYCLE_ENABLED, cycleEnabled.isChecked())
                .putString(AppPrefs.KEY_LAST_PERIOD_START, lastPeriodStartInput.getText().toString().trim())
                .putInt(AppPrefs.KEY_CYCLE_LENGTH, parseInt(cycleLengthInput.getText().toString().trim(), 30, 15, 60))
                .putInt(AppPrefs.KEY_PERIOD_LENGTH, parseInt(periodLengthInput.getText().toString().trim(), 6, 1, 14))
                .putInt(AppPrefs.KEY_CYCLE_REMIND_BEFORE, parseInt(cycleRemindBeforeInput.getText().toString().trim(), 3, 0, 14))
                .apply();
    }

    private void startCompanionService() {
        saveSettings();
        String url = serverUrl.getText().toString().trim();
        String token = tokenInput.getText().toString().trim();
        if (url.isEmpty() || token.isEmpty()) { Toast.makeText(this, "请填写服务器地址和 Token", Toast.LENGTH_SHORT).show(); return; }
        if (ScreenshotService.getInstance() == null) { DebugState.append(this, "启动失败：无障碍服务未连接"); Toast.makeText(this, "请先开启掌心窗无障碍服务", Toast.LENGTH_LONG).show(); openAccessibilitySettings(); return; }
        getSharedPreferences(AppPrefs.PREFS, MODE_PRIVATE).edit().putBoolean("user_stopped", false).apply();
        requestIgnoreBatteryOptimization();
        Intent intent = new Intent(this, CompanionService.class);
        intent.putExtra("server_url", url);
        intent.putExtra("token", token);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent); else startService(intent);
        DebugState.append(this, "已请求启动前台服务，生活状态和主动提醒会随轮询运行");
        serviceRunning = true;
        updateUI();
    }

    private void stopCompanionService() {
        getSharedPreferences(AppPrefs.PREFS, MODE_PRIVATE).edit().putBoolean("user_stopped", true).apply();
        stopService(new Intent(this, CompanionService.class));
        DebugState.append(this, "已停止服务");
        serviceRunning = false;
        updateUI();
    }

    private void testScreenshot() {
        saveSettings();
        String url = serverUrl.getText().toString().trim();
        String token = tokenInput.getText().toString().trim();
        ScreenshotService ss = ScreenshotService.getInstance();
        if (url.isEmpty() || token.isEmpty()) { Toast.makeText(this, "请先填写服务器地址和 Token", Toast.LENGTH_SHORT).show(); return; }
        if (ss == null) { DebugState.append(this, "测试失败：无障碍服务未连接"); Toast.makeText(this, "请先开启无障碍服务", Toast.LENGTH_LONG).show(); openAccessibilitySettings(); return; }
        DebugState.append(this, "手动测试：开始截图上传");
        ss.doScreenshot(url, token);
        Toast.makeText(this, "已开始截图测试", Toast.LENGTH_SHORT).show();
        updateUI();
    }

    private void testAlarm() {
        Calendar c = Calendar.getInstance(); c.add(Calendar.MINUTE, 1);
        try {
            Intent i = new Intent(AlarmClock.ACTION_SET_ALARM);
            i.putExtra(AlarmClock.EXTRA_HOUR, c.get(Calendar.HOUR_OF_DAY));
            i.putExtra(AlarmClock.EXTRA_MINUTES, c.get(Calendar.MINUTE));
            i.putExtra(AlarmClock.EXTRA_MESSAGE, "掌心窗测试闹钟：你看到了就说明成功");
            i.putExtra(AlarmClock.EXTRA_VIBRATE, true);
            i.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
            startActivity(i);
            DebugState.append(this, "已请求设置一分钟后的测试闹钟");
        } catch (Exception e) { DebugState.append(this, "测试闹钟失败：" + e.getClass().getSimpleName()); Toast.makeText(this, "闹钟 App 没接住请求", Toast.LENGTH_SHORT).show(); }
    }

    private void testNotification() {
        boolean ok = CompanionService.showReminderNotification(this, "掌心窗测试提醒", "你看到了就说明通知成功");
        DebugState.append(this, ok ? "已弹出本机测试通知" : "通知失败：请允许掌心窗发送通知");
        Toast.makeText(this, ok ? "已发送测试通知" : "请先允许通知权限", Toast.LENGTH_SHORT).show();
        updateUI();
    }

    private boolean openPackage(String pkg) {
        try {
            Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
            if (i == null) { toast(false); return false; }
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            toast(true);
            return true;
        } catch (Exception e) { toast(false); return false; }
    }

    private void toast(boolean ok) { Toast.makeText(this, ok ? "执行成功" : "执行失败，请检查权限/包名", Toast.LENGTH_SHORT).show(); }
    private int parseInterval(String raw) { try { int v = Integer.parseInt(raw); if (v < 700) return 700; if (v > 10000) return 10000; return v; } catch (Exception e) { return 1500; } }
    private int parseInt(String raw, int def, int min, int max) { try { int v = Integer.parseInt(raw); if (v < min) return min; if (v > max) return max; return v; } catch (Exception e) { return def; } }
    private void openAccessibilitySettings() { try { startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); } catch (Exception e) { Toast.makeText(this, "设置 → 无障碍 → 掌心窗", Toast.LENGTH_LONG).show(); } }
    private void openUsageAccessSettings() { try { startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)); } catch (Exception e) { Toast.makeText(this, "设置 → 应用 → 特殊权限 → 使用情况访问", Toast.LENGTH_LONG).show(); } }
    private void requestIgnoreBatteryOptimization() { if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return; try { PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE); if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) { Intent bi = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS); bi.setData(Uri.parse("package:" + getPackageName())); startActivity(bi); } } catch (Exception ignored) { } }

    private void updateUI() {
        boolean accessibilityOk = ScreenshotService.getInstance() != null;
        boolean usageOk = LifeState.hasUsagePermission(this);
        String extra = "\n当前包名：" + ScreenshotService.currentPackage();
        if (serviceRunning) {
            statusText.setText((accessibilityOk ? "运行中：看见/控制/生活状态已待命" : "运行中，但无障碍未开启") + extra);
            statusText.setTextColor(accessibilityOk ? 0xFF2E9D72 : 0xFFFF9800);
            toggleButton.setText("停止");
            toggleButton.setBackgroundColor(0xFFE86D7A);
        } else {
            statusText.setText((accessibilityOk ? "未连接：可以启动" : "未连接：请先开启无障碍服务") + extra);
            statusText.setTextColor(0xFF777777);
            toggleButton.setText("启动");
            toggleButton.setBackgroundResource(R.drawable.pill_primary);
        }
        usageAccessButton.setText(usageOk ? "使用情况权限：已开启" : "打开使用情况访问权限");
        if (lifeStatusText != null) lifeStatusText.setText(LifeState.pretty(this));
        if (debugText != null) debugText.setText(DebugState.get(this));
    }
}
