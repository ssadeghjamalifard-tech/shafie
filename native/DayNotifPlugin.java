package app.tshia.calendar;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.util.Base64;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * اعلان دائمی تاریخ.
 * JS: DayNotif.show({ days:[{key:'2026-11-21', title, body, large(base64 png), small(base64 png)}...], color:'#0E6B57' })
 * داده‌ی ۱۴ روز ذخیره می‌شود و هر نیمه‌شب، پس از روشن شدن گوشی و پس از به‌روزرسانی برنامه، اعلانِ همان روز دوباره ساخته می‌شود.
 * (نسخه‌ی قدیمی show({title,text}) هم هنوز کار می‌کند.)
 */
@CapacitorPlugin(
    name = "DayNotif",
    permissions = {
        @Permission(strings = { Manifest.permission.POST_NOTIFICATIONS }, alias = "notifications")
    }
)
public class DayNotifPlugin extends Plugin {

    static final String CHANNEL_ID = "day_notif_channel";
    static final int NOTIF_ID = 7101;
    static final String PREFS = "day_notif_store";
    static final String ACT = "app.tshia.calendar.DAYNOTIF_TICK";
    static final String FALLBACK_TITLE = "تقویم شیعه";
    static final String STALE_TEXT = "برای به‌روزرسانی تاریخ، برنامه را یک بار باز کنید.";
    static final String REM_CHANNEL = "tsh_remind";
    static final String ACT_SCHED = "app.tshia.calendar.TSH_SCHED_TICK";
    static final String ACT_DEL = "app.tshia.calendar.DAYNOTIF_DISMISSED";

    private String pendingOpen = null;

    /** اگر برنامه با زدن روی یک اعلان یادآوری باز شده باشد، مقدار extra آن را نگه می‌دارد */
    @Override
    public void load() {
        try {
            Intent i = getActivity().getIntent();
            if (i != null && i.hasExtra("tsh_open")) {
                pendingOpen = i.getStringExtra("tsh_open");
                i.removeExtra("tsh_open");
            }
        } catch (Throwable e) { }
    }

    @Override
    protected void handleOnNewIntent(Intent intent) {
        super.handleOnNewIntent(intent);
        try {
            if (intent != null && intent.hasExtra("tsh_open")) {
                pendingOpen = intent.getStringExtra("tsh_open");
                intent.removeExtra("tsh_open");
                JSObject d = new JSObject();
                d.put("open", pendingOpen);
                notifyListeners("tshOpen", d, true);
            }
        } catch (Throwable e) { }
    }

    @PluginMethod
    public void getLaunch(PluginCall call) {
        JSObject r = new JSObject();
        r.put("open", pendingOpen == null ? "" : pendingOpen);
        pendingOpen = null;
        call.resolve(r);
    }

    /** لیست اعلان‌های زمان‌دار: items=[{id,kind,at(ms),title,body,open}] — جای لیست قبلی را می‌گیرد */
    @PluginMethod
    public void setSchedule(PluginCall call) {
        try {
            Context ctx = getContext();
            JSArray arr = call.getArray("items");
            String s = (arr == null) ? "[]" : arr.toString();
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("sched", s).apply();
            fire(ctx);
            call.resolve();
        } catch (Exception e) {
            call.reject("schedule failed: " + e.getMessage());
        }
    }

    @PluginMethod
    public void show(PluginCall call) {
        if (Build.VERSION.SDK_INT >= 33
                && getPermissionState("notifications") != PermissionState.GRANTED) {
            requestPermissionForAlias("notifications", call, "permissionCallback");
            return;
        }
        doShow(call);
    }

    @PermissionCallback
    private void permissionCallback(PluginCall call) {
        if (getPermissionState("notifications") == PermissionState.GRANTED) {
            doShow(call);
        } else {
            call.reject("notification permission denied");
        }
    }

    private void doShow(PluginCall call) {
        try {
            Context ctx = getContext();
            SharedPreferences.Editor ed = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
            JSArray arr = call.getArray("days");
            if (arr != null && arr.length() > 0) {
                ed.putString("days", arr.toString());
            } else {
                ed.putString("days", "[]");
                ed.putString("title", call.getString("title", FALLBACK_TITLE));
                ed.putString("text", call.getString("text", ""));
            }
            ed.putString("color", call.getString("color", ""));
            ed.putBoolean("on", true);
            ed.apply();

            render(ctx);

            JSObject ret = new JSObject();
            ret.put("shown", true);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("show failed: " + e.getMessage());
        }
    }

