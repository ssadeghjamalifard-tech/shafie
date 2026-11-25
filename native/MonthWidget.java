package app.tshia.calendar;

import android.content.Context;
import android.view.View;
import android.widget.RemoteViews;
import org.json.JSONArray;
import org.json.JSONObject;

public class MonthWidget extends WBase {
  @Override int layout() { return R.layout.tw_month; }

  static boolean has(JSONArray a, int v) {
    if (a == null) return false;
    for (int i = 0; i < a.length(); i++) if (a.optInt(i) == v) return true;
    return false;
  }

  @Override RemoteViews build(Context c, int id, int sz, boolean dark, JSONObject d, JSONObject cfg) throws Exception {
    if (d == null) return shell(c, layout(), cfg, dark, "برنامه‌ی «تقویم شیعه» را یک بار باز کنید تا ابزارک داده بگیرد");
    long mid = WStore.midnight(System.currentTimeMillis());
    JSONArray ms = d.getJSONArray("months");
    JSONObject m = null;
    int today = 0;
    for (int i = 0; i < ms.length(); i++) {
      JSONObject o = ms.getJSONObject(i);
      long k = Math.round((mid - o.getLong("ed")) / (double) WStore.DAY);
      if (k >= 0 && k < o.getInt("len")) { m = o; today = (int) k + 1; break; }
    }
    if (m == null) return shell(c, layout(), cfg, dark, "داده‌ی ابزارک قدیمی شده؛ برنامه را باز کنید");
    RemoteViews rv = shell(c, layout(), cfg, dark, null);
    int ink = ink(dark), mute = mute(dark), em = em(dark), gold = gold(dark), red = red(dark);
    boolean hij = cfg.optInt("h", 0) == 1;
    txt(rv, R.id.tw_mt, m.optString("n"), ink, sz == 0 ? 12 : 14);
    txt(rv, R.id.tw_ms, m.optString("hs"), mute, 10);
    show(rv, R.id.tw_ms, sz >= 1);
    for (int k = 0; k < 7; k++) rv.setTextColor(rid(c, "tw_w", k), k == 0 ? red : mute);
    int len = m.getInt("len"), f = m.getInt("f");
    int rows = (f + len + 6) / 7;
    for (int r = 0; r < 6; r++) show(rv, rid(c, "tw_r", r), r < rows);
    JSONArray ev = m.optJSONArray("ev"), hl = m.optJSONArray("hl"), hd = m.optJSONArray("hd");
    float ns = sz == 0 ? 9 : sz == 1 ? 11 : 14;
    for (int i = 0; i < 42; i++) {
      int day = i - f + 1;
      int col = i % 7;               // ۰ = شنبه
      int pos = (i / 7) * 7 + (6 - col); // چیدمان چپ‌به‌راست؛ شنبه سمت راست
      int nId = rid(c, "tw_n", pos), sId = rid(c, "tw_s", pos);
      if (day < 1 || day > len) {
        rv.setTextViewText(nId, "");
        rv.setTextViewText(sId, "");
        rv.setInt(nId, "setBackgroundResource", 0);
        continue;
      }
      boolean isT = day == today, holi = has(hl, day) || col == 6, evt = has(ev, day);
      int color = isT ? (dark ? 0xFF06130F : 0xFFFFFFFF) : holi ? red : ink;
      txt(rv, nId, WStore.fa(String.valueOf(day)), color, ns);
      rv.setInt(nId, "setBackgroundResource", isT ? (dark ? R.drawable.tw_today_d : R.drawable.tw_today_l) : 0);
      String sub = hij ? WStore.fa(String.valueOf(hd == null ? 0 : hd.optInt(day - 1))) : (evt ? "•" : "");
      txt(rv, sId, sub, evt ? gold : mute, hij ? (sz == 2 ? 9 : 8) : (sz == 2 ? 11 : 9));
      show(rv, sId, sz >= 1 || evt);
    }
    return rv;
  }
}
