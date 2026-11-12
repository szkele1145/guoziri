package com.guoziri.app;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 把前端静态资源从 assets 解压到私有目录，供 Flask send_from_directory 使用。
 *
 * Gradle assets.srcDirs 指向 web/ 时，内容直接映射到 assets 根：
 *   assets/index.html, assets/app.js, assets/style.css, assets/fonts/...
 *
 * 策略：白名单解压（不递归整个 assets 根，避免把 chaquopy/ 等大目录卷进来，
 * 也规避部分设备 am.list("") 返回异常的问题）。
 */
public final class Assets {

    private static final String TAG = "guoziri";

    /** 需要解压的前端文件/目录（相对 assets 根） */
    private static final String[] ENTRIES = {
        "index.html",
        "app.js",
        "style.css",
        "preview.html",
        "fonts",           // 目录：HarmonyOS Sans SC
    };

    private Assets() {}

    /** 每次启动都同步（文件少、总量小），保证升级 APK 后资源是新的 */
    public static void unpackWeb(Context ctx, File outDir) {
        try {
            AssetManager am = ctx.getAssets();
            for (String entry : ENTRIES) {
                String[] children = am.list(entry);
                if (children != null && children.length > 0) {
                    // 目录：递归
                    unpackDir(am, entry, new File(outDir, entry));
                } else {
                    // 文件：直接拷
                    copyFile(am, entry, new File(outDir, entry));
                }
            }
            // 校验关键文件真的落了地
            File index = new File(outDir, "index.html");
            if (!index.exists() || index.length() == 0) {
                throw new RuntimeException("index.html 解压后缺失");
            }
            Log.i(TAG, "assets unpacked to " + outDir.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "unpackWeb failed", e);
            throw new RuntimeException("解压 web 资源失败: " + e.getMessage(), e);
        }
    }

    private static void unpackDir(AssetManager am, String assetPath, File outDir) throws Exception {
        outDir.mkdirs();
        String[] children = am.list(assetPath);
        if (children == null) return;
        for (String child : children) {
            String sub = assetPath + "/" + child;
            String[] subChildren = am.list(sub);
            if (subChildren != null && subChildren.length > 0) {
                unpackDir(am, sub, new File(outDir, child));
            } else {
                copyFile(am, sub, new File(outDir, child));
            }
        }
    }

    private static void copyFile(AssetManager am, String assetPath, File outFile) throws Exception {
        outFile.getParentFile().mkdirs();
        try (InputStream in = am.open(assetPath);
             OutputStream os = new FileOutputStream(outFile)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
        }
    }
}
