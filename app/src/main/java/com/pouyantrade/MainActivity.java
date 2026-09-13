package com.pouyantrade;

import android.app.*;
import android.os.*;
import android.Manifest;
import android.content.pm.PackageManager;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    TextView status, price, signal, details;
    RadioGroup leverageGroup;
    ExecutorService pool = Executors.newSingleThreadExecutor();
    String lastSignal = "";

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        status=findViewById(R.id.status); price=findViewById(R.id.price);
        signal=findViewById(R.id.signal); details=findViewById(R.id.details);
        leverageGroup=findViewById(R.id.leverageGroup);
        findViewById(R.id.refresh).setOnClickListener(v -> analyze());
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
        analyze();
    }

    void analyze() {
        status.setText("در حال دریافت داده‌های DOGEUSDT Futures...");
        pool.submit(() -> {
            try {
                URL u = new URL("https://fapi.binance.com/fapi/v1/klines?symbol=DOGEUSDT&interval=1h&limit=120");
                HttpURLConnection c=(HttpURLConnection)u.openConnection();
                c.setConnectTimeout(8000); c.setReadTimeout(8000);
                BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream()));
                StringBuilder s=new StringBuilder(); String line;
                while((line=r.readLine())!=null)s.append(line);
                JSONArray a=new JSONArray(s.toString());
                ArrayList<Double> closes=new ArrayList<>();
                for(int i=0;i<a.length();i++) closes.add(a.getJSONArray(i).getDouble(4));
                double p=closes.get(closes.size()-1);
                double ema20=ema(closes,20), ema50=ema(closes,50), rsi=rsi(closes,14);
                double atr=atr(closes,14);
                String sig="WAIT";
                if(p>ema20 && ema20>ema50 && rsi>=52 && rsi<=70) sig="LONG";
                else if(p<ema20 && ema20<ema50 && rsi>=30 && rsi<=48) sig="SHORT";
                double entry=p;
                double stop = sig.equals("LONG") ? entry-1.5*atr : sig.equals("SHORT") ? entry+1.5*atr : entry;
                double tp1 = sig.equals("LONG") ? entry+1.5*atr : sig.equals("SHORT") ? entry-1.5*atr : entry;
                double tp2 = sig.equals("LONG") ? entry+3.0*atr : sig.equals("SHORT") ? entry-3.0*atr : entry;
                final double fp=p, fe20=ema20, fe50=ema50, frsi=rsi, fentry=entry, fstop=stop, ftp1=tp1, ftp2=tp2, fator=atr; final String fs=sig;
                runOnUiThread(() -> {
                    status.setText("تحلیل ۱ ساعته — Futures | داده زنده");
                    price.setText(String.format(Locale.US,"قیمت: %.6f USDT",fp));
                    signal.setText("وضعیت: "+fs);
                    int lev=(leverageGroup.getCheckedRadioButtonId()==R.id.x20)?20:10;
                    details.setText(String.format(Locale.US,
                        "EMA20: %.6f\nEMA50: %.6f\nRSI(14): %.1f\nاهرم انتخابی: %d×\n\nمنطق: روند + مومنتوم. این نسخه هنوز اجرای معامله خودکار ندارد.",fe20,fe50,frsi,lev,fentry,fstop,ftp1,ftp2,fator));
                });
            } catch(Exception e) {
                runOnUiThread(() -> status.setText("خطا در دریافت داده: "+e.getClass().getSimpleName()));
            }
        });
    }

    double ema(ArrayList<Double> x,int n){
        double k=2.0/(n+1), e=x.get(0);
        for(int i=1;i<x.size();i++) e=x.get(i)*k+e*(1-k);
        return e;
    }
   
    double rsi(ArrayList<Double> x,int n){
        double gain=0,loss=0;
        for(int i=x.size()-n;i<x.size();i++){ double d=x.get(i)-x.get(i-1); if(d>0)gain+=d; else loss-=d; }
        if(loss==0)return 100; double rs=(gain/n)/(loss/n); return 100-(100/(1+rs));
    }
}
