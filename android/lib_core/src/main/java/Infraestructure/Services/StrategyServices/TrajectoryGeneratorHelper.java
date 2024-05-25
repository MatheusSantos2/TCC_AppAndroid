package Infraestructure.Services.StrategyServices;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.Pair;

import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

import java.util.ArrayList;
import java.util.List;

public class TrajectoryGeneratorService {

    public static Pair<List<PointF>, Bitmap> drawCirclesOnBitmap(Bitmap bitmap) {
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);

        Bitmap resultBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(resultBitmap);

        PointF startPoint = findLastNonRedLine(bitmap);
        PointF endPoint = findFirstNonRedLine(bitmap);

        if (startPoint != null && endPoint != null) {
            canvas.drawCircle(startPoint.x, startPoint.y, 10, paint);
            canvas.drawCircle(endPoint.x, endPoint.y, 10, paint);

            resultBitmap = fillCircle(resultBitmap, startPoint, 10);
            resultBitmap = fillCircle(resultBitmap, endPoint, 10);
        }

        return generateCurve(resultBitmap,  startPoint,  endPoint,  3,  100);
    }

    public static PointF findLastNonRedLine(Bitmap heatmap) {
        int h = heatmap.getHeight();
        int w = heatmap.getWidth();

        double sceneWidthMeters = 10.0;
        double imageWidthPixels = w;
        double pixelsPerMeter = imageWidthPixels / sceneWidthMeters;

        int radiusPixels = (int) pixelsPerMeter;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = heatmap.getPixel(x, y);
                if (!isWhiteOrTransparent(pixel)) {
                    if (!isRedPixel(pixel)) {
                        if (!hasRedPixelsAround(heatmap, x, y, radiusPixels)) {
                            return new PointF(x, y);
                        }
                    }
                }
            }
        }
        return null;
    }

    public static PointF findFirstNonRedLine(Bitmap image) {
        int h = image.getHeight();
        int w = image.getWidth();

        double sceneWidthMeters = 10.0;
        double imageWidthPixels = w;
        double pixelsPerMeter = imageWidthPixels / sceneWidthMeters;

        int radiusPixels = (int) pixelsPerMeter;

        int halfHeight = h / 2;

        for (int y = h - 1; y >= halfHeight; y--) {
            for (int x = 0; x < w; x++) {
                int pixel = image.getPixel(x, y);
                if (!isWhiteOrTransparent(pixel)) {
                    if (!isRedPixel(pixel)) {
                        if (!hasRedPixelsAround(image, x, y, radiusPixels)) {
                            return new PointF(x, y);
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean isWhiteOrTransparent(int pixel) {
        int red = Color.red(pixel);
        int green = Color.green(pixel);
        int blue = Color.blue(pixel);
        int alpha = Color.alpha(pixel);
        return red == 255 && green == 255 && blue == 255 ||
                red == 0 && green == 0 && blue == 0 ||
                alpha == 0;
    }

    private static boolean hasRedPixelsAround(Bitmap heatmap, int x, int y, int radius) {
        int h = heatmap.getHeight();
        int w = heatmap.getWidth();
        int startX = Math.max(0, x - radius);
        int endX = Math.min(w, x + radius);
        int startY = Math.max(0, y - radius);
        int endY = Math.min(h, y + radius);

        for (int i = startX; i < endX; i++) {
            for (int j = startY; j < endY; j++) {
                int pixel = heatmap.getPixel(i, j);
                if (isRedPixel(pixel)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isRedPixel(int pixel) {
        int red = Color.red(pixel);
        int redThreshold = 150;
        return red > redThreshold;
    }

    public static Bitmap fillCircle(Bitmap image, PointF center, int radius) {
        Bitmap resultBitmap = image.copy(image.getConfig(), true);
        int h = resultBitmap.getHeight();
        int w = resultBitmap.getWidth();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double distance = Math.sqrt(Math.pow(x - center.x, 2) + Math.pow(y - center.y, 2));
                if (distance <= radius) {
                    resultBitmap.setPixel(x, y, Color.BLACK);
                }
            }
        }
        return resultBitmap;
    }

    public static Pair<List<PointF>, Bitmap> generateCurve(Bitmap bitmap, PointF startPoint, PointF endPoint, int degree, int numPoints) {
        double[] xValues = {startPoint.x, endPoint.x};
        double[] yValues = {startPoint.y, endPoint.y};

        WeightedObservedPoints obs = new WeightedObservedPoints();
        for (int i = 0; i < xValues.length; i++) {
            obs.add(xValues[i], yValues[i]);
        }

        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(degree);
        double[] coefficients = fitter.fit(obs.toList());

        double[] xValuesCurve = linspace(xValues[0], xValues[1], numPoints);
        List<PointF> curvePoints = new ArrayList<>();
        for (int i = 0; i < numPoints; i++) {
            float x = (float) xValuesCurve[i];
            float y = (float) evaluatePolynomial(coefficients, xValuesCurve[i]);
            curvePoints.add(new PointF(x, y));
        }

        Bitmap resultBitmap = drawCurveOnBitmap(bitmap, curvePoints);

        return new Pair<>(curvePoints, resultBitmap);
    }

    private static Bitmap drawCurveOnBitmap(Bitmap bitmap, List<PointF> points) {
        Bitmap resultBitmap = bitmap.copy(bitmap.getConfig(), true);
        Canvas canvas = new Canvas(resultBitmap);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);

        Path path = new Path();
        path.moveTo(points.get(0).x, points.get(0).y);
        for (int i = 1; i < points.size(); i++) {
            path.lineTo(points.get(i).x, points.get(i).y);
        }
        canvas.drawPath(path, paint);

        return resultBitmap;
    }

    private static double[] linspace(double start, double end, int numPoints) {
        double[] values = new double[numPoints];
        double step = (end - start) / (numPoints - 1);
        for (int i = 0; i < numPoints; i++) {
            values[i] = start + i * step;
        }
        return values;
    }

    private static double evaluatePolynomial(double[] coefficients, double x) {
        double result = 0.0;
        for (int i = 0; i < coefficients.length; i++) {
            result += coefficients[i] * Math.pow(x, i);
        }
        return result;
    }
}
