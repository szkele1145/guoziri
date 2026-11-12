package com.guoziri.app;

import android.app.Application;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Application: start Python early, and capture any uncaught crash
 * into a readable file the user can share (no adb needed).
 */
public class App extends Application {

    public static final String TAG = "guoziri";

    @Override
    public void onCreate() {
        super.onCreate();
        // 全局崩溃捕获：写到 app 私有目录 crash.txt，用户可在系统"文件"里看不到，
        // 所以我们再提供 MainActivity 里的"导出崩溃日志"按钮
        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                StringWriter sw = new StringWriter();
                throwable.printStackTrace(new PrintWriter(sw));
                File dir = getFilesDir();
                File log = new File(dir, "crash.txt");
                try (FileWriter fw = new FileWriter(log, true)) {
                    fw.write("==== " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date())
                            + " thread=" + thread.getName() + " ====\n");
                    fw.write(sw.toString());
                    fw.write("\n");
                }
            } catch (Throwable ignored) {}
            if (defaultHandler != null) defaultHandler.uncaughtException(thread, throwable);
        });

        try {
            if (!com.chaquo.python.Python.isStarted()) {
                com.chaquo.python.Python.start(new com.chaquo.python.android.AndroidPlatform(this));
            }
            Log.i(TAG, "Python started OK");
        } catch (Throwable t) {
            Log.e(TAG, "Python.start failed", t);
            writeCrash(t);
            // Python 起不来后面必然崩，提前写日志再抛给系统
            throw t instanceof RuntimeException ? (RuntimeException) t : new RuntimeException(t);
        }
    }

    private void writeCrash(Throwable t) {
        try {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            File log = new File(getFilesDir(), "crash.txt");
            try (FileWriter fw = new FileWriter(log, true)) {
                fw.write("==== Python.start failed " + new Date() + " ====\n" + sw + "\n");
            }
        } catch (Throwable ignored) {}
    }

    /** 供其他组件写崩溃日志 */
    public static void writeCrashFile(android.content.Context ctx, Throwable t) {
        try {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            File log = new File(ctx.getFilesDir(), "crash.txt");
            try (FileWriter fw = new FileWriter(log, true)) {
                fw.write("==== " + new Date() + " ====\n" + sw + "\n");
            }
        } catch (Throwable ignored) {}
    }

    /** 崩溃日志内容（导出用） */
    public static String readCrashFile(android.content.Context ctx) {
        try {
            File log = new File(ctx.getFilesDir(), "crash.txt");
            if (!log.exists()) return "（暂无崩溃记录）";
            byte[] buf = new byte[(int) log.length()];
            try (java.io.FileInputStream fis = new java.io.FileInputStream(log)) {
                fis.read(buf);
            }
            return new String(buf, "UTF-8");
        } catch (Throwable t) {
            return "读取失败: " + t.getMessage();
        }
    }
}
