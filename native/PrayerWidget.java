package app.tshia.calendar;

import android.content.Context;
import android.widget.RemoteViews;
import java.util.Calendar;
import org.json.JSONArray;
import org.json.JSONObject;

public class PrayerWidget extends WBase {
  static final String[] JM = {"فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"};
  // Calendar.DAY_OF_WEEK: 1 = یکشنبه ... 7 = شنبه
  static final String[] WD = {"یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه", "شنبه"};

  @Override int layout() { return R.layout.tw_prayer; }

  static String fa(String s) {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      char ch = s.charAt(i);
      b.append(ch >= '0' && ch <= '9' ? (char) ('\u06F0' + (ch - '0')) : ch);
    }
    return b.toString();
  }

  static int[] toJalali(int gy, int gm, int gd) {
    int[] gdm = {0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334};
    int gy2 = gm > 2 ? gy + 1 : gy;
    int days = 355666 + 365 * gy + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400 + gd + gdm[gm - 1];
    int jy = -1595 + 33 * (days / 12053);
    days %= 12053;
    jy += 4 * (days / 1461);
    days %= 1461;
    if (days > 365) { jy += (days - 1) / 365; days = (days - 1) % 365; }
    int jm = days < 186 ? 1 + days / 31 : 7 + (days - 186) / 30;
    int jd = 1 + (days < 186 ? days % 31 : (days - 186) % 30);
    return new int[] {jy, jm, jd};
  }

  static String jdate(long ms) {
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(ms);
    int[] j = toJalali(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    return WD[cal.get(Calendar.DAY_OF_WEEK) - 1] + "، " + fa(String.valueOf(j[2])) + " " + JM[j[1] - 1] + " " + fa(String.valueOf(j[0]));
  }

  @Override RemoteViews build(Context c, int id, int sz, boolean dark, JSONObject d, JSONObject cfg) throws Exception {
    // اگر کاربر شفافیت را تنظیم نکرده باشد، پیش‌فرض نیمه‌شفاف است
    JSONObject k = new JSONObject(cfg.toString());
    if (!k.has("op")) k.put("op", 70);
    cfg = k;

    if (d == null) return shell(c, layout(), cfg, dark, "برنامه‌ی «تقویم شیعه» را یک بار باز کنید تا ابزارک داده بگیرد");
    long now = System.currentTimeMillis(), mid = WStore.midnight(now);
    int di = WStore.dayIdx(d.getJSONArray("days"), mid);
    int ci = WStore.city(d, cfg);
    JSONArray pts = d.getJSONArray("pt").getJSONArray(ci);
    long[] nx = di < 0 ? null : WStore.nextAdhan(pts, now);
    if (nx == null) return shell(c, layout(), cfg, dark, "داده‌ی ابزارک قدیمی شده؛ برنامه را باز کنید");
    RemoteViews rv = shell(c, layout(), cfg, dark, null);
    boolean wide = sz >= 1;
    int ink = ink(dark), mute = mute(dark), gold = gold(dark);
    int hi = dark ? 0xFF6FEBD8 : em(false);
    show(rv, R.id.tw_deco, false);

    // رنگ و قاب‌ها بر اساس تم (روشن/تیره/گرادیانی) برای خوانایی روی هر پس‌زمینه
    rv.setInt(R.id.tw_card, "setBackgroundResource", dark ? R.drawable.tw_card_d : R.drawable.tw_card_l);
    rv.setInt(R.id.tw_left, "setBackgroundResource", dark ? R.drawable.tw_pane_d : R.drawable.tw_pane_l);

    // کارت «اذان بعدی»
    txt(rv, R.id.tw_label, "اذان بعدی: " + WStore.PN[(int) nx[2]], gold, sz == 0 ? 10 : 11);
    txt(rv, R.id.tw_at, WStore.hm(nx[1]), ink, sz == 0 ? 20 : 22);
    progress(rv, dark, (int) Math.max(0, Math.min(1000, (now - nx[0]) * 1000 / Math.max(1, nx[1] - nx[0]))));

    // ستون چپ: تاریخ، شهر، ساعت بزرگ، شمارش معکوس، نام برنامه
    txt(rv, R.id.tw_city, WStore.cityName(d, ci), mute, 9);
    txt(rv, R.id.tw_dt, jdate(now), gold, 12);
    rv.setTextColor(R.id.tw_clk, ink);
    rv.setTextColor(R.id.tw_cdl, mute);
    txt(rv, R.id.tw_brand, "شفیع", gold, 18);
    count(rv, nx[1] - now, hi, sz == 0 ? 14 : 15);
    show(rv, R.id.tw_dt, wide);
    show(rv, R.id.tw_clk, wide);
    show(rv, R.id.tw_brand, wide);

    // شش وقت در سه ردیف دوتایی؛ وقت بعدی با قاب فیروزه‌ای
    show(rv, R.id.tw_row, wide);
    if (wide) {
      JSONArray tt = pts.getJSONArray(di);
      for (int i = 0; i < 6; i++) {
        boolean nextOne = tt.getLong(i) == nx[1];
        txt(rv, rid(c, "tw_pn", i), WStore.PS[i] + ":", nextOne ? hi : gold, sz == 2 ? 12 : 11);
        txt(rv, rid(c, "tw_pt", i), WStore.hm(tt.getLong(i)), nextOne ? hi : ink, sz == 2 ? 13 : 12);
        int bg = nextOne ? (dark ? R.drawable.tw_cell_on_d : R.drawable.tw_cell_on_l) : (dark ? R.drawable.tw_cell_d : R.drawable.tw_cell_l);
        rv.setInt(rid(c, "tw_pc", i), "setBackgroundResource", bg);
      }
    }
    return rv;
  }
}
