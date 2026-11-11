package com.guoziri.app;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/** 把 assets/web 静态资源解压到私有目录，供 Flask send_from_directory 使用 */
public final class Assets {

    private Assets() {}

    /** 每次启动都同步（文件少、总量小，开销可忽略），保证升级 APK 后资源是新的 */
    public static void unpackWeb(Context ctx, File outDir) {
        try {
            AssetManager am = ctx.getAssets();
            unpackDir(am, "web", outDir);
        } catch (Exception e) {
            throw new RuntimeException("解压 web 资源失败", e);
        }
    }

    private static void unpackDir(AssetManager am, String assetPath, File outDir) throws Exception {
        // list 为空可能是文件也可能是空目录，先按目录试
        String[] children = am.list(assetPath);
        File out = new File(outDir, new File(assetPath).getName());
        if (children == null || children.length == 0) {
            copyFile(am, assetPath, out);
            return;
        }
        out.mkdirs();
        for (String child : children) {
            unpackDir(am, assetPath + "/" + child, out);
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
