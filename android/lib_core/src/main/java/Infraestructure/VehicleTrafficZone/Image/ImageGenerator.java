package Infraestructure.VehicleTrafficZone.Image;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;

import java.util.List;

import Models.Point3D;

public class ImageGenerator {
    public Bitmap createBitmapImageXZ(List< Point3D> positions, Integer imageWidth, Integer imageHeight) {
        Bitmap bitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.RGB_565);
        bitmap.eraseColor(Color.WHITE);

        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);

        float xMin = Float.MAX_VALUE;
        float xMax = Float.MIN_VALUE;
        float zMin = Float.MAX_VALUE;
        float zMax = Float.MIN_VALUE;

        for (Point3D point : positions) {
            float x = point.getX();
            float z = point.getZ();

            xMin = Math.min(xMin, x);
            xMax = Math.max(xMax, x);
            zMin = Math.min(zMin, z);
            zMax = Math.max(zMax, z);
        }

        float xRange = Math.max(Math.abs(xMin), Math.abs(xMax));
        float zRange = zMax - zMin;

        for (Point3D point : positions)
        {
            float x = point.getX();
            float z = point.getZ();

            float px = (x - xMin) / xRange * imageWidth;
            float py = (z - zMin) / zRange * imageHeight;

            paint.setColor(Color.MAGENTA);
            canvas.drawPoint(px, py, paint);
        }

        return bitmapMirror(flipBitmapVertically(bitmap));
    }

    public Bitmap bitmapMirror(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.preScale(-1, 1);

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
    }
    
    public Bitmap createEmptyImage(int imageWidth, int imageHeight)
    {
        Bitmap bitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.RGB_565);
        bitmap.eraseColor(Color.BLACK);
        
        return bitmap;
    }

    public Bitmap flipBitmapVertically(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postRotate(180);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Bitmap resizeBitmap(Bitmap inputBitmap, int newWidth, int newHeight) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(inputBitmap, newWidth, newHeight, false);
        return resizedBitmap;
    }
}