    @PluginMethod
    public void hide(PluginCall call) {
        try {
            Context ctx = getContext();
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("on", false).apply();
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.cancel(NOTIF_ID);
            cancelAlarm(ctx);
            call.resolve();
        } catch (Exception e) {
            call.reject("hide failed: " + e.getMessage());
        }
    }

    /** ساخت اعلانِ امروز از روی داده‌ی ذخیره‌شده؛ از برنامه و از گیرنده‌ی پس‌زمینه صدا زده می‌شود */
    static void render(Context ctx) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String days = sp.getString("days", null);
            if (days == null || !sp.getBoolean("on", true)) return;

            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            String key = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            JSONObject hit = null;
            JSONArray a = new JSONArray(days);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                if (key.equals(o.optString("key"))) { hit = o; break; }
            }

            String title;
            String text;
            Bitmap large = null;
            Bitmap small = null;
            if (hit != null) {
                title = hit.optString("title", FALLBACK_TITLE);
                text = hit.optString("body", "");
                large = decode(hit.optString("large", ""));
                small = decode(hit.optString("small", ""));
            } else {
                title = sp.getString("title", FALLBACK_TITLE);
                text = sp.getString("text", "");
                if (text.length() == 0) text = STALE_TEXT;
            }

            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel ch = new NotificationChannel(
                        CHANNEL_ID, "تاریخ روز", NotificationManager.IMPORTANCE_LOW);
                ch.setShowBadge(false);
                nm.createNotificationChannel(ch);
            }

            Intent open = new Intent(ctx, MainActivity.class);
            open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pi = PendingIntent.getActivity(ctx, 0, open, flags);

            Notification.Builder b = Build.VERSION.SDK_INT >= 26
                    ? new Notification.Builder(ctx, CHANNEL_ID)
                    : new Notification.Builder(ctx);

            boolean iconSet = false;
            if (small != null && Build.VERSION.SDK_INT >= 23) {
                try {
                    b.setSmallIcon(Icon.createWithBitmap(small));
                    iconSet = true;
                } catch (Throwable e) { iconSet = false; }
            }
            if (!iconSet) b.setSmallIcon(ctx.getApplicationInfo().icon);
            if (large != null) b.setLargeIcon(large);

            int col = parseColor(sp.getString("color", ""));
            if (col != 0) b.setColor(col);

            b.setContentTitle(title)
                    .setContentText(text)
                    .setStyle(new Notification.BigTextStyle().bigText(text))
                    .setContentIntent(pi)
                    .setDeleteIntent(delIntent(ctx))
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setShowWhen(false)
                    .setVisibility(Notification.VISIBILITY_PUBLIC);
            if (Build.VERSION.SDK_INT < 26) b.setPriority(Notification.PRIORITY_LOW);

            nm.notify(NOTIF_ID, b.build());
            schedule(ctx);
        } catch (Throwable e) {
            // اعلان نباید برنامه را از کار بیندازد
        }
    }

    private static Bitmap decode(String b64) {
        try {
            if (b64 == null || b64.length() < 8) return null;
            byte[] d = Base64.decode(b64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(d, 0, d.length);
        } catch (Throwable e) {
            return null;
        }
    }

    private static int parseColor(String s) {
        try {
            if (s == null || s.length() == 0) return 0;
            return Color.parseColor(s);
        } catch (Throwable e) {
            return 0;
        }
    }

    private static PendingIntent alarmIntent(Context ctx) {
        Intent i = new Intent(ctx, Rcv.class);
        i.setAction(ACT);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(ctx, 71, i, flags);
    }

    /** اگر کاربر اعلان را کنار زد (اندروید ۱۴ به بالا یا «پاک کردن همه»)، بلافاصله دوباره ساخته می‌شود */
    private static PendingIntent delIntent(Context ctx) {
        Intent i = new Intent(ctx, Rcv.class);
        i.setAction(ACT_DEL);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(ctx, 73, i, flags);
    }

    /** بازسازی اعلان درست بعد از نیمه‌شب (تاریخ عوض می‌شود) و هر ۳۰ دقیقه برای اطمینان از ماندن اعلان */
    static void schedule(Context ctx) {
        try {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_YEAR, 1);
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 3);
            c.set(Calendar.MILLISECOND, 0);
            long t = c.getTimeInMillis();
            long soon = System.currentTimeMillis() + 30L * 60L * 1000L;
            if (soon < t) t = soon;
            if (Build.VERSION.SDK_INT >= 23) {
                am.setAndAllowWhileIdle(AlarmManager.RTC, t, alarmIntent(ctx));
            } else {
                am.set(AlarmManager.RTC, t, alarmIntent(ctx));
            }
        } catch (Throwable e) { }
    }

    static void cancelAlarm(Context ctx) {
        try {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am != null) am.cancel(alarmIntent(ctx));
        } catch (Throwable e) { }
    }

    // ---------- اعلان‌های زمان‌دار (ذکر روزانه، چیستان، روایت) ----------
    private static PendingIntent schedIntent(Context ctx) {
        Intent i = new Intent(ctx, Rcv.class);
        i.setAction(ACT_SCHED);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(ctx, 72, i, flags);
    }

    static void armAt(Context ctx, long t) {
        try {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            if (Build.VERSION.SDK_INT >= 23) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, schedIntent(ctx));
            } else {
                am.set(AlarmManager.RTC_WAKEUP, t, schedIntent(ctx));
            }
        } catch (Throwable e) { }
    }

    static void cancelSched(Context ctx) {
        try {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am != null) am.cancel(schedIntent(ctx));
        } catch (Throwable e) { }
    }

    /** اعلان‌های رسیده را نشان می‌دهد، از لیست برمی‌دارد و زنگ بعدی را می‌گذارد */
    static void fire(Context ctx) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            JSONArray a = new JSONArray(sp.getString("sched", "[]"));
            long now = System.currentTimeMillis();
            JSONArray keep = new JSONArray();
            java.util.HashMap<String, JSONObject> due = new java.util.HashMap<String, JSONObject>();
            long next = Long.MAX_VALUE;
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                long at = o.optLong("at", 0);
                if (at <= now + 2000) {
                    // فقط اعلانی که حداکثر ۶ ساعت از وقتش گذشته؛ از هر نوع، تازه‌ترین
                    if (now - at < 6L * 3600L * 1000L) {
                        String k = o.optString("kind", "x");
                        JSONObject p = due.get(k);
                        if (p == null || p.optLong("at", 0) < at) due.put(k, o);
                    }
                } else {
                    keep.put(o);
                    if (at < next) next = at;
                }
            }
            sp.edit().putString("sched", keep.toString()).apply();
            for (JSONObject o : due.values()) postItem(ctx, o);
            if (next != Long.MAX_VALUE) armAt(ctx, next); else cancelSched(ctx);
        } catch (Throwable e) { }
    }

    private static void postItem(Context ctx, JSONObject o) {
        try {
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            String kind = o.optString("kind", "x");
            int nid = 7209;
            if ("dua".equals(kind)) nid = 7201;
            else if ("joke".equals(kind)) nid = 7202;
            else if ("rav".equals(kind)) nid = 7203;

            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel ch = new NotificationChannel(
                        REM_CHANNEL, "یادآوری‌ها", NotificationManager.IMPORTANCE_DEFAULT);
                ch.setShowBadge(false);
                nm.createNotificationChannel(ch);
            }

            Intent open = new Intent(ctx, MainActivity.class);
            open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            open.putExtra("tsh_open", o.optString("open", kind));
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pi = PendingIntent.getActivity(ctx, nid, open, flags);

            String title = o.optString("title", FALLBACK_TITLE);
            String text = o.optString("body", "");

            Notification.Builder b = Build.VERSION.SDK_INT >= 26
                    ? new Notification.Builder(ctx, REM_CHANNEL)
                    : new Notification.Builder(ctx);
            b.setSmallIcon(ctx.getApplicationInfo().icon)
                    .setContentTitle(title)
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .setShowWhen(true);
            if (text.length() > 0) {
                b.setContentText(text);
                b.setStyle(new Notification.BigTextStyle().bigText(text));
            }
            int col = parseColor(ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("color", ""));
            if (col != 0) b.setColor(col);
            nm.notify(nid, b.build());
        } catch (Throwable e) { }
    }

    /** روشن شدن گوشی، به‌روزرسانی برنامه و رسیدن نیمه‌شب → اعلان دوباره ساخته شود */
    public static class Rcv extends BroadcastReceiver {
        @Override
        public void onReceive(Context c, Intent i) {
            try { render(c); } catch (Throwable e) { }
            try { fire(c); } catch (Throwable e) { }
        }
    }
}
