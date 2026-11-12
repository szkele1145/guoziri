package com.guoziri.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
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
    private LinearLayout rootLayout;
    private boolean webViewShown = false;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ---------- 状态页：样式对齐前端 splash，用户感知是同一个连续的启动画面 ----------
        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setGravity(Gravity.CENTER);
        rootLayout.setBackgroundColor(Color.rgb(0xF2, 0xF2, 0xF7));
        statusText = new TextView(this);
        statusText.setText("🥬\n\n过日子\n正在解压资源…");
        statusText.setTextSize(17);
        statusText.setTextColor(Color.rgb(0x1C, 0x1C, 0x1E));
        statusText.setPadding(40, 0, 40, 0);
        statusText.setGravity(Gravity.CENTER);
        statusText.setLineSpacing(8, 1.2f);
        rootLayout.addView(statusText);
        statusText.setOnLongClickListener(v -> {
            String log = App.readCrashFile(this);
            android.content.Intent send = new android.content.Intent();
            send.setAction(android.content.Intent.ACTION_SEND);
            send.putExtra(android.content.Intent.EXTRA_TEXT, log);
            send.setType("text/plain");
            startActivity(android.content.Intent.createChooser(send, "发送诊断日志"));
            return true;
        });
        setContentView(rootLayout);

        // ---------- WebView 先于一切构建好（含所有调试回调） ----------
        webView = new WebView(this);
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setCacheMode(WebSettings.LOAD_NO_CACHE);
        ws.setAllowFileAccess(false);
        ws.setAllowContentAccess(false);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setTextZoom(100);                       // 防系统字体缩放打乱布局
        // 混合内容放开：本页 http，但 AI 转发在后端，前端仍是 http，无需 mixed content
        webView.setBackgroundColor(Color.rgb(0xF2, 0xF2, 0xF7));

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage m) {
                android.util.Log.i("guoziri-js", "[" + m.messageLevel() + "] " + m.message()
                        + " @" + m.sourceId() + ":" + m.lineNumber());
                return true;
            }
        });
        // touch: let JS decide if tap hit an input; if not, hide keyboard
        webView.setOnTouchListener((v, ev) -> {
            if (ev.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                final float x = ev.getX(), y = ev.getY();
                webView.evaluateJavascript(
                    "(function(){var w=window.devicePixelRatio;var el=document.elementFromPoint(" + x + "*w," + y + "*w);return (el&&(el.tagName==='INPUT'||el.tagName==='TEXTAREA'))?'1':'0';})()",
                    value -> {
                        if (value != null && value.contains("0")) {
                            android.view.inputmethod.InputMethodManager imm =
                                (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                            if (imm != null) imm.hideSoftInputFromWindow(webView.getWindowToken(), 0);
                        }
                    });
            }
            return false;
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                android.util.Log.i("guoziri", "onPageFinished: " + url);
                // 页面真的渲染完成才切视图，杜绝白屏窗口
                if (!webViewShown) {
                    webViewShown = true;
                    setContentView(webView);
                }
            }
            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                if (req.isForMainFrame()) {
                    android.util.Log.e("guoziri", "main frame error: " + err.getDescription());
                    runOnUiThread(() -> {
                        if (!webViewShown) {
                            statusText.setText("页面加载失败：" + err.getDescription());
                        }
                    });
                }
            }
        });

        // ---------- 资源解压 ----------
        File dataDir = getFilesDir();
        File webDir = new File(getCacheDir(), "web");
        try {
            Assets.unpackWeb(this, webDir);
            statusText.setText("🥬\n\n过日子\n正在启动本地服务…");
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
                App.writeCrashFile(this, t);
                runOnUiThread(() -> {
                    if (!webViewShown) {
                        statusText.setText("🥬\n\n过日子\n服务启动失败：" + t.getMessage());
                    }
                });
            }
        });
        flaskThread.start();

        // ---------- 探活成功才让 WebView 开始 load ----------
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
                // 无论探活结果如何都 loadUrl：某些 ROM 的 HttpURLConnection 走系统代理连不通 localhost，
                // 但 WebView 自身不受影响。失败路径由 onReceivedError 显示。
                statusText.setText("🥬\n\n过日子\n正在加载界面…");
                webView.loadUrl(url);
                if (!ready) {
                    android.util.Log.w("guoziri", "probe failed but loading anyway");
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
