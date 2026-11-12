package com.guoziri.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

import java.io.File;

public class MainActivity extends Activity {

    private WebView webView;
    private Thread flaskThread;
    private static final int PORT = 5000;
    private TextView statusText;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ---------- UI：先显示状态页，服务起来再切 WebView ----------
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.rgb(0xF2, 0xF2, 0xF7));
        statusText = new TextView(this);
        statusText.setText("正在启动本地服务…");
        statusText.setTextSize(16);
        statusText.setTextColor(Color.rgb(0x3C, 0x3C, 0x43));
        statusText.setPadding(40, 0, 40, 0);
        statusText.setGravity(Gravity.CENTER);
        root.addView(statusText);
        // 便捷导出崩溃日志的入口（长按状态文字）
        statusText.setOnLongClickListener(v -> {
            String log = App.readCrashFile(this);
            android.content.Intent send = new android.content.Intent();
            send.setAction(android.content.Intent.ACTION_SEND);
            send.putExtra(android.content.Intent.EXTRA_TEXT, log);
            send.setType("text/plain");
            startActivity(android.content.Intent.createChooser(send, "发送崩溃日志"));
            return true;
        });
        setContentView(root);

        // ---------- 数据与资源目录 ----------
        File dataDir = getFilesDir();
        File webDir = new File(getCacheDir(), "web");
        try {
            Assets.unpackWeb(this, webDir);
        } catch (Exception e) {
            App.writeCrashFile(this, e);
            android.util.Log.e("guoziri", "unpack failed", e);
            statusText.setText("资源解压失败：" + e.getMessage());
            return;
        }

        // ---------- 起 Flask ----------
        final String pyDataDir = dataDir.getAbsolutePath();
        final String pyWebDir = webDir.getAbsolutePath();
        flaskThread = new Thread(() -> {
            try {
                Python py = Python.getInstance();
                PyObject main = py.getModule("run_server");
                main.callAttr("start_server", pyDataDir, pyWebDir, PORT);
            } catch (Throwable t) {
                // Flask 线程内异常：写 crash.txt 并把错误顶到状态页
                App.writeCrashFile(this, t);
                runOnUiThread(() -> statusText.setText("服务启动失败，请把设置页导出的日志发给开发者\n" + t.getMessage()));
            }
        });
        flaskThread.start();

        // ---------- WebView 就绪后替换 ----------
        webView = new WebView(this);
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setCacheMode(WebSettings.LOAD_NO_CACHE);
        ws.setAllowFileAccess(false);
        ws.setAllowContentAccess(false);
        webView.setWebViewClient(new WebViewClient());

        waitAndLoad("http://127.0.0.1:" + PORT + "/", 15000);
    }

    private void waitAndLoad(String url, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        new Thread(() -> {
            boolean ok = false;
            while (System.currentTimeMillis() < deadline) {
                try {
                    java.net.HttpURLConnection c =
                        (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                    c.setConnectTimeout(500);
                    c.connect();
                    int code = c.getResponseCode();
                    c.disconnect();
                    if (code == 200) { ok = true; break; }
                } catch (Exception e) {
                    try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                }
            }
            final boolean ready = ok;
            runOnUiThread(() -> {
                if (ready) {
                    setContentView(webView);
                } else {
                    statusText.setText("服务未能启动（15 秒超时）。\n\n"
                        + "如果反复出现，请到系统设置里彻底关闭本应用后重试，\n"
                        + "或把崩溃日志发给开发者。");
                }
            });
        }).start();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (flaskThread != null) flaskThread.interrupt();
        super.onDestroy();
    }
}
