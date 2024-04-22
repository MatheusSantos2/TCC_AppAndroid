package Infraestructure.VehicleTrafficZone.Image;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;

import java.util.ArrayList;

public class SceneRedesigner {
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

    public static final MatOfDouble DISTORTION_COEFFS = new MatOfDouble(1, 5, CvType.CV_64F) {{
        put(0, 0, 0.56239355);
        put(0, 1, 0.25599173);
        put(0, 2, 0.16078187);
        put(0, 3, 0.01695844);
        put(0, 4, 0.17199612);
    }};

    public static ArrayList<Point3> calculate3DCoordinates(Mat depthMap) {
        ArrayList<Point3> validCoordinates = new ArrayList<>();

        int imageHeight = depthMap.rows();
        int imageWidth = depthMap.cols();

        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                double[] depthArray = depthMap.get(y, x);
                double depth = depthArray[0];

                if (depth >= 0 && depth <= 1) {
                    Point imageCoordinates = new Point(x, y);
                    Point3 coordinates3D = calculate3DCoordinates(imageCoordinates, depth);
                    validCoordinates.add(coordinates3D);
                }
            }
        }

        return validCoordinates;
    }

    private static Point3 calculate3DCoordinates(Point imageCoordinates, double depth) {
        MatOfPoint2f imageCoordinatesMat = new MatOfPoint2f(imageCoordinates);
        MatOfPoint3f objectCoordinatesMat = new MatOfPoint3f(new Point3(imageCoordinates.x, imageCoordinates.y, depth));

        Mat rvecs = new Mat();
        Mat tvecs = new Mat();
        Calib3d.solvePnP(objectCoordinatesMat, imageCoordinatesMat, CAMERA_MATRIX, DISTORTION_COEFFS, rvecs, tvecs);

        double[] tvecsArray = tvecs.get(0, 0);
        return new Point3(tvecsArray[0], tvecsArray[1], tvecsArray[2]);
    }
}
