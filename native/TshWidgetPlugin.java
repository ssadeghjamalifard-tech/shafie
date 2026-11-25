package app.tshia.calendar;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.os.Build;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/** پل برنامه ↔ ابزارک‌ها: دریافت داده، شمارش ابزارک‌های فعال، افزودن ابزارک به صفحه‌ی اصلی */
@CapacitorPlugin(name = "TshWidget")
public class TshWidgetPlugin extends Plugin {
  @PluginMethod
  public void setData(PluginCall call) {
    String s = call.getString("json");
    if (s == null || s.length() < 2) { call.reject("no data"); return; }
    WStore.saveData(getContext(), s);
    try { WBase.updateAll(getContext()); WTick.schedule(getContext()); } catch (Throwable e) { }
    call.resolve();
  }

  @PluginMethod
  public void info(PluginCall call) {
    JSObject r = new JSObject();
    r.put("count", WBase.count(getContext()));
    r.put("pin", Build.VERSION.SDK_INT >= 26 && AppWidgetManager.getInstance(getContext()).isRequestPinAppWidgetSupported());
    call.resolve(r);
  }

  /** تیک‌های زده‌شده روی ابزارک مدیریت برنامه‌ریزی که هنوز به برنامه نرسیده‌اند */
  @PluginMethod
  public void takeEdits(PluginCall call) {
    JSObject r = new JSObject();
    try { r.put("items", new JSArray(PlanWidget.takeQueue(getContext()))); }
    catch (Throwable e) { r.put("items", new JSArray()); }
    call.resolve(r);
  }

  @PluginMethod
  public void pin(PluginCall call) {
    int k = call.getInt("kind", 0);
    WBase[] a = WBase.all();
    JSObject r = new JSObject();
    boolean ok = false;
    try {
      AppWidgetManager m = AppWidgetManager.getInstance(getContext());
      if (Build.VERSION.SDK_INT >= 26 && m.isRequestPinAppWidgetSupported() && k >= 0 && k < a.length)
        ok = m.requestPinAppWidget(new ComponentName(getContext(), a[k].getClass()), null, null);
    } catch (Throwable e) { ok = false; }
    r.put("ok", ok);
    call.resolve(r);
  }
}
