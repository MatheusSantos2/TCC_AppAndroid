package Infraestructure.Services.ImageServices;

import android.graphics.Bitmap;
import android.graphics.Color;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Point3;

import java.util.ArrayList;

import Utils.ImageHelper;

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

    public static ArrayList<Point3> calculate3DCoordinates(Mat depthMap) {
        ArrayList<Point3> points3DList = new ArrayList<>();
        int rows = depthMap.rows();
        int cols = depthMap.cols();

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                double[] pixelValues = depthMap.get(y, x);
                int r = (int) pixelValues[0];
                int g = (int) pixelValues[1];
                int b = (int) pixelValues[2];
                int a = (int) pixelValues[3];
                int pixel = (a << 24) | (r << 16) | (g << 8) | b; // Reconstroi o pixel RGBA

                Point imageCoordinate = new Point(x, y);
                Point3 point = get3DCoordinates(imageCoordinate, pixel);
                points3DList.add(point);
            }
        }

        return points3DList;
    }

    public static Point3 get3DCoordinates(Point imageCoordinates, int pixel) {
        double depth = convertDepth(pixel);

        MatOfPoint2f imageCoordinatesMat = new MatOfPoint2f(new Point(imageCoordinates.x, imageCoordinates.y));

        MatOfPoint2f undistortedCoordinatesMat = new MatOfPoint2f();
        Calib3d.undistortPoints(imageCoordinatesMat, undistortedCoordinatesMat, CAMERA_MATRIX, DISTORTION_COEFFS);

        Point[] undistortedPointsArray = undistortedCoordinatesMat.toArray();
        double[] undistortedCoordinates = {undistortedPointsArray[0].x, undistortedPointsArray[0].y};

        double[] coordinates3D = {imageCoordinates.x, undistortedCoordinates[1] * depth, depth};

        return new Point3((float) (coordinates3D[0]), (float) (coordinates3D[1]), (float) coordinates3D[2]);
    }

    public static double convertDepth(int pixel) {
        int r = (pixel >> 24) & 0xFF;
        int g = (pixel >> 16) & 0xFF;
        int b = (pixel >> 8) & 0xFF;
        int a = pixel & 0xFF;

        double rNormalized = r / 255.0;
        double gNormalized = g / 255.0;
        double bNormalized = b / 255.0;
        double aNormalized = a / 255.0;

        double[] blueColor = {0, 0, 1}; // Azul
        double[] redColor = {1, 0, 0}; // Vermelho

        double distanceBlue = Math.sqrt(Math.pow(rNormalized - blueColor[0], 2) + Math.pow(gNormalized - blueColor[1], 2) + Math.pow(bNormalized - blueColor[2], 2) + Math.pow(aNormalized - 1, 2)); // A opacidade do azul é 1
        double distanceRed = Math.sqrt(Math.pow(rNormalized - redColor[0], 2) + Math.pow(gNormalized - redColor[1], 2) + Math.pow(bNormalized - redColor[2], 2) + Math.pow(aNormalized - 1, 2)); // A opacidade do vermelho é 1

        double minDepth = 0.0;
        double maxDepth = 1.0;

        double convertedDepth = (distanceBlue / (distanceBlue + distanceRed)) * (maxDepth - minDepth) + minDepth;

        return convertedDepth;
    }

    public Bitmap generateTopViewBitmap(ArrayList<Point3> points3D, Mat depthMap) {
        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxZ = Double.MIN_VALUE;

        for (Point3 point : points3D) {
            double x = point.x;
            double z = point.z;
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
        }

        int imageWidth = (int) Math.ceil(maxX - minX) + 1;
        int imageHeight = (int) Math.ceil(maxZ - minZ) + 1;

        Bitmap bitmap = ImageHelper.Companion.createEmptyBitmap(imageWidth, imageHeight, Color.WHITE);

        for (Point3 point : points3D) {
            int x = (int) Math.round(point.x - minX);
            int z = (int) Math.round(point.z);

            int color = getColorFromDepthMap(depthMap, x, z);
            bitmap.setPixel(x, z, color);
        }

        return bitmap;
    }

    private int getColorFromDepthMap(Mat depthMap,int x, int y) {
        double[] rgba = new double[4];
        Mat depthMapRGBA = new Mat();
        depthMap.convertTo(depthMapRGBA, CvType.CV_64F);

        depthMapRGBA.get(y, x, rgba);
        int alpha = (int) rgba[3];
        int red = (int) rgba[2];
        int green = (int) rgba[1];
        int blue = (int) rgba[0];

        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
}
