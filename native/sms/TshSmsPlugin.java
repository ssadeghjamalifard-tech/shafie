package __PKG__;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * افزونه‌ی پیامک برای «تقویم شیعه / دفتر من».
 * متدها: checkPermissions، requestPermissions (alias: sms)، list، takeNew، setFilters
 * رویداد: smsReceived (وقتی برنامه باز است)
 */
@CapacitorPlugin(
    name = "TshSms",
    permissions = {
        @Permission(alias = "sms", strings = { Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS })
    }
)
public class TshSmsPlugin extends Plugin {

    static final String PREF = "tsh_sms";
    static TshSmsPlugin instance;

    @Override
    public void load() {
        instance = this;
    }

    static void emit(JSObject o) {
        try {
            if (instance != null) instance.notifyListeners("smsReceived", o);
        } catch (Exception ignored) { }
    }

    private boolean granted() {
        return getPermissionState("sms") == PermissionState.GRANTED;
    }

    /** پیامک‌های دریافتی (برای انتخاب نمونه و ثبت ۳۰ روز گذشته). */
    @PluginMethod
    public void list(PluginCall call) {
        if (!granted()) {
            call.reject("permission");
            return;
        }
        long since = call.getLong("since", 0L);
        int limit = call.getInt("limit", 50);
        JSArray arr = new JSArray();
        Cursor c = null;
        try {
            c = getContext().getContentResolver().query(
                Uri.parse("content://sms/inbox"),
                new String[] { "_id", "address", "body", "date" },
                "date > ?",
                new String[] { String.valueOf(since) },
                "date DESC"
            );
            if (c != null) {
                int n = 0;
                while (c.moveToNext() && n < limit) {
                    JSObject o = new JSObject();
                    o.put("id", c.getString(0));
                    o.put("address", c.getString(1));
                    o.put("body", c.getString(2));
                    o.put("date", c.getLong(3));
                    arr.put(o);
                    n++;
                }
            }
        } catch (Exception e) {
            call.reject("read failed: " + e.getMessage());
            return;
        } finally {
            if (c != null) c.close();
        }
        JSObject ret = new JSObject();
        ret.put("messages", arr);
        call.resolve(ret);
    }

    /** پیامک‌های رسیده‌ی بانک‌های آموزش‌دیده که هنوز به برنامه تحویل نشده‌اند. */
    @PluginMethod
    public void takeNew(PluginCall call) {
        SharedPreferences sp = getContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String q = sp.getString("q", "[]");
        sp.edit().putString("q", "[]").apply();
        JSObject ret = new JSObject();
        try {
            ret.put("messages", new JSArray(q));
        } catch (Exception e) {
            ret.put("messages", new JSArray());
        }
        call.resolve(ret);
    }

    /** فهرست فرستنده‌ها/نشانه‌هایی که اجازه‌ی ذخیره‌ی پیامکشان داده شده؛ بقیه‌ی پیامک‌ها هرگز ذخیره نمی‌شود. */
    @PluginMethod
    public void setFilters(PluginCall call) {
        JSArray se = call.getArray("senders", new JSArray());
        JSArray si = call.getArray("sigs", new JSArray());
        SharedPreferences sp = getContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
        sp.edit().putString("senders", se.toString()).putString("sigs", si.toString()).apply();
        call.resolve();
    }
}
