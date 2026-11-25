package app.tshia.calendar;

import android.content.Context;
import android.widget.RemoteViews;
import org.json.JSONObject;

/** روز زندگی من: روزهای گذشته از تولد، سن دقیق شمسی، روزشمار تا تولد بعدی */
public class LifeWidget extends WBase {
  @Override int layout() { return R.layout.tw_life; }

  @Override RemoteViews build(Context c, int id, int sz, boolean dark, JSONObject d, JSONObject cfg) throws Exception {
    if (d == null) return shell(c, layout(), cfg, dark, "برنامه‌ی «تقویم شیعه» را یک بار باز کنید تا ابزارک داده بگیرد");
    int di = WStore.dayIdx(d.getJSONArray("days"), WStore.midnight(System.currentTimeMillis()));
    if (di < 0) return shell(c, layout(), cfg, dark, "داده‌ی ابزارک قدیمی شده؛ برنامه را باز کنید");
    JSONObject x = d.getJSONArray("days").getJSONObject(di);
    if (!x.has("ld")) return shell(c, layout(), cfg, dark, "تاریخ تولد را در برنامه ثبت کنید: منو ← تنظیمات ← «روز زندگی»");
    RemoteViews rv = shell(c, layout(), cfg, dark, null);
    int ink = ink(dark), mute = mute(dark), gold = dark ? 0xFFF2D58A : gold(false);
    txt(rv, R.id.tw_lf_h, "امروز روزِ", mute, sz == 0 ? 11 : 12);
    txt(rv, R.id.tw_lf_n, WStore.num(x.optLong("ld")), gold, sz == 0 ? 30 : sz == 1 ? 42 : 56);
    txt(rv, R.id.tw_lf_u, "از زندگی شماست", ink, sz == 0 ? 12 : 14);
    progress(rv, dark, x.optInt("ap"));
    txt(rv, R.id.tw_lf_a, x.optString("ag"), ink, sz == 0 ? 11 : 13);
    int nb = x.optInt("nb");
    txt(rv, R.id.tw_lf_b, nb == 0 ? "امروز تولد شماست؛ تولدتان مبارک" : WStore.fa(String.valueOf(nb)) + " روز تا تولد بعدی", mute, sz == 0 ? 10.5f : 12);
    show(rv, R.id.tw_lf_a, sz >= 1);
    return rv;
  }
}
