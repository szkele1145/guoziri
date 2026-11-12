package com.guoziri.app;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 把前端静态资源从 assets 解压到私有目录，供 Flask send_from_directory 使用。
 * 注意：Gradle 的 assets.srcDirs 指向 web/ 目录时，其"内容"直接映射到 assets 根
 * （即 index.html 在 assets/index.html，而不是 assets/web/index.html）。
 */
public final class Assets {

    private Assets() {}

    /** 每次启动都同步（文件少、总量小），保证升级 APK 后资源是新的 */
    public static void unpackWeb(Context ctx, File outDir) {
        try {
            AssetManager am = ctx.getAssets();
            // 资源在 assets 根：index.html / app.js / style.css / fonts/
            unpackDir(am, "", outDir);
        } catch (Exception e) {
            throw new RuntimeException("解压 web 资源失败", e);
        }
    }

    private static void unpackDir(AssetManager am, String assetPath, File outDir) throws Exception {
        String[] children = am.list(assetPath);
        if (children == null || children.length == 0) {
            // 空路径没有"文件"可拷；只有非空路径才按文件处理
            if (!assetPath.isEmpty()) {
                copyFile(am, assetPath, new File(outDir, new File(assetPath).getName()));
            }
            return;
        }
        // 是目录：建目录并递归
        File out = assetPath.isEmpty() ? outDir : new File(outDir, assetPath);
        out.mkdirs();
        for (String child : children) {
            unpackDir(am, assetPath.isEmpty() ? child : assetPath + "/" + child, outDir);
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
