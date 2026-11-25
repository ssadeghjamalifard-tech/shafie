package app.tshia.calendar;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.SpannableString;
import android.text.style.StrikethroughSpan;
import android.widget.RemoteViews;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Locale;

/** مدیریت برنامه‌ریزی: کارهای امروز با تیک «انجام شد» از صفحه‌ی اصلی (تغییرها هنگام باز شدن برنامه به برنامه‌ریزی منتقل می‌شود) */
public class PlanWidget extends WBase {
  static final String ACT = "app.tshia.calendar.PLAN_TOGGLE", Q = "tsh_planq";

  @Override int layout() { return R.layout.tw_plan; }

  static String mm(int m) {
    m = ((m % 1440) + 1440) % 1440;
    return WStore.fa(String.format(Locale.US, "%02d:%02d", m / 60, m % 60));
  }

  static String dur(int m) {
    int h = m / 60, r = m % 60;
    return WStore.fa((h > 0 ? h + " ساعت" : "") + (h > 0 && r > 0 ? " و " : "") + (r > 0 ? r + " دقیقه" : ""));
  }

  @Override RemoteViews build(Context c, int id, int sz, boolean dark, JSONObject d, JSONObject cfg) throws Exception {
    if (d == null || d.optJSONObject("plan") == null)
      return shell(c, layout(), cfg, dark, "برنامه‌ی «تقویم شیعه» را یک بار باز کنید تا برنامه‌ی امروز بیاید");
    JSONArray days = d.getJSONObject("plan").optJSONArray("days");
    long mid = WStore.midnight(System.currentTimeMillis());
    int di = days == null ? -1 : WStore.dayIdx(days, mid);
    if (di < 0) return shell(c, layout(), cfg, dark, "داده‌ی برنامه قدیمی شده؛ برنامه را باز کنید");
    JSONObject day = days.getJSONObject(di);
    JSONArray ts = day.optJSONArray("tasks");
    ArrayList<JSONObject> todo = new ArrayList<JSONObject>(), done = new ArrayList<JSONObject>();
    if (ts != null) for (int i = 0; i < ts.length(); i++) {
      JSONObject t = ts.optJSONObject(i);
      if (t == null) continue;
      if (t.optInt("d") == 1) done.add(t); else todo.add(t);
    }
    int total = todo.size() + done.size();
    RemoteViews rv = shell(c, layout(), cfg, dark, null);
    int ink = ink(dark), mute = mute(dark), em = dark ? 0xFFF2D58A : em(false);
    txt(rv, R.id.tw_pl_h, "برنامه‌ی امروز", em, sz == 0 ? 13 : 14);
    txt(rv, R.id.tw_pl_n, total == 0 ? "" : WStore.fa(done.size() + " از " + total), mute, 12);
    progress(rv, dark, total == 0 ? 0 : done.size() * 1000 / total);
    int cap = sz == 0 ? 3 : sz == 1 ? 5 : 8;
    ArrayList<JSONObject> all = new ArrayList<JSONObject>(todo);
    all.addAll(done);
    int[] lc = {0, red(dark), gold(dark), em(dark)};
    for (int i = 1; i <= 8; i++) {
      int rr = rid(c, "tw_pl_r", i), cc = rid(c, "tw_pl_c", i), tt = rid(c, "tw_pl_t", i), mi = rid(c, "tw_pl_m", i);
      boolean on = i <= cap && i <= all.size();
      show(rv, rr, on);
      if (!on) continue;
      JSONObject t = all.get(i - 1);
      boolean dn = t.optInt("d") == 1;
      int l = Math.max(1, Math.min(3, t.optInt("l", 3)));
      txt(rv, cc, dn ? "✔" : "○", dn ? em(dark) : lc[l], 17);
      SpannableString sp = new SpannableString(t.optString("t"));
      if (dn) sp.setSpan(new StrikethroughSpan(), 0, sp.length(), 0);
      txt(rv, tt, sp, dn ? mute : ink, sz == 0 ? 13 : 14);
      txt(rv, mi, sz == 0 ? "" : mm(t.optInt("a")) + " · " + dur(t.optInt("m")), mute, 11);
      Intent in = new Intent(c, PlanWidget.class);
      in.setAction(ACT);
      in.setData(Uri.parse("tsh://plan/" + id + "/" + i));
      in.putExtra("ed", day.optLong("ed"));
      in.putExtra("tid", t.optString("i"));
      rv.setOnClickPendingIntent(rr, PendingIntent.getBroadcast(c, id * 20 + i, in, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
    }
    String f;
    if (total == 0) f = "برنامه‌ای برای امروز ثبت نشده؛ برای افزودن، برنامه را باز کنید";
    else if (all.size() > cap) f = WStore.fa("+" + (all.size() - cap) + " کار دیگر · برای دیدن همه، برنامه را باز کنید");
    else if (todo.isEmpty()) f = "همه‌ی کارهای امروز انجام شد 🎉";
    else f = "برای تیک زدن روی هر کار بزنید";
    txt(rv, R.id.tw_pl_f, f, mute, 11.5f);
    return rv;
  }

  @Override public void onReceive(Context c, Intent i) {
    if (i != null && ACT.equals(i.getAction())) {
      toggle(c, i.getLongExtra("ed", 0), i.getStringExtra("tid"));
      return;
    }
    super.onReceive(c, i);
  }

  /** تیک خوردن/برداشتن یک کار: داده‌ی ابزارک را عوض می‌کند و تغییر را برای برنامه در صف می‌گذارد */
  static void toggle(Context c, long ed, String tid) {
    try {
      JSONObject d = WStore.data(c);
      JSONObject pl = d == null ? null : d.optJSONObject("plan");
      JSONArray days = pl == null ? null : pl.optJSONArray("days");
      if (days == null || tid == null) return;
      for (int i = 0; i < days.length(); i++) {
        JSONObject o = days.optJSONObject(i);
        if (o == null || o.optLong("ed") != ed) continue;
        JSONArray ts = o.optJSONArray("tasks");
        if (ts == null) continue;
        for (int k = 0; k < ts.length(); k++) {
          JSONObject t = ts.optJSONObject(k);
          if (t == null || !tid.equals(t.optString("i"))) continue;
          int nd = t.optInt("d") == 1 ? 0 : 1;
          t.put("d", nd);
          WStore.saveData(c, d.toString());
          queue(c, ed, tid, nd);
        }
      }
    } catch (Throwable e) { }
    try { WBase.updateAll(c); } catch (Throwable e) { }
  }

  static void queue(Context c, long ed, String tid, int nd) throws Exception {
    android.content.SharedPreferences p = c.getSharedPreferences(Q, 0);
    JSONArray old = new JSONArray(p.getString("q", "[]")), n = new JSONArray();
    for (int i = 0; i < old.length(); i++) {
      JSONObject o = old.getJSONObject(i);
      if (!(o.optLong("ed") == ed && tid.equals(o.optString("i")))) n.put(o);
    }
    JSONObject e = new JSONObject();
    e.put("ed", ed); e.put("i", tid); e.put("d", nd);
    n.put(e);
    p.edit().putString("q", n.toString()).apply();
  }

  /** تغییرهای ثبت‌شده از ابزارک (و خالی کردن صف) */
  static String takeQueue(Context c) {
    android.content.SharedPreferences p = c.getSharedPreferences(Q, 0);
    String s = p.getString("q", "[]");
    p.edit().remove("q").apply();
    return s;
  }
}
