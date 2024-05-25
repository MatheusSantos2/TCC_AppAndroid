package Infraestructure.Services.ImageServices;

import android.graphics.Bitmap;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Point3;

import java.util.ArrayList;
import java.util.List;

import Infraestructure.Services.StrategyServices.CoordinatesProcessor;

public class SceneRedesignService {

    public static final Mat CAMERA_MATRIX = new Mat(3, 3, CvType.CV_64F) {{
        put(0, 0, 2.95389836e+03);
        put(0, 1, 0.00000000e+00);
        put(0, 2, 1.59195353e+03);
        put(1, 0, 0.00000000e+00);
        put(1, 1, 2.60746459e+03);
        put(1, 2, 1.28217291e+03);
        put(2, 0, 0.00000000e+00);
        put(2, 1, 0.00000000e+00);
        put(2, 2, 1.00000000e+00);
    }};

    public static final Mat DISTORTION_COEFFS = new Mat(1, 5, CvType.CV_64F) {{
        put(0, 0, 0.56239355);
        put(0, 1, 0.25599173);
        put(0, 2, 0.16078187);
        put(0, 3, 0.01695844);
        put(0, 4, 0.17199612);
    }};


    public static List<Point3> processDepthMap(Bitmap depthMap) {
        int imageHeight = depthMap.getWidth();
        int imageWidth = depthMap.getHeight();
        List<Point3> validCoordinates = new ArrayList<>();

        double minZ = Double.MAX_VALUE;
        double maxZ = Double.MIN_VALUE;

        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                double depth = depthMap.getPixel(y, x);
                minZ = Math.min(minZ, depth);
                maxZ = Math.max(maxZ, depth);
            }
        }

        double scaleX = 1 / (maxZ - minZ);

        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                double depth = depthMap.getPixel(y, x);
                double normalizedX = (depth - minZ) * scaleX;

                Point imageCoordinates = new Point(x, y);
                Point3 coordinates3D = get3DCoordinates(imageCoordinates, normalizedX);
                validCoordinates.add(coordinates3D);
            }
        }
        return validCoordinates;
    }

    public static Point3 get3DCoordinates(Point imageCoordinates, double depth) {
        MatOfPoint2f imageCoordinatesMat = new MatOfPoint2f(new Point(imageCoordinates.x, imageCoordinates.y));

        MatOfPoint2f undistortedCoordinatesMat = new MatOfPoint2f();
        Calib3d.undistortPoints(imageCoordinatesMat, undistortedCoordinatesMat, CAMERA_MATRIX, DISTORTION_COEFFS);

        Point[] undistortedPointsArray = undistortedCoordinatesMat.toArray();
        double[] undistortedCoordinates = {undistortedPointsArray[0].x, undistortedPointsArray[0].y};

        double[] coordinates3D = {imageCoordinates.x, imageCoordinates.y, depth};

        return new Point3(coordinates3D[0], coordinates3D[1], coordinates3D[2]);
    }

    public static Bitmap generateTopViewBitmap(List<Point3> points3D, Bitmap depthMap) {
        Bitmap result = new CoordinatesProcessor().generateHeatmap(points3D, 96, 129);

        return result;
    }

    private static Point3 findClosestPoint(List<Point3> points3D, double normalizedX, double normalizedZ) {
        Point3 closestPoint = null;
        double minDistance = Double.MAX_VALUE;

        for (Point3 point : points3D) {
            double distance = Math.sqrt(Math.pow(point.x - normalizedX, 2) + Math.pow(point.z - normalizedZ, 2));
            if (distance < minDistance) {
                minDistance = distance;
                closestPoint = point;
            }
        }

        return closestPoint;
    }

    private static int getColorFromDepthValue(double depth, double minDepth, double maxDepth) {
        // Normaliza a profundidade para o intervalo [0, 1] com base nos valores mínimo e máximo
        depth = (depth - minDepth) / (maxDepth - minDepth);
        depth = Math.max(0, Math.min(1, depth));

        // Mapa de cores "jet" (do azul ao verde, vermelho e amarelo)
        float[] rgb = new float[3];
        if (depth < 0.25) {
            rgb[0] = 0;
            rgb[1] = (float) (4 * depth);
            rgb[2] = 1;
        } else
        if (depth < 0.5) {
            rgb[0] = 0;
            rgb[1] = 1;
            rgb[2] = (float) (1 - 4 * (depth - 0.25));
        } else if (depth < 0.75) {
            rgb[0] = (float) (4 * (depth - 0.5));
            rgb[1] = 1;
            rgb[2] = 0;
        } else {
            rgb[0] = 1;
            rgb[1] = (float) (1 - 4 * (depth - 0.75));
            rgb[2] = 0;
        }

        // Converte as componentes de cor para int no intervalo [0, 255]
        int alpha = 255; // Opacidade máxima
        int red = (int) (rgb[0] * 255);
        int green = (int) (rgb[1] * 255);
        int blue = (int) (rgb[2] * 255);

        // Cria uma nova cor ARGB (Alpha, Red, Green, Blue)
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
}
