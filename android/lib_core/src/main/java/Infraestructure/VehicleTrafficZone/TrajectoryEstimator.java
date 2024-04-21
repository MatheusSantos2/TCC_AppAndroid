package Infraestructure.VehicleTrafficZone;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

import Models.Node;
import Models.Point2D;
import Models.Point3D;
import Utils.ImageGenerator;
import Utils.RTTHelper;
import Utils.SegmentColors;

public class TrajectoryEstimator extends SegmentColors {
    protected Bitmap semanticMap;
    protected Bitmap depthMap;
    private double stepSize = 1.0;
    private double goalThreshold = 5.0;
    private double vehicleWidth = 15F;
    private double vehicleHeight = 15F;
    private RTTHelper rrtHelper = new RTTHelper();
    private AStar aStar = new AStar();

    private Pair imageSize = rrtHelper.getVehicleSize(192, 200, 1000, 1000, vehicleHeight, vehicleWidth);

    private DistanceEstimator distanceEstimator = new DistanceEstimator();
    private ImageGenerator imageGenerator = new ImageGenerator();
    private RRT rrt = new RRT(stepSize, goalThreshold, (Double) imageSize.first, (Double) imageSize.second);

    private int roadColor = getColorForLabel("Road");
    private boolean isTraversable = true;
    private int resizedWidth = 0;
    private List<Pair<Boolean, Point3D>> coordinateDictionary;

    public Pair<Bitmap, List<PointF>> getTraversableZone(Bitmap semanticMap, Bitmap depthMap, int region) {
        this.semanticMap = semanticMap;
        this.depthMap = depthMap;

        generateListPointsTraversable(region);

        Bitmap newImage = imageGenerator.createBitmapImageXZ(coordinateDictionary, resizedWidth, 200);

        Bitmap newImage2 = imageGenerator.mapColors(newImage, Color.WHITE);

        if(region == 1){
            newImage2 = imageGenerator.getPartialImage(newImage2, true);
        }else if(region == 2){
            newImage2 =  imageGenerator.getPartialImage(newImage2, false);
        }else{
            imageGenerator.createMagentaStain(newImage2, 1, Color.MAGENTA, 2);
        }

        Pair<List<PointF>, Bitmap> result = generateListTrajectory(newImage2, coordinateDictionary);

        return new Pair(result.second, result.first);
    }

    private void generateListPointsTraversable(int region) {
        Bitmap resizedSemantic = imageGenerator.resizeBitmap(semanticMap, 192, 640);
        Bitmap resizedDepth = imageGenerator.resizeBitmap(depthMap, 192, 640);

        resizedWidth = resizedSemantic.getWidth();
        int height = resizedSemantic.getHeight();

        Pair<Boolean, Point3D> pairCoordinates;
        coordinateDictionary = new ArrayList<>();

        int start;
        int end;

        if (region == 2) {
            start = 0;
            end = resizedWidth / 2;
        } else if (region == 1) {
            start = resizedWidth / 2;
            end = resizedWidth;
        } else {
            start = 0;
            end = resizedWidth;
        }

        for (int y = 0; y < height; y++) {
            for (int x = start; x < end; x++) {

                Point2D pointPixel = new Point2D(x, y);
                int semanticPixel = resizedSemantic.getPixel(x, y);
                int depthPixel = resizedDepth.getPixel(x, y);
                Point3D realCoordinates = distanceEstimator.getRealPositions(pointPixel, depthPixel, resizedWidth, height);

                if (semanticPixel == roadColor) {
                    pairCoordinates = new Pair<>(isTraversable, realCoordinates);
                } else {
                    pairCoordinates = new Pair(!isTraversable, realCoordinates);
                }

                coordinateDictionary.add(pairCoordinates);
            }
        }
    }

    private Pair<List<PointF>, Bitmap> generateListTrajectory(Bitmap image, List<Pair<Boolean, Point3D>> coordinateDictionary){
        try {
            Node root = rrtHelper.getMidpointOfFirstWhiteLine(image);
            Node goal = rrtHelper.getMidpointOfFirstWhiteLineTop(image);

            Pair<List<Node>, Bitmap> pairRtt = rrt.findPath(image, root, goal,8000);

            if (!pairRtt.first.isEmpty())
            {
                List<Node> first = pairRtt.first;

                first = aStar.findShortestPath(first);
                first = rrtHelper.getEquidistantNodes(first,5);

                if(!first.isEmpty()) {
                    return new Pair<>(rrtHelper.convertNodesToPositions(first, coordinateDictionary, resizedWidth, 100), pairRtt.second);
                }
                else{
                    return new Pair<>(new ArrayList<PointF>(), imageGenerator.createEmptyImage(100, 100));
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}