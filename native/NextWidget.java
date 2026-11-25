package app.tshia.calendar;

import android.content.Context;
import android.widget.RemoteViews;
import org.json.JSONArray;
import org.json.JSONObject;

/** مناسبت بعدی و تعطیلی بعدی با روزشمار */
public class NextWidget extends WBase {
  @Override int layout() { return R.layout.tw_next; }

  static String rel(long k) { return k <= 0 ? "امروز" : k == 1 ? "فردا" : WStore.fa(String.valueOf(k)) + " روز دیگر"; }

  @Override RemoteViews build(Context c, int id, int sz, boolean dark, JSONObject d, JSONObject cfg) throws Exception {
    if (d == null) return shell(c, layout(), cfg, dark, "برنامه‌ی «تقویم شیعه» را یک بار باز کنید تا ابزارک داده بگیرد");
    long mid = WStore.midnight(System.currentTimeMillis());
    JSONArray up = d.optJSONArray("up");
    JSONObject a = null, h = null;
    if (up != null) for (int i = 0; i < up.length(); i++) {
      JSONObject o = up.optJSONObject(i);
      if (o == null || o.optLong("ed") < mid - 3600000L) continue;
      if (a == null) a = o;
      if (h == null && o.optInt("h") == 1) h = o;
    }
    if (a == null) return shell(c, layout(), cfg, dark, "داده‌ی ابزارک قدیمی شده؛ برنامه را باز کنید");
    RemoteViews rv = shell(c, layout(), cfg, dark, null);
    int ink = ink(dark), mute = mute(dark), gold = dark ? 0xFFF2D58A : gold(false), red = dark ? 0xFFFFA0A0 : red(false);
    long k = Math.round((a.optLong("ed") - mid) / (double) WStore.DAY);
    txt(rv, R.id.tw_nx_h, "مناسبت بعدی", mute, sz == 0 ? 11 : 12);
    txt(rv, R.id.tw_nx_t, a.optString("t"), ink, sz == 0 ? 14 : 17);
    rv.setInt(R.id.tw_nx_t, "setMaxLines", sz == 0 ? 3 : 2);
    txt(rv, R.id.tw_nx_d, a.optString("p"), mute, 12);
    txt(rv, R.id.tw_nx_c, rel(k), gold, sz == 0 ? 24 : sz == 1 ? 32 : 40);
    boolean two = sz >= 1 && h != null && h != a;
    show(rv, R.id.tw_nx_h2, two);
    show(rv, R.id.tw_nx_t2, two);
    if (two) {
      long k2 = Math.round((h.optLong("ed") - mid) / (double) WStore.DAY);
      txt(rv, R.id.tw_nx_h2, "تعطیلی بعدی · " + rel(k2), mute, 11);
      txt(rv, R.id.tw_nx_t2, h.optString("t"), red, 13);
    }
    return rv;
  }
}
