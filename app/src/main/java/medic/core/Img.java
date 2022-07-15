package medic.core;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static medic.core.Utils.createFileIfNotExists;
import static medic.core.Utils.logError;

public class Img {
    /**
     * 位图
     */
    private final Bitmap bitmap;
    /**
     * 画布，提供画不同的图形方法
     */
    private final Canvas canvas;
    /**
     * 画笔
     */
    private Paint paint;

    public enum PathType {
        // 路径类型，本地或者网络
        LOCAL_PATH,
        URL
    }

    /**
     * 将本地/网络图片转成 Bitmap.
     *
     * @param pathOrUrl 图片路径
     * @return 位图
     */
    public static Bitmap getBitmap(PathType type, String pathOrUrl) {
        if (type == PathType.LOCAL_PATH) {
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(pathOrUrl))) {
                return BitmapFactory.decodeStream(bis);
            } catch (IOException e) {
                logError("path: " + pathOrUrl, e);
                return null;
            }
        } else if (type == PathType.URL) {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(pathOrUrl).openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                return BitmapFactory.decodeStream(input);
            } catch (IOException e) {
                logError("url: " + pathOrUrl, e);
                return null;
            }
        } else {
            logError(new Exception("错误的类型：" + type));
            return null;
        }
    }

    public Img(int width, int height) {
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        paint = new Paint();
    }

    Img(PathType type, String pathOrUrl) {
        this.bitmap = getBitmap(type, pathOrUrl);
        canvas = new Canvas(bitmap);
    }

    public int getWidth() {
        return canvas.getWidth();
    }

    public int getHeight() {
        return canvas.getHeight();
    }

    public void drawText(String text, float x, float y) {
        canvas.drawText(text, x, y, paint);
    }

    public void drawLine(float startX, float startY, float stopX, float stopY) {
        canvas.drawLine(startX, startY, stopX, stopY, paint);
    }

    public void drawRect(float left, float top, float right, float bottom) {
        canvas.drawRect(left, top, right, bottom, paint);
    }

    public void drawCircle(float cx, float cy, float radius) {
        canvas.drawCircle(cx, cy, radius, paint);
    }


    public void setArgbColor(int alpha, int red, int green, int blue) {
        paint.setColor(Color.argb(alpha, red, green, blue));
    }

    public void setRgbColor(int red, int green, int blue) {
        paint.setColor(Color.rgb(red, green, blue));
    }

    public void setStyle(Style style) {
        paint.setStyle(style);
    }


    public boolean save(File file) {
        return saveBitmapToLocalFile(bitmap, file);
    }

    public static boolean saveBitmapToLocalFile(Bitmap bitmap, File f) {
        if (!createFileIfNotExists(f)) {
            logError(new UnexpectedStateException("创建图片失败"));
            return false;
        }
        try (FileOutputStream fos = new FileOutputStream(f)) {
            return bitmap.compress(CompressFormat.JPEG, 100, fos);
        } catch (IOException e) {
            logError(e);
            return false;
        }
    }
}
