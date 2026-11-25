package app.tshia.calendar;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

/** صفحه‌ی تنظیمات ابزارک: تم، شفافیت، شهر (اوقات و رمضان)، گزیده (مناسبت)، قمری (ماه) */
public class WConfig extends Activity {
  int wid;
  JSONObject cfg;
  RadioGroup rgTheme, rgOp, rgStyle, rgSrc;
  Spinner spCity;
  CheckBox cbQ, cbH;

  int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

  TextView head(String s) {
    TextView t = new TextView(this);
    t.setText(s);
    t.setTextSize(15);
    t.setTextColor(Color.parseColor("#0E6B57"));
    t.setPadding(0, dp(14), 0, dp(4));
    t.setGravity(Gravity.RIGHT);
    return t;
  }

  RadioGroup radios(String[] labels, int checked) {
    RadioGroup g = new RadioGroup(this);
    g.setOrientation(LinearLayout.VERTICAL);
    for (int i = 0; i < labels.length; i++) {
      RadioButton b = new RadioButton(this);
      b.setText(labels[i]);
      b.setId(1000 + i);
      b.setTextSize(15);
      g.addView(b);
    }
    g.check(1000 + checked);
    return g;
  }

  @Override protected void onCreate(Bundle b) {
    super.onCreate(b);
    setResult(RESULT_CANCELED);
    Intent in = getIntent();
    wid = in.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
    if (wid == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return; }
    cfg = WStore.cfg(this, wid);
    int kind = WBase.kindOf(this, wid);
    JSONObject d = WStore.data(this);

    ScrollView sv = new ScrollView(this);
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    root.setPadding(dp(20), dp(12), dp(20), dp(16));
    sv.addView(root);

    TextView title = new TextView(this);
    title.setText("تنظیمات ابزارک");
    title.setTextSize(19);
    title.setGravity(Gravity.RIGHT);
    root.addView(title);

    root.addView(head("سبک ظاهری"));
    rgStyle = radios(new String[] {"ساده (روشن یا تیره)", "زمردی", "شب", "طلایی", "اقیانوس", "بنفشه", "انار"}, Math.max(0, Math.min(6, cfg.optInt("st", 0))));
    root.addView(rgStyle);
    if (kind == 5) {
      root.addView(head("منبع صفحه‌ی روزانه"));
      rgSrc = radios(new String[] {"قرآن (صفحه‌ی مصحف)", "کتاب", "دعا"}, Math.max(0, Math.min(2, cfg.optInt("src", 0))));
      root.addView(rgSrc);
    }
    root.addView(head("پوسته‌ی سبک «ساده»"));
    rgTheme = radios(new String[] {"خودکار (هماهنگ با گوشی)", "روشن", "تیره"}, Math.max(0, Math.min(2, cfg.optInt("th", 0))));
    root.addView(rgTheme);

    root.addView(head("شفافیت پس‌زمینه"));
    int[] ops = {100, 85, 70, 50};
    int oc = 0;
    for (int i = 0; i < ops.length; i++) if (cfg.optInt("op", 100) <= ops[i]) oc = i;
    rgOp = radios(new String[] {"۱۰۰٪", "۸۵٪", "۷۰٪", "۵۰٪"}, oc);
    root.addView(rgOp);

    if (kind == 0 || kind == 4) {
      root.addView(head("شهر"));
      ArrayList<String> names = new ArrayList<String>();
      names.add("همان شهر برنامه");
      try {
        JSONArray cn = d == null ? null : d.optJSONArray("cn");
        if (cn != null) for (int i = 0; i < cn.length(); i++) names.add(cn.getString(i));
      } catch (Exception e) { }
      spCity = new Spinner(this);
      spCity.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, names));
      int cs = cfg.optInt("city", -1) + 1;
      spCity.setSelection(cs >= 0 && cs < names.size() ? cs : 0);
      root.addView(spCity);
    }
    if (kind == 2) {
      cbQ = new CheckBox(this);
      cbQ.setText("نمایش گزیده‌ای از کتاب نصایح");
      cbQ.setChecked(cfg.optInt("q", 1) == 1);
      root.addView(cbQ);
    }
    if (kind == 3) {
      cbH = new CheckBox(this);
      cbH.setText("نمایش روز قمری زیر هر روز");
      cbH.setChecked(cfg.optInt("h", 0) == 1);
      root.addView(cbH);
    }

    Button ok = new Button(this);
    ok.setText("ذخیره");
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    lp.topMargin = dp(18);
    ok.setLayoutParams(lp);
    ok.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) { save(); }
    });
    root.addView(ok);
    setContentView(sv);
  }

  void save() {
    try {
      int[] ops = {100, 85, 70, 50};
      cfg.put("th", rgTheme.getCheckedRadioButtonId() - 1000);
      cfg.put("st", Math.max(0, Math.min(6, rgStyle.getCheckedRadioButtonId() - 1000)));
      if (rgSrc != null) cfg.put("src", Math.max(0, Math.min(2, rgSrc.getCheckedRadioButtonId() - 1000)));
      cfg.put("op", ops[Math.max(0, Math.min(3, rgOp.getCheckedRadioButtonId() - 1000))]);
      if (spCity != null) cfg.put("city", spCity.getSelectedItemPosition() - 1);
      if (cbQ != null) cfg.put("q", cbQ.isChecked() ? 1 : 0);
      if (cbH != null) cfg.put("h", cbH.isChecked() ? 1 : 0);
    } catch (Exception e) { }
    WStore.saveCfg(this, wid, cfg);
    try { WBase.updateAll(this); WTick.schedule(this); } catch (Throwable e) { }
    Intent r = new Intent();
    r.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, wid);
    setResult(RESULT_OK, r);
    finish();
  }
}
