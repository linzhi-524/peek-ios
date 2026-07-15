package dev.linjian.peek;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPrefs {
    public static final String PREFS = "linjian_peek";
    public static final String KEY_SERVER = "server_url";
    public static final String KEY_TOKEN = "token";
    public static final String KEY_DEVICE = "device_id";
    public static final String KEY_INTERVAL = "poll_interval_ms";
    public static final String KEY_CITY = "life_city";
    public static final String KEY_WEATHER_NOTE = "life_weather_note";
    public static final String KEY_ACTIVE_REMINDERS = "active_reminders_enabled";
    public static final String KEY_RULE_BATTERY = "rule_battery_enabled";
    public static final String KEY_BATTERY_THRESHOLD = "rule_battery_threshold";
    public static final String KEY_RULE_SCREEN = "rule_screen_enabled";
    public static final String KEY_SCREEN_THRESHOLD_MIN = "rule_screen_threshold_min";
    public static final String KEY_RULE_WATER = "rule_water_enabled";
    public static final String KEY_WATER_INTERVAL_MIN = "rule_water_interval_min";
    public static final String KEY_RULE_REST = "rule_rest_enabled";
    public static final String KEY_REST_INTERVAL_MIN = "rule_rest_interval_min";
    public static final String KEY_CYCLE_ENABLED = "cycle_enabled";
    public static final String KEY_LAST_PERIOD_START = "cycle_last_period_start";
    public static final String KEY_CYCLE_LENGTH = "cycle_length_days";
    public static final String KEY_PERIOD_LENGTH = "cycle_period_length_days";
    public static final String KEY_CYCLE_REMIND_BEFORE = "cycle_remind_before_days";

    public static SharedPreferences get(Context ctx) { return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    public static String server(Context ctx) { return get(ctx).getString(KEY_SERVER, ""); }
    public static String token(Context ctx) { return get(ctx).getString(KEY_TOKEN, ""); }
    public static String device(Context ctx) { return get(ctx).getString(KEY_DEVICE, "my-phone"); }
    public static int interval(Context ctx) { return Math.max(700, get(ctx).getInt(KEY_INTERVAL, 1500)); }

    public static String packageForApp(Context ctx, String app) {
        String raw = app == null ? "" : app.trim();
        String key = raw.toLowerCase();
        String def;
        switch (key) {
            case "xiaohongshu": case "xhs": case "小红书": def = "com.xingin.xhs"; break;
            case "wechat": case "微信": def = "com.tencent.mm"; break;
            case "qq": def = "com.tencent.mobileqq"; break;
            case "douyin": case "抖音": def = "com.ss.android.ugc.aweme"; break;
            case "chatgpt": def = "com.openai.chatgpt"; break;
            case "speedcat": def = get(ctx).getString("pkg_speedcat", ""); break;
            default: def = "";
        }
        return get(ctx).getString("pkg_" + key, def);
    }
}
