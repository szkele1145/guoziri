package com.guoziri.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;

import com.chaquo.python.PyObject;

import java.io.File;

public class MainActivity extends Activity {

    private WebView webView;
    private Thread flaskThread;
    private static final int PORT = 5000;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ---------- 1. Python 已在 App(Application) 中启动 ----------

        // 数据目录：app 私有 files 目录（系统级隔离，卸载即清，其它应用读不到）
        File dataDir = getFilesDir();

        // 静态资源目录：assets/web 解压到内部缓存（Chaquopy 的 assets 可直接流式读，
        // 但 Flask send_from_directory 需要真实路径，首启动解压一次）
        File webDir = new File(getCacheDir(), "web");
        Assets.unpackWeb(this, webDir);

        // ---------- 2. 起 Flask（后台线程，127.0.0.1） ----------
        final String pyDataDir = dataDir.getAbsolutePath();
        final String pyWebDir = webDir.getAbsolutePath();
        flaskThread = new Thread(() -> {
            PyObject main = com.chaquo.python.Python.getInstance()
                    .getModule("run_server");   // backend/run_server.py
            main.callAttr("start_server", pyDataDir, pyWebDir, PORT);
        });
        flaskThread.start();

        // ---------- 3. WebView 加载本地服务 ----------
        webView = new WebView(this);
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);            // localStorage（仅 UI 状态用）
        ws.setCacheMode(WebSettings.LOAD_NO_CACHE);
        ws.setAllowFileAccess(false);
        ws.setAllowContentAccess(false);

        webView.setWebViewClient(new WebViewClient());
        setContentView(webView);

        // 等端口就绪再加载，最多 10 秒
        waitAndLoad("http://127.0.0.1:" + PORT + "/", 10000);
    }

    private void waitAndLoad(String url, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        new Thread(() -> {
            while (System.currentTimeMillis() < deadline) {
                try {
                    java.net.HttpURLConnection c =
                        (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                    c.setConnectTimeout(500);
                    c.connect();
                    c.disconnect();
                    break;
                } catch (Exception e) {
                    try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                }
            }
            runOnUiThread(() -> webView.loadUrl(url));
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
