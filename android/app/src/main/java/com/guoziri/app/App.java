package com.guoziri.app;

import android.app.Application;
import android.content.Context;

/** Chaquopy 文档推荐：在 Application 里尽早启动 Python */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        if (!com.chaquo.python.Python.isStarted()) {
            com.chaquo.python.Python.start(new com.chaquo.python.android.AndroidPlatform(this));
        }
    }
}
