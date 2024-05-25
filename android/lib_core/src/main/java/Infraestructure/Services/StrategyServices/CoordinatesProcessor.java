package Infraestructure.Services.StrategyServices;

import android.graphics.Bitmap;
import android.graphics.Color;

import org.opencv.core.Point3;

import java.util.List;

public class CoordinatesProcessor {
    public Bitmap generateHeatmap(List<Point3> points, int width, int height) {
        Bitmap heatmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        double[][] coordinates = pointsToArray(points);
        int[][] pointCounts = new int[width][height];
        int[][] redSum = new int[width][height];
        int[][] greenSum = new int[width][height];
        int[][] blueSum = new int[width][height];

        for (int i = 0; i < coordinates.length; i++) {
            double[] coordinate = coordinates[i];
            int x = (int) coordinate[0];
            int y = scaleCoordinateToBitmap((float) coordinate[1], height);

            float depth = (float) coordinate[1];
            int color = getJetColor(depth);

            // Update sums and point counts for neighboring pixels
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    int nx = x + dx;
                    int ny = y + dy;
                    if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                        redSum[nx][ny] += Color.red(color);
                        greenSum[nx][ny] += Color.green(color);
                        blueSum[nx][ny] += Color.blue(color);
                        pointCounts[nx][ny]++;
                    }
                }
            }
        }

        // Set average color for each pixel based on neighboring points
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (pointCounts[x][y] > 0) {
                    int avgRed = redSum[x][y] / pointCounts[x][y];
                    int avgGreen = greenSum[x][y] / pointCounts[x][y];
                    int avgBlue = blueSum[x][y] / pointCounts[x][y];
                    heatmap.setPixel(x, y, Color.rgb(avgRed, avgGreen, avgBlue));
                }
            }
        }

        return heatmap;
    }

    private double[][] pointsToArray(List<Point3> points) {
        double[][] array = new double[points.size()][2];
        for (int i = 0; i < points.size(); i++) {
            Point3 point = points.get(i);
            array[i][0] = point.x;
            array[i][1] = point.z;
        }
        return array;
    }

    private int scaleCoordinateToBitmap(float coordinate, int bitmapHeight) {
        return Math.round(coordinate * (bitmapHeight - 1));
    }

    private int getJetColor(float depth) {
        int r = (int) (255 * Math.max(0, Math.min(1, 1.5 - Math.abs(2 * depth - 1.5))));
        int g = (int) (255 * Math.max(0, Math.min(1, 1.5 - Math.abs(2 * depth - 0.5))));
        int b = (int) (255 * Math.max(0, Math.min(1, 1.5 - Math.abs(2 * depth + 0.5))));
        return Color.rgb(r, g, b);
    }
}
