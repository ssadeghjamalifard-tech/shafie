package app.tshia.calendar;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/** وضعیت و درخواست مجوزهای لازم برای کار در پس‌زمینه: اعلان، معافیت از بهینه‌سازی باتری، زنگ دقیق */
@CapacitorPlugin(name = "TshSys")
public class TshSysPlugin extends Plugin {

  @PluginMethod
  public void status(PluginCall call) {
    Context c = getContext();
    JSObject r = new JSObject();
    boolean battery = true, exact = true, notif = true;
    try {
      if (Build.VERSION.SDK_INT >= 23) {
        PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
        battery = pm != null && pm.isIgnoringBatteryOptimizations(c.getPackageName());
      }
      if (Build.VERSION.SDK_INT >= 31) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        exact = am != null && am.canScheduleExactAlarms();
      }
      if (Build.VERSION.SDK_INT >= 24) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        notif = nm == null || nm.areNotificationsEnabled();
      }
    } catch (Throwable e) { }
    r.put("battery", battery);
    r.put("exact", exact);
    r.put("notif", notif);
    r.put("sdk", Build.VERSION.SDK_INT);
    r.put("maker", Build.MANUFACTURER == null ? "" : Build.MANUFACTURER);
    call.resolve(r);
  }

  private void go(PluginCall call, Intent main, Intent fallback) {
    try {
      main.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      getContext().startActivity(main);
      call.resolve();
    } catch (Throwable e) {
      try {
        if (fallback != null) {
          fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          getContext().startActivity(fallback);
          call.resolve();
          return;
        }
      } catch (Throwable x) { }
      call.reject("failed");
    }
  }

  private Intent details() {
    return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getContext().getPackageName()));
  }

  @PluginMethod
  public void battery(PluginCall call) {
    Uri u = Uri.parse("package:" + getContext().getPackageName());
    go(call, new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, u), new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
  }

  @PluginMethod
  public void exact(PluginCall call) {
    if (Build.VERSION.SDK_INT >= 31) {
      Uri u = Uri.parse("package:" + getContext().getPackageName());
      go(call, new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, u), details());
    } else {
      call.resolve();
    }
  }

  @PluginMethod
  public void notif(PluginCall call) {
    try {
      if (Build.VERSION.SDK_INT >= 33 && getActivity() != null) {
        getActivity().requestPermissions(new String[] {"android.permission.POST_NOTIFICATIONS"}, 4411);
        call.resolve();
        return;
      }
    } catch (Throwable e) { }
    Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
    i.putExtra(Settings.EXTRA_APP_PACKAGE, getContext().getPackageName());
    go(call, i, details());
  }

  @PluginMethod
  public void appSettings(PluginCall call) {
    go(call, details(), null);
  }
}
