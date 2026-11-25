package __PKG__;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import com.getcapacitor.JSObject;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

/** دریافت پیامک: فقط پیامک فرستنده‌های آموزش‌دیده در صف می‌ماند تا برنامه آن را بخواند. */
public class SmsReceiver extends BroadcastReceiver {

    private static String norm(String a) {
        if (a == null) return "";
        String s = a.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        if (s.startsWith("+98")) s = "0" + s.substring(3);
        else if (s.startsWith("98") && s.length() > 10) s = "0" + s.substring(2);
        return s;
    }

    private static boolean allowed(SharedPreferences sp, String addr, String body) {
        try {
            JSONArray se = new JSONArray(sp.getString("senders", "[]"));
            String a = norm(addr);
            for (int i = 0; i < se.length(); i++) {
                if (norm(se.getString(i)).equals(a) && a.length() > 0) return true;
            }
            JSONArray si = new JSONArray(sp.getString("sigs", "[]"));
            String b = body == null ? "" : body.toLowerCase(Locale.ROOT);
            for (int i = 0; i < si.length(); i++) {
                String g = si.getString(i).toLowerCase(Locale.ROOT);
                if (g.length() > 0 && b.contains(g)) return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        try {
            if (intent == null || !Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) return;
            SmsMessage[] msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent);
            if (msgs == null || msgs.length == 0) return;
            String addr = msgs[0].getOriginatingAddress();
            long ts = msgs[0].getTimestampMillis();
            StringBuilder body = new StringBuilder();
            for (SmsMessage m : msgs) body.append(m.getMessageBody());

            SharedPreferences sp = ctx.getSharedPreferences(TshSmsPlugin.PREF, Context.MODE_PRIVATE);
            if (!allowed(sp, addr, body.toString())) return;

            JSONObject o = new JSONObject();
            o.put("id", "r" + ts);
            o.put("address", addr == null ? "" : addr);
            o.put("body", body.toString());
            o.put("date", ts);

            JSONArray q = new JSONArray(sp.getString("q", "[]"));
            q.put(o);
            // حداکثر ۲۰۰ پیامک در صف
            if (q.length() > 200) {
                JSONArray t = new JSONArray();
                for (int i = q.length() - 200; i < q.length(); i++) t.put(q.get(i));
                q = t;
            }
            sp.edit().putString("q", q.toString()).apply();

            JSObject ev = new JSObject();
            ev.put("address", addr == null ? "" : addr);
            TshSmsPlugin.emit(ev);
        } catch (Exception ignored) { }
    }
}
