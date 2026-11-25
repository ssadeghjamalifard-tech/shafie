package app.tshia.calendar;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Calendar;
import java.util.Locale;

/** ذخیره و خواندن داده‌ی ابزارک‌ها (SharedPreferences) و محاسبه‌های مشترک */
final class WStore {
  static final String PREF = "tsh_widget", CFG = "tsh_wcfg";
  static final long DAY = 86400000L;
  static final String[] PN = {"اذان صبح", "طلوع آفتاب", "اذان ظهر", "غروب آفتاب", "اذان مغرب", "نیمه‌شب شرعی"};
  static final String[] PS = {"صبح", "طلوع", "ظهر", "غروب", "مغرب", "نیمه‌شب"};

  static JSONObject data(Context c) {
    try {
      String s = c.getSharedPreferences(PREF, 0).getString("d", null);
      return s == null ? null : new JSONObject(s);
    } catch (Exception e) { return null; }
  }

  static void saveData(Context c, String json) {
    c.getSharedPreferences(PREF, 0).edit().putString("d", json).apply();
  }

  static JSONObject cfg(Context c, int id) {
    try {
      String s = c.getSharedPreferences(CFG, 0).getString("w" + id, null);
      if (s != null) return new JSONObject(s);
    } catch (Exception e) { }
    JSONObject o = new JSONObject();
    try { o.put("th", 0); o.put("st", 1); o.put("src", 0); o.put("op", 100); o.put("city", -1); o.put("q", 1); o.put("h", 0); } catch (Exception e) { }
    return o;
  }

  static void saveCfg(Context c, int id, JSONObject o) {
    c.getSharedPreferences(CFG, 0).edit().putString("w" + id, o.toString()).apply();
  }

  static void delCfg(Context c, int id) {
    c.getSharedPreferences(CFG, 0).edit().remove("w" + id).apply();
  }

  static String num(long n) {
    String s = String.valueOf(Math.abs(n)), r = "";
    for (int i = 0; i < s.length(); i++) { if (i > 0 && (s.length() - i) % 3 == 0) r += "\u066C"; r += s.charAt(i); }
    return fa((n < 0 ? "-" : "") + r);
  }

  static String fa(String s) {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      char ch = s.charAt(i);
      b.append(ch >= '0' && ch <= '9' ? (char) (0x06F0 + (ch - '0')) : ch);
    }
    return b.toString();
  }

  static long midnight(long now) {
    Calendar c = Calendar.getInstance();
    c.setTimeInMillis(now);
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    return c.getTimeInMillis();
  }

  static String hm(long t) {
    Calendar c = Calendar.getInstance();
    c.setTimeInMillis(t);
    return fa(String.format(Locale.US, "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)));
  }

  static String dur(long ms) {
    long m = Math.max(0, ms / 60000), h = m / 60;
    return fa((h > 0 ? h + " ساعت و " : "") + (m % 60) + " دقیقه");
  }

  /** اندیس روزِ امروز در آرایه‌ی days (هر روز با ed = نیمه‌شب محلی) یا -1 اگر داده کهنه است */
  static int dayIdx(JSONArray days, long mid) {
    for (int i = 0; i < days.length(); i++) {
      JSONObject o = days.optJSONObject(i);
      if (o != null && Math.abs(o.optLong("ed") - mid) < 12 * 3600000L) return i;
    }
    return -1;
  }

  static int city(JSONObject d, JSONObject cfg) {
    JSONArray pt = d.optJSONArray("pt");
    int n = pt == null ? 0 : pt.length(), ci = cfg.optInt("city", -1);
    if (ci < 0 || ci >= n) ci = d.optInt("dc", 0);
    return Math.max(0, Math.min(ci, Math.max(0, n - 1)));
  }

  static String cityName(JSONObject d, int ci) {
    JSONArray cn = d.optJSONArray("cn");
    return cn == null ? "" : cn.optString(ci, "");
  }

  /** {اذان قبلی، اذان بعدی، اندیس (۰ صبح، ۲ ظهر، ۴ مغرب)} یا null */
  static long[] nextAdhan(JSONArray pts, long now) {
    long prev = 0;
    int[] ks = {0, 2, 4};
    for (int i = 0; i < pts.length(); i++) {
      JSONArray t = pts.optJSONArray(i);
      if (t == null) continue;
      for (int k : ks) {
        long v = t.optLong(k);
        if (v > now) return new long[] {prev == 0 ? v - 6 * 3600000L : prev, v, k};
        prev = v;
      }
    }
    return null;
  }
}
