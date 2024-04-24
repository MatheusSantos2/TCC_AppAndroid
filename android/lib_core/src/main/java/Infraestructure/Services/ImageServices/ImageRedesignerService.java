package Infraestructure.Services.ImageServices;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

import Models.Point2D;
import Models.Point3D;
import Variables.Constants;

public class ImageRedesignerService {

    private Constants constants = new Constants();

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

    private Mat resizeMatrix = resizeCameraMatrix(CAMERA_MATRIX, constants.ResizedWidth, constants.ResizedHeight);

    public Point3D getRealPositions(Point2D imageCoordinates, int pixel, int positionXSize, int positionYSize)
    {
        Point3D coordinates3D = get3DCoordinates(imageCoordinates, pixel);
        return new Point3D((convertToPositionX(imageCoordinates.getX(), positionXSize) * coordinates3D.getX()), (convertToPositionY(imageCoordinates.getY(), positionYSize) * coordinates3D.getY()) , convertToPositionZ(coordinates3D.getZ(), 100));
    }

    public Point3D get3DCoordinates(Point2D imageCoordinates, int pixel) {

        double depth = convertDepth(pixel);

        MatOfPoint2f imageCoordinatesMat = new MatOfPoint2f(new Point(imageCoordinates.getX(), imageCoordinates.getY()));

        MatOfPoint2f undistortedCoordinatesMat = new MatOfPoint2f();
        Calib3d.undistortPoints(imageCoordinatesMat, undistortedCoordinatesMat, resizeMatrix, DISTORTION_COEFFS);

        Point[] undistortedPointsArray = undistortedCoordinatesMat.toArray();
        double[] undistortedCoordinates = {undistortedPointsArray[0].x, undistortedPointsArray[0].y};

        double[] coordinates3D = {imageCoordinates.getX(), undistortedCoordinates[1] * depth, depth};

        return new Point3D((float) (coordinates3D[0]), (float) (coordinates3D[1]) , (float) coordinates3D[2]);
    }

    public static Mat resizeCameraMatrix(Mat cameraMatrix, int newWidth, int newHeight) {
        int originalWidth = cameraMatrix.width();
        int originalHeight = cameraMatrix.height();

        double scaleX = (double) newWidth / originalWidth;
        double scaleY = (double) newHeight / originalHeight;

        Mat resizedMatrix = new Mat(3, 3, CvType.CV_64F);

        resizedMatrix.put(0, 0, cameraMatrix.get(0, 0)[0] * scaleX);
        resizedMatrix.put(0, 1, cameraMatrix.get(0, 1)[0] * scaleX);
        resizedMatrix.put(0, 2, cameraMatrix.get(0, 2)[0] * scaleX);
        resizedMatrix.put(1, 0, cameraMatrix.get(1, 0)[0] * scaleY);
        resizedMatrix.put(1, 1, cameraMatrix.get(1, 1)[0] * scaleY);
        resizedMatrix.put(1, 2, cameraMatrix.get(1, 2)[0] * scaleY);
        resizedMatrix.put(2, 0, cameraMatrix.get(2, 0)[0]);
        resizedMatrix.put(2, 1, cameraMatrix.get(2, 1)[0]);
        resizedMatrix.put(2, 2, cameraMatrix.get(2, 2)[0]);

        return resizedMatrix;
    }

    public static double convertDepth(int pixel) {
        int r = (pixel >> 16) & 0xFF;
        int g = (pixel >> 8) & 0xFF;
        int b = pixel & 0xFF;

        int blueR = 0;
        int blueG = 0;
        int blueB = 255;

        int redR = 255;
        int redG = 0;
        int redB = 0;

        double rNormalized = r / 255.0;
        double gNormalized = g / 255.0;
        double bNormalized = b / 255.0;

        double distanceBlue = Math.sqrt(Math.pow(rNormalized - blueR, 2) + Math.pow(gNormalized - blueG, 2) + Math.pow(bNormalized - blueB, 2));
        double distanceRed = Math.sqrt(Math.pow(rNormalized - redR, 2) + Math.pow(gNormalized - redG, 2) + Math.pow(bNormalized - redB, 2)) * 0.8;

        double minDepth = 0;
        double maxDepth = 1;

        double convertedDepth = ((distanceBlue / (distanceBlue + distanceRed)) * (maxDepth - minDepth)) + minDepth;

        return convertedDepth;
    }

    public static float convertToPositionX(float x, int imageWidth) {
        float x_cm = (x / (imageWidth)) * 200;
        return  x_cm;
    }

    public static float convertToPositionY(float y, int imageHeight) {
        float y_cm = (y / (imageHeight)) * 250;
        return y_cm;
    }

    public static float convertToPositionZ(float z, int size){
        return (float) Math.log10(z * 10 + 1);
    }
}