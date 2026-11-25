package app.tshia.calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import org.json.JSONArray;
import org.json.JSONObject;

/** زمان‌بند به‌روزرسانی ابزارک‌ها: هر ۱۵ دقیقه، سر اذان بعدی، نیمه‌شب، تغییر ساعت و روشن شدن گوشی */
public class WTick extends BroadcastReceiver {
  static final String ACT = "app.tshia.calendar.WTICK";

  @Override public void onReceive(Context c, Intent i) {
    try { WBase.updateAll(c); } catch (Throwable e) { }
    schedule(c);
  }

  static PendingIntent pi(Context c) {
    Intent i = new Intent(c, WTick.class);
    i.setAction(ACT);
    return PendingIntent.getBroadcast(c, 7, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
  }

  static void schedule(Context c) {
    try {
      AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
      if (am == null) return;
      PendingIntent p = pi(c);
      if (WBase.count(c) == 0) { am.cancel(p); return; }
      long now = System.currentTimeMillis();
      long t = now + 15 * 60000L;
      long mid = WStore.midnight(now) + WStore.DAY + 2000;
      if (mid < t) t = mid;
      JSONObject d = WStore.data(c);
      if (d != null) {
        JSONArray pts = d.getJSONArray("pt").getJSONArray(d.optInt("dc", 0));
        long[] nx = WStore.nextAdhan(pts, now);
        if (nx != null && nx[1] + 2000 < t) t = nx[1] + 2000;
      }
      if (t < now + 5000) t = now + 5000;
      am.setAndAllowWhileIdle(AlarmManager.RTC, t, p);
    } catch (Throwable e) { }
  }
}
