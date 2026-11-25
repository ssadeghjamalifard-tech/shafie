مرحله‌ی ۶ — ابزارک مدیریت برنامه‌ریزی + اتصال دوباره‌ی ابزارک‌ها به برنامه

محتوای این zip را روی ریشه‌ی مخزن جایگزین کنید (index.html، پوشه‌ی native و scripts).
فایل‌های native/DayNotifPlugin.java و native/TshSysPlugin.java و package.json و workflow را دست نزنید.
بعد Actions ← Build APK.

چه چیزی عوض شد:
- index.html: پل ابزارک‌ها (TshWidget) به نسخه‌ی فعلی برگشت؛ تا قبل از این هیچ داده‌ای به ابزارک‌ها نمی‌رسید.
- کارت «اجرا در پس‌زمینه» (اعلان، باتری، زنگ دقیق) در تنظیمات.
- برنامه‌ریزی روزانه دیگر روز انتخاب‌شده‌ی تقویم را عوض نمی‌کند و بعد از هر تغییر ابزارک را به‌روز می‌کند.
- native/PlanWidget.java و منابع tw_plan*: ابزارک «مدیریت برنامه‌ریزی».
- WBase.java، TshWidgetPlugin.java، tw_strings.xml و patch_android.py برای ثبت ابزارک جدید.
