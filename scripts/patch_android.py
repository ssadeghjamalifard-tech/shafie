import shutil, pathlib, glob, os, re

pkg = "app/tshia/calendar"
d = pathlib.Path("android/app/src/main/java") / pkg

# --- کد بومی: همه‌ی فایل‌های native/*.java (افزونه‌ی اعلان + ابزارک‌ها) ---
for f in sorted(glob.glob("native/*.java")):
    shutil.copy(f, d / os.path.basename(f))

# --- منابع ابزارک‌ها: چیدمان‌ها، آیکون‌ها، تنظیمات ارائه‌دهنده و رشته‌ها ---
res_src = pathlib.Path("native/res")
if res_src.exists():
    shutil.copytree(res_src, pathlib.Path("android/app/src/main/res"), dirs_exist_ok=True)

has_widgets = pathlib.Path("native/TshWidgetPlugin.java").exists()
plugins = "    registerPlugin(DayNotifPlugin.class);\n"
if has_widgets:
    plugins += "    registerPlugin(TshWidgetPlugin.class);\n"
if pathlib.Path("native/TshSysPlugin.java").exists():
    plugins += "    registerPlugin(TshSysPlugin.class);\n"

(d / "MainActivity.java").write_text("""package app.tshia.calendar;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
  @Override public void onCreate(Bundle b) {
""" + plugins + """    super.onCreate(b);
  }
}
""", encoding="utf-8")

m = pathlib.Path("android/app/src/main/AndroidManifest.xml")
t = m.read_text(encoding="utf-8")
perms = ('<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>\n'
         '    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>\n    ')
# مجوزهای کار در پس‌زمینه: معافیت از بهینه‌سازی باتری و زنگ دقیق
for _p in ("REQUEST_IGNORE_BATTERY_OPTIMIZATIONS", "SCHEDULE_EXACT_ALARM"):
    if _p not in t:
        perms += '<uses-permission android:name="android.permission.%s"/>\n    ' % _p
rcv = ('<receiver android:name=".DayNotifPlugin$Rcv" android:exported="false">'
       '<intent-filter><action android:name="android.intent.action.BOOT_COMPLETED"/>'
       '<action android:name="android.intent.action.MY_PACKAGE_REPLACED"/></intent-filter></receiver>\n    ')

wid = ""
if has_widgets:
    for cls, lab, info in [("PrayerWidget", "tw_name_prayer", "tw_prayer_info"),
                           ("DateWidget", "tw_name_date", "tw_date_info"),
                           ("OccasionWidget", "tw_name_occ", "tw_occ_info"),
                           ("MonthWidget", "tw_name_month", "tw_month_info"),
                           ("RamadanWidget", "tw_name_ram", "tw_ram_info"),
                           ("PageWidget", "tw_name_page", "tw_page_info"),
                           ("LifeWidget", "tw_name_life", "tw_life_info"),
                           ("NextWidget", "tw_name_next", "tw_next_info"),
                           ("PlanWidget", "tw_name_plan", "tw_plan_info")]:
        wid += ('<receiver android:name=".%s" android:exported="true" android:label="@string/%s">'
                '<intent-filter><action android:name="android.appwidget.action.APPWIDGET_UPDATE"/></intent-filter>'
                '<meta-data android:name="android.appwidget.provider" android:resource="@xml/%s"/></receiver>\n    ') % (cls, lab, info)
    wid += ('<receiver android:name=".WTick" android:exported="false"><intent-filter>'
            '<action android:name="android.intent.action.BOOT_COMPLETED"/>'
            '<action android:name="android.intent.action.MY_PACKAGE_REPLACED"/>'
            '<action android:name="android.intent.action.DATE_CHANGED"/>'
            '<action android:name="android.intent.action.TIME_SET"/>'
            '<action android:name="android.intent.action.TIMEZONE_CHANGED"/></intent-filter></receiver>\n    ')
    wid += ('<activity android:name=".WConfig" android:exported="true" '
            'android:theme="@android:style/Theme.Material.Light.Dialog">'
            '<intent-filter><action android:name="android.appwidget.action.APPWIDGET_CONFIGURE"/></intent-filter></activity>\n  ')

assert "<application" in t and "</application>" in t
t = t.replace("<application", perms + "<application", 1).replace("</application>", rcv + wid + "</application>", 1)
# برای چیدمان درست راست‌به‌چپ ابزارک‌ها
if "supportsRtl" not in t:
    t = t.replace("<application", '<application android:supportsRtl="true"', 1)
m.write_text(t, encoding="utf-8")

# --- نام برنامه: «شفیع» (Shafie) ---
sx = pathlib.Path("android/app/src/main/res/values/strings.xml")
if sx.exists():
    st = sx.read_text(encoding="utf-8")
    for k in ("app_name", "title_activity_main"):
        st = re.sub(r'(<string name="%s">)[^<]*(</string>)' % k, r"\1شفیع\2", st)
    sx.write_text(st, encoding="utf-8")
    print("app name -> شفیع")

import os, re
g = pathlib.Path("android/app/build.gradle")
gt = g.read_text(encoding="utf-8")
gt = re.sub(r"versionCode \d+", "versionCode " + os.environ.get("GITHUB_RUN_NUMBER", "1"), gt, 1)
g.write_text(gt, encoding="utf-8")
print("patched", "(+widgets)" if has_widgets else "")
