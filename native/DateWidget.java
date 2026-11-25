package app.tshia.calendar;

import android.content.Context;
import android.widget.RemoteViews;
import org.json.JSONObject;

public class DateWidget extends WBase {
  @Override int layout() { return R.layout.tw_date; }

  @Override RemoteViews build(Context c, int id, int sz, boolean dark, JSONObject d, JSONObject cfg) throws Exception {
    if (d == null) return shell(c, layout(), cfg, dark, "برنامه‌ی «تقویم شیعه» را یک بار باز کنید تا ابزارک داده بگیرد");
    long mid = WStore.midnight(System.currentTimeMillis());
    int di = WStore.dayIdx(d.getJSONArray("days"), mid);
    if (di < 0) return shell(c, layout(), cfg, dark, "داده‌ی ابزارک قدیمی شده؛ برنامه را باز کنید");
    JSONObject x = d.getJSONArray("days").getJSONObject(di);
    RemoteViews rv = shell(c, layout(), cfg, dark, null);
    int ink = ink(dark), mute = mute(dark), em = em(dark);
    txt(rv, R.id.tw_wd, x.optString("wd"), mute, sz == 0 ? 12 : 14);
    txt(rv, R.id.tw_d1, x.optString("p"), ink, sz == 0 ? 17 : sz == 1 ? 26 : 32);
    txt(rv, R.id.tw_d2, x.optString("h"), em, sz == 0 ? 12 : 14);
    txt(rv, R.id.tw_d3, x.optString("g"), mute, 13);
    show(rv, R.id.tw_d3, sz >= 1);
    String hol = x.optString("hol");
    show(rv, R.id.tw_hol, sz >= 1 && hol.length() > 0);
    txt(rv, R.id.tw_hol, hol + " · تعطیل", red(dark), 12);
    rv.setTextColor(R.id.tw_clk, ink);
    rv.setTextViewTextSize(R.id.tw_clk, android.util.TypedValue.COMPLEX_UNIT_SP, sz == 0 ? 26 : sz == 1 ? 34 : 46);
    return rv;
  }
}
