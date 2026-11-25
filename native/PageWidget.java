package app.tshia.calendar;

import android.content.Context;
import android.widget.RemoteViews;
import org.json.JSONArray;
import org.json.JSONObject;

/** روزی یک صفحه: قرآن (صفحه‌ی مصحف)، کتاب یا دعا */
public class PageWidget extends WBase {
  @Override int layout() { return R.layout.tw_page; }

  @Override RemoteViews build(Context c, int id, int sz, boolean dark, JSONObject d, JSONObject cfg) throws Exception {
    if (d == null) return shell(c, layout(), cfg, dark, "برنامه‌ی «تقویم شیعه» را یک بار باز کنید تا ابزارک داده بگیرد");
    int di = WStore.dayIdx(d.getJSONArray("days"), WStore.midnight(System.currentTimeMillis()));
    JSONObject pg = d.optJSONObject("pg");
    String[] keys = {"q", "b", "d"};
    JSONArray arr = pg == null ? null : pg.optJSONArray(keys[Math.max(0, Math.min(2, cfg.optInt("src", 0)))]);
    JSONObject it = (di < 0 || arr == null) ? null : arr.optJSONObject(di);
    if (it == null) return shell(c, layout(), cfg, dark, "برای این منبع محتوایی نیست؛ در تنظیمات برنامه «روزی یک صفحه» را بررسی کنید");
    RemoteViews rv = shell(c, layout(), cfg, dark, null);
    int ink = ink(dark), mute = mute(dark), em = dark ? 0xFFF2D58A : em(false);
    txt(rv, R.id.tw_pg_t, it.optString("t"), em, sz == 0 ? 11 : 12);
    boolean quran = cfg.optInt("src", 0) == 0;
    txt(rv, R.id.tw_pg_a, it.optString("a"), ink, quran ? (sz == 0 ? 14 : sz == 1 ? 17 : 19) : (sz == 0 ? 13 : 14));
    rv.setInt(R.id.tw_pg_a, "setMaxLines", sz == 0 ? 5 : sz == 1 ? 4 : 11);
    String f = it.optString("f");
    show(rv, R.id.tw_pg_f, sz >= 2 && f.length() > 0);
    txt(rv, R.id.tw_pg_f, f, mute, 12.5f);
    String i = it.optString("i");
    show(rv, R.id.tw_pg_i, sz >= 1 && i.length() > 0);
    txt(rv, R.id.tw_pg_i, i, mute, 11);
    return rv;
  }
}
