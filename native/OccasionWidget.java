package app.tshia.calendar;

import android.content.Context;
import android.view.View;
import android.widget.RemoteViews;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

public class OccasionWidget extends WBase {
  @Override int layout() { return R.layout.tw_occ; }

  @Override RemoteViews build(Context c, int id, int sz, boolean dark, JSONObject d, JSONObject cfg) throws Exception {
    if (d == null) return shell(c, layout(), cfg, dark, "برنامه‌ی «تقویم شیعه» را یک بار باز کنید تا ابزارک داده بگیرد");
    long mid = WStore.midnight(System.currentTimeMillis());
    JSONArray days = d.getJSONArray("days");
    int di = WStore.dayIdx(days, mid);
    if (di < 0) return shell(c, layout(), cfg, dark, "داده‌ی ابزارک قدیمی شده؛ برنامه را باز کنید");
    JSONObject x = days.getJSONObject(di);
    RemoteViews rv = shell(c, layout(), cfg, dark, null);
    int ink = ink(dark), mute = mute(dark);
    ArrayList<String> lines = new ArrayList<String>();
    String hol = x.optString("hol");
    if (hol.length() > 0) lines.add(hol);
    JSONArray ev = x.optJSONArray("ev");
    if (ev != null) for (int i = 0; i < ev.length(); i++) lines.add(ev.getString(i));
    int maxLines = sz == 0 ? 2 : sz == 1 ? 2 : 3;
    txt(rv, R.id.tw_hd, "مناسبت امروز · " + x.optString("wd") + " " + x.optString("p"), mute, sz == 0 ? 11 : 12);
    int[] ids = {R.id.tw_e0, R.id.tw_e1, R.id.tw_e2};
    for (int i = 0; i < 3; i++) {
      boolean on = i < maxLines && i < lines.size();
      show(rv, ids[i], on || (i == 0 && lines.isEmpty()));
      if (on) txt(rv, ids[i], (i == 0 && hol.length() > 0 ? "" : "• ") + lines.get(i), (i == 0 && hol.length() > 0) ? red(dark) : ink, sz == 0 ? 13 : 14);
      if (i == 0 && lines.isEmpty()) txt(rv, ids[0], "مناسبت ثبت‌شده‌ای برای امروز نیست", mute, 13);
    }
    JSONArray qs = d.optJSONArray("q");
    boolean wantQ = sz >= 1 && cfg.optInt("q", 1) == 1 && qs != null && qs.length() > 0;
    show(rv, R.id.tw_q, wantQ);
    if (wantQ) {
      int k = (int) ((mid / WStore.DAY + id) % qs.length());
      txt(rv, R.id.tw_q, "«" + qs.getString(k) + "»", mute, sz == 2 ? 13 : 12);
      rv.setInt(R.id.tw_q, "setMaxLines", sz == 2 ? 6 : 2);
    }
    return rv;
  }
}
