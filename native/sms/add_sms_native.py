#!/usr/bin/env python3
"""
نصب خودکار افزونه‌ی پیامک در پروژه‌ی اندروید (Capacitor).
اجرا (بعد از `npx cap add android` / `npx cap sync android` و قبل از ساخت APK):

    python3 add_sms_native.py <مسیر پوشه android>      # مثلاً: python3 add_sms_native.py android

کارهایی که می‌کند:
  ۱) نام پکیج و MainActivity را پیدا می‌کند
  ۲) TshSmsPlugin.java و SmsReceiver.java را کنار MainActivity می‌گذارد
  ۳) مجوزهای READ_SMS / RECEIVE_SMS و گیرنده‌ی پیامک را به AndroidManifest.xml اضافه می‌کند
  ۴) TshSmsPlugin را در MainActivity ثبت می‌کند
اجرای دوباره آسیبی نمی‌زند (تکراری اضافه نمی‌شود).
"""
import os, re, sys

here = os.path.dirname(os.path.abspath(__file__))
root = sys.argv[1] if len(sys.argv) > 1 else 'android'

def find(pattern_fn):
    for d, _, fs in os.walk(root):
        if '/build/' in d.replace('\\', '/'):
            continue
        for f in fs:
            p = os.path.join(d, f)
            if pattern_fn(p, f):
                return p
    return None

manifest = find(lambda p, f: f == 'AndroidManifest.xml' and '/app/src/main/' in p.replace('\\', '/'))
main = find(lambda p, f: f in ('MainActivity.java', 'MainActivity.kt'))
if not manifest or not main:
    sys.exit('AndroidManifest.xml یا MainActivity پیدا نشد؛ مسیر پوشه‌ی android را بررسی کنید.')

src = open(main, encoding='utf-8').read()
m = re.search(r'^\s*package\s+([\w.]+)', src, re.M)
if not m:
    sys.exit('پکیج MainActivity پیدا نشد.')
pkg = m.group(1)
is_kt = main.endswith('.kt')
outdir = os.path.dirname(main)

# ۲) فایل‌های جاوا
for name in ('TshSmsPlugin.java', 'SmsReceiver.java'):
    code = open(os.path.join(here, name), encoding='utf-8').read().replace('__PKG__', pkg)
    open(os.path.join(outdir, name), 'w', encoding='utf-8').write(code)
    print('نوشته شد:', os.path.join(outdir, name))

# ۳) مانیفست
mf = open(manifest, encoding='utf-8').read()
perms = ''
for p in ('READ_SMS', 'RECEIVE_SMS'):
    if 'android.permission.%s' % p not in mf:
        perms += '    <uses-permission android:name="android.permission.%s" />\n' % p
if perms:
    mf = mf.replace('</manifest>', perms + '</manifest>')
if 'SmsReceiver' not in mf:
    rec = ('        <receiver android:name="%s.SmsReceiver" android:exported="true" '
           'android:permission="android.permission.BROADCAST_SMS">\n'
           '            <intent-filter android:priority="999">\n'
           '                <action android:name="android.provider.Telephony.SMS_RECEIVED" />\n'
           '            </intent-filter>\n'
           '        </receiver>\n') % pkg
    mf = mf.replace('</application>', rec + '    </application>')
open(manifest, 'w', encoding='utf-8').write(mf)
print('مانیفست به‌روز شد:', manifest)

# ۴) ثبت افزونه در MainActivity
if 'TshSmsPlugin' in src:
    print('افزونه از قبل در MainActivity ثبت شده است.')
else:
    reg = 'registerPlugin(TshSmsPlugin.class)' if not is_kt else 'registerPlugin(TshSmsPlugin::class.java)'
    reg += '' if is_kt else ';'
    if re.search(r'registerPlugin\(', src):
        # کنار ثبت‌های قبلی
        src = re.sub(r'([ \t]*)(registerPlugin\([^\n]*\n)', lambda mm: mm.group(1) + reg + '\n' + mm.group(1) + mm.group(2), src, count=1)
    elif re.search(r'super\.onCreate\(', src):
        src = re.sub(r'([ \t]*)(super\.onCreate\([^\n]*\n)', lambda mm: mm.group(1) + reg + '\n' + mm.group(1) + mm.group(2), src, count=1)
    else:
        # MainActivity خالی است: متد onCreate اضافه کن
        if is_kt:
            body = '    override fun onCreate(savedInstanceState: android.os.Bundle?) {\n        %s\n        super.onCreate(savedInstanceState)\n    }\n' % reg
            if '{' in src[src.index('class MainActivity'):]:
                i = src.index('{', src.index('class MainActivity'))
                src = src[:i + 1] + '\n' + body + src[i + 1:]
        else:
            body = '    @Override\n    public void onCreate(android.os.Bundle savedInstanceState) {\n        %s\n        super.onCreate(savedInstanceState);\n    }\n' % reg
            i = src.index('{', src.index('class MainActivity'))
            src = src[:i + 1] + '\n' + body + src[i + 1:]
    open(main, 'w', encoding='utf-8').write(src)
    print('افزونه در MainActivity ثبت شد:', main)
print('تمام. حالا APK را بسازید.')
