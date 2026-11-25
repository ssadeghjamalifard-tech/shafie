package app.tshia.calendar;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import org.json.JSONObject;

/** پایه‌ی همه‌ی ابزارک‌ها: اندازه، تم، کلیک، به‌روزرسانی */
abstract class WBase extends AppWidgetProvider {
  abstract int layout();
  abstract RemoteViews build(Context c, int id, int sz, boolean dark, JSONObject d, JSONObject cfg) throws Exception;

  static int ink(boolean d) { return d ? 0xFFE3EEE8 : 0xFF17261F; }
  static int mute(boolean d) { return d ? 0xFF8CA196 : 0xFF66786F; }
  static int em(boolean d) { return d ? 0xFF3DB496 : 0xFF0E6B57; }
  static int gold(boolean d) { return d ? 0xFFDDB35A : 0xFFA87A1E; }
  static int red(boolean d) { return d ? 0xFFEE7676 : 0xFFBF3636; }

  static final int[] STYLE = {R.drawable.tw_bg_g1, R.drawable.tw_bg_g2, R.drawable.tw_bg_g3, R.drawable.tw_bg_g4, R.drawable.tw_bg_g5, R.drawable.tw_bg_g6};

  /** سبک گرادیانی (۱ تا ۶) متن روشن دارد؛ سبک ۰ خودکار/روشن/تیره است */
  static boolean dark(Context c, JSONObject cfg) {
    if (cfg.optInt("st", 0) > 0) return true;
    int th = cfg.optInt("th", 0);
    return th == 2 || (th == 0 && isNight(c));
  }

