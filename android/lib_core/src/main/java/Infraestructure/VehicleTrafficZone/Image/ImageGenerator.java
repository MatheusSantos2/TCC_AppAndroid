package Utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;

import org.opencv.core.Point;

import java.util.ArrayList;
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

    public Bitmap rotateBitmapClockwise(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Bitmap resizeBitmap(Bitmap inputBitmap, int newWidth, int newHeight) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(inputBitmap, newWidth, newHeight, false);
        return resizedBitmap;
    }

    public Bitmap mapColors(Bitmap bitmap, int color) {
        Bitmap resultBitmap = bitmap.copy(bitmap.getConfig(), true);

        int width = resultBitmap.getWidth();
        int height = resultBitmap.getHeight();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = resultBitmap.getPixel(x, y);

                if (pixel == Color.BLACK) {
                    resultBitmap.setPixel(x, y, color);
                }
            }
        }

        return resultBitmap;
    }

    public void createMagentaStain(Bitmap image, int threshold, int stainColor, int stainRadius) {
        int width = image.getWidth();
        int height = image.getHeight();
        boolean[][] visited = new boolean[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (!visited[x][y] && isMagentaPixel(image.getPixel(x, y), threshold)) {
                    List<Point> region = new ArrayList<>();
                    floodFill(image, visited, x, y, threshold, region);

                    applyStainToRegion(image, region, stainColor, stainRadius);
                }
            }
        }
    }

    private void applyStainToRegion(Bitmap image, List<Point> region, int stainColor, int stainRadius) {
        for (Point point : region) {
            int centerX = (int)point.x;
            int centerY = (int)point.y;

            for (int dx = -stainRadius; dx <= stainRadius; dx++) {
                for (int dy = -stainRadius; dy <= stainRadius; dy++) {
                    int nx = centerX + dx;
                    int ny = centerY + dy;

                    if (nx >= 0 && nx < image.getWidth() && ny >= 0 && ny < image.getHeight()) {
                        double distance = Math.sqrt(dx * dx + dy * dy);
                        double intensity = 1.0 - (distance / stainRadius);

                        int pixel = image.getPixel(nx, ny);
                        int newColor = blendColors(pixel, stainColor, intensity);
                        image.setPixel(nx, ny, newColor);
                    }
                }
            }
        }
    }

    private int blendColors(int color1, int color2, double ratio) {
        int alpha = (int) (Color.alpha(color1) * (1.0 - ratio) + Color.alpha(color2) * ratio);
        int red = (int) (Color.red(color1) * (1.0 - ratio) + Color.red(color2) * ratio);
        int green = (int) (Color.green(color1) * (1.0 - ratio) + Color.green(color2) * ratio);
        int blue = (int) (Color.blue(color1) * (1.0 - ratio) + Color.blue(color2) * ratio);
        return Color.argb(alpha, red, green, blue);
    }

    private void floodFill(Bitmap image, boolean[][] visited, int x, int y, int threshold, List<Point> region) {
        int width = image.getWidth();
        int height = image.getHeight();
        int targetColor = image.getPixel(x, y);

        if (visited[x][y] || !isMagentaPixel(targetColor, threshold)) {
            return;
        }

        visited[x][y] = true;
        region.add(new Point(x, y));

        int[] dx = {0, 0, -1, 1};
        int[] dy = {-1, 1, 0, 0};

        for (int i = 0; i < 4; i++) {
            int nx = x + dx[i];
            int ny = y + dy[i];

            if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                floodFill(image, visited, nx, ny, threshold, region);
            }
        }
    }

    private boolean isMagentaPixel(int pixel, int threshold) {
        int red = Color.red(pixel);
        int green = Color.green(pixel);
        int blue = Color.blue(pixel);
        int magentaThreshold = threshold;

        return (red >= 255 - magentaThreshold) && (green <= magentaThreshold) && (blue >= 255 - magentaThreshold);
    }
}
