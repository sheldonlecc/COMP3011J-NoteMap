package com.noworld.notemap.data;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;

/**
 * 将用户选择的图片/视频复制到应用内部目录，返回本地文件绝对路径。
 */
class LocalMediaStore {
    private final Context context;

    LocalMediaStore(Context context) {
        this.context = context.getApplicationContext();
    }

    String copyToLocal(Uri uri, String mediaType) throws Exception {
        if (uri == null) return null;
        File dir = new File(context.getFilesDir(), "chat_media");
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        String ext = guessExt(uri, mediaType);
        File target = new File(dir, "media_" + System.currentTimeMillis() + ext);
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(target)) {
            if (in == null) return null;
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
        }
        return target.getAbsolutePath();
    }

    private String guessExt(Uri uri, String mediaType) {
        String mime = getMime(uri);
        if (!TextUtils.isEmpty(mime)) {
            if (mime.toLowerCase(Locale.US).contains("png")) return ".png";
            if (mime.toLowerCase(Locale.US).contains("gif")) return ".gif";
            if (mime.toLowerCase(Locale.US).contains("webp")) return ".webp";
            if (mime.toLowerCase(Locale.US).contains("mp4")) return ".mp4";
        }
        if ("video".equals(mediaType)) return ".mp4";
        return ".jpg";
    }

    private String getMime(Uri uri) {
        ContentResolver resolver = context.getContentResolver();
        return resolver.getType(uri);
    }
}