  static boolean isNight(Context c) {
    return (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
  }

  static int rid(Context c, String name, int i) {
    return c.getResources().getIdentifier(name + i, "id", c.getPackageName());
  }

  static PendingIntent open(Context c) {
    Intent i = c.getPackageManager().getLaunchIntentForPackage(c.getPackageName());
    if (i == null) i = new Intent();
    return PendingIntent.getActivity(c, 0, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
  }

  /** قاب مشترک: پس‌زمینه‌ی روشن/تیره با شفافیت، کلیک برای باز کردن برنامه، پیام خطا */
  static RemoteViews shell(Context c, int layout, JSONObject cfg, boolean dark, String msg) {
    RemoteViews rv = new RemoteViews(c.getPackageName(), layout);
    int a = Math.max(20, Math.min(100, cfg.optInt("op", 100))) * 255 / 100;
    int st = cfg.optInt("st", 0);
    boolean grad = st > 0 && st <= STYLE.length;
    rv.setViewVisibility(R.id.tw_bgl, !grad && !dark ? View.VISIBLE : View.GONE);
    rv.setViewVisibility(R.id.tw_bgd, !grad && dark ? View.VISIBLE : View.GONE);
    rv.setViewVisibility(R.id.tw_bgs, grad ? View.VISIBLE : View.GONE);
    if (grad) rv.setImageViewResource(R.id.tw_bgs, STYLE[st - 1]);
    rv.setInt(R.id.tw_bgl, "setImageAlpha", a);
    rv.setInt(R.id.tw_bgd, "setImageAlpha", a);
    rv.setInt(R.id.tw_bgs, "setImageAlpha", a);
    rv.setInt(R.id.tw_deco, "setColorFilter", dark ? 0xFFFFFFFF : 0xFF0E6B57);
    rv.setInt(R.id.tw_deco, "setImageAlpha", dark ? 26 : 22);
    rv.setViewVisibility(R.id.tw_body, msg == null ? View.VISIBLE : View.GONE);
    rv.setViewVisibility(R.id.tw_msg, msg == null ? View.GONE : View.VISIBLE);
    if (msg != null) {
      rv.setTextViewText(R.id.tw_msg, msg);
      rv.setTextColor(R.id.tw_msg, ink(dark));
    }
    rv.setOnClickPendingIntent(R.id.tw_root, open(c));
    return rv;
  }

  static void txt(RemoteViews rv, int id, CharSequence s, int color, float sp) {
    rv.setTextViewText(id, s);
    rv.setTextColor(id, color);
    if (sp > 0) rv.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, sp);
  }

  static void show(RemoteViews rv, int id, boolean on) {
    rv.setViewVisibility(id, on ? View.VISIBLE : View.GONE);
  }

  /** شمارش معکوس زنده (Chronometer) یا متن ساده در اندرویدهای قدیمی */
  static void count(RemoteViews rv, long ms, int color, float sp) {
    if (Build.VERSION.SDK_INT >= 24) {
      show(rv, R.id.tw_chr, true);
      show(rv, R.id.tw_cd, false);
      rv.setChronometerCountDown(R.id.tw_chr, true);
      rv.setChronometer(R.id.tw_chr, SystemClock.elapsedRealtime() + Math.max(0, ms), null, true);
      rv.setTextColor(R.id.tw_chr, color);
      rv.setTextViewTextSize(R.id.tw_chr, TypedValue.COMPLEX_UNIT_SP, sp);
    } else {
      show(rv, R.id.tw_chr, false);
      show(rv, R.id.tw_cd, true);
      txt(rv, R.id.tw_cd, WStore.dur(ms), color, sp * 0.7f);
    }
  }

  static void progress(RemoteViews rv, boolean dark, int p) {
    show(rv, R.id.tw_pbl, !dark);
    show(rv, R.id.tw_pbd, dark);
    rv.setProgressBar(R.id.tw_pbl, 1000, p, false);
    rv.setProgressBar(R.id.tw_pbd, 1000, p, false);
  }

  void push(Context c, AppWidgetManager m, int id) {
    JSONObject cfg = WStore.cfg(c, id);
    boolean dark = dark(c, cfg);
    RemoteViews rv;
    try {
      Bundle o = m.getAppWidgetOptions(id);
      int w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250);
      int h = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110);
      int sz = w >= 180 ? (h >= 180 ? 2 : 1) : 0;
      rv = build(c, id, sz, dark, WStore.data(c), cfg);
    } catch (Throwable e) {
      rv = shell(c, layout(), cfg, dark, "نمایش ابزارک ممکن نشد؛ برنامه را باز کنید");
    }
    m.updateAppWidget(id, rv);
  }

  @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
    for (int id : ids) push(c, m, id);
    WTick.schedule(c);
  }

  @Override public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle o) {
    push(c, m, id);
  }

  @Override public void onDeleted(Context c, int[] ids) {
    for (int id : ids) WStore.delCfg(c, id);
  }

  @Override public void onEnabled(Context c) { WTick.schedule(c); }
  @Override public void onDisabled(Context c) { WTick.schedule(c); }

  static WBase[] all() {
    return new WBase[] {new PrayerWidget(), new DateWidget(), new OccasionWidget(), new MonthWidget(), new RamadanWidget(), new PageWidget(), new LifeWidget(), new NextWidget(), new PlanWidget()};
  }

  static void updateAll(Context c) {
    AppWidgetManager m = AppWidgetManager.getInstance(c);
    for (WBase p : all()) {
      int[] ids = m.getAppWidgetIds(new ComponentName(c, p.getClass()));
      for (int id : ids) p.push(c, m, id);
    }
  }

  static int count(Context c) {
    AppWidgetManager m = AppWidgetManager.getInstance(c);
    int n = 0;
    for (WBase p : all()) n += m.getAppWidgetIds(new ComponentName(c, p.getClass())).length;
    return n;
  }

  /** نوع ابزارک بر اساس نام کلاس: ۰ اوقات، ۱ تاریخ، ۲ مناسبت، ۳ ماه، ۴ رمضان */
  static int kindOf(Context c, int id) {
    try {
      String n = AppWidgetManager.getInstance(c).getAppWidgetInfo(id).provider.getClassName();
      WBase[] a = all();
      for (int i = 0; i < a.length; i++) if (n.endsWith(a[i].getClass().getSimpleName())) return i;
    } catch (Exception e) { }
    return 0;
  }
}
