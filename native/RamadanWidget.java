package app.tshia.calendar;

import android.content.Context;
import android.widget.RemoteViews;
import org.json.JSONArray;
import org.json.JSONObject;

public class RamadanWidget extends WBase {
  @Override int layout() { return R.layout.tw_ram; }

  @Override RemoteViews build(Context c, int id, int sz, boolean dark, JSONObject d, JSONObject cfg) throws Exception {
    if (d == null) return shell(c, layout(), cfg, dark, "برنامه‌ی «تقویم شیعه» را یک بار باز کنید تا ابزارک داده بگیرد");
    long now = System.currentTimeMillis(), mid = WStore.midnight(now);
    JSONArray days = d.getJSONArray("days");
    int di = WStore.dayIdx(days, mid);
    if (di < 0) return shell(c, layout(), cfg, dark, "داده‌ی ابزارک قدیمی شده؛ برنامه را باز کنید");
    JSONObject x = days.getJSONObject(di);
    RemoteViews rv = shell(c, layout(), cfg, dark, null);
    int ink = ink(dark), mute = mute(dark), em = em(dark);
    float big = sz == 0 ? 24 : sz == 1 ? 30 : 40;
    if (x.optInt("hm") != 9) {
      // خارج از ماه رمضان: روزشمار تا آغاز ماه
      JSONObject r = d.optJSONObject("ram");
      txt(rv, R.id.tw_city, "ماه رمضان", mute, 12);
      txt(rv, R.id.tw_label, "تا آغاز ماه رمضان", ink, sz == 0 ? 13 : 15);
      show(rv, R.id.tw_chr, false);
      show(rv, R.id.tw_cd, true);
      long n = r == null ? 0 : Math.round((r.optLong("ed") - mid) / (double) WStore.DAY);
      txt(rv, R.id.tw_cd, WStore.fa(String.valueOf(Math.max(0, n))) + " روز", em, big);
      show(rv, R.id.tw_pbw, false);
      show(rv, R.id.tw_row, false);
      return rv;
    }
    int ci = WStore.city(d, cfg);
    JSONArray pts = d.getJSONArray("pt").getJSONArray(ci);
    JSONArray tt = pts.getJSONArray(di);
    long fajr = tt.getLong(0), mg = tt.getLong(4), target, start;
    String lab;
    if (now < fajr) {
      target = fajr; lab = "مانده تا سحر";
      start = di > 0 ? pts.getJSONArray(di - 1).getLong(4) : fajr - 8 * 3600000L;
    } else if (now < mg) {
      target = mg; lab = "مانده تا افطار"; start = fajr;
    } else if (di + 1 < days.length() && days.getJSONObject(di + 1).optInt("hm") == 9) {
      target = pts.getJSONArray(di + 1).getLong(0); lab = "مانده تا سحر"; start = mg;
    } else {
      txt(rv, R.id.tw_city, "ماه رمضان", mute, 12);
      txt(rv, R.id.tw_label, "ماه رمضان به پایان رسید؛ عید فطر مبارک", ink, 15);
      show(rv, R.id.tw_chr, false);
      show(rv, R.id.tw_cd, false);
      show(rv, R.id.tw_pbw, false);
      show(rv, R.id.tw_row, false);
      return rv;
    }
    txt(rv, R.id.tw_city, "رمضان · روز " + WStore.fa(String.valueOf(x.optInt("hd"))) + " · " + WStore.cityName(d, ci), mute, sz == 0 ? 11 : 12);
    txt(rv, R.id.tw_label, lab, ink, sz == 0 ? 13 : 15);
    count(rv, target - now, em, big);
    show(rv, R.id.tw_pbw, true);
    progress(rv, dark, (int) Math.max(0, Math.min(1000, (now - start) * 1000 / Math.max(1, target - start))));
    show(rv, R.id.tw_row, sz >= 1);
    txt(rv, R.id.tw_pn0, "سحر (اذان صبح)", mute, 10);
    txt(rv, R.id.tw_pt0, WStore.hm(fajr), ink, 13);
    txt(rv, R.id.tw_pn1, "افطار (اذان مغرب)", mute, 10);
    txt(rv, R.id.tw_pt1, WStore.hm(mg), ink, 13);
    return rv;
  }
}
