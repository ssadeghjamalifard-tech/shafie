تغییر نام به «شفیع»، آیکون جدید و اعلان براق

محتوای این zip را روی ریشه‌ی مخزن GitHub جایگزین کنید (همان‌جا که index.html و پوشه‌های native و scripts هست)، سپس Actions ← Build APK.
فایل‌های دیگر (workflow، package.json، ابزارک‌ها) را دست نزنید.

داخل zip:
- index.html: نام «شفیع» در عنوان و سربرگ + دانلود خودکار کتاب‌ها از zip
- scripts/patch_android.py: نام برنامه روی گوشی را «شفیع» می‌کند
- native/DayNotifPlugin.java: اعلان مینیمال و براق
- native/res/layout/tw_notif*.xml و drawable/tw_notif_bg.xml: ظاهر اعلان
- native/res/mipmap-*: آیکون برنامه (قدیمی، گرد و تطبیقی) و values/shafie_colors.xml
