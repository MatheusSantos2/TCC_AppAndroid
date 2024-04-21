package Infraestructure.VehicleTrafficZone;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

import Infraestructure.VehicleTrafficZone.Image.ImageRedesigner;
import Infraestructure.VehicleTrafficZone.Strategy.AStar;
import Infraestructure.VehicleTrafficZone.Strategy.RRT;
import Infraestructure.VehicleTrafficZone.Image.ImageGenerator;
import Infraestructure.VehicleTrafficZone.Strategy.RTTHelper;

import Models.Node;
import Models.Point2D;
import Models.Point3D;
import Variables.Constants;

public class TrajectoryEstimator
{
    private AStar aStar = new AStar();
    private ImageRedesigner imageRedesigner = new ImageRedesigner();
    private ImageGenerator imageGenerator = new ImageGenerator();
    private RRT rrt = new RRT();
    private RTTHelper rrtHelper = new RTTHelper();
    private Constants constants = new Constants();

    public Pair<Bitmap, List<PointF>> getTraversableZone(Bitmap originalMap, Bitmap depthMap)
    {
        //fazer a reprojeção da cena

        List<Point3D> coordinateList = generateListPointsTraversable(depthMap);

        Bitmap imageXZ = imageGenerator.createBitmapImageXZ(coordinateList, constants.LimitImageWidth, constants.LimitImageHeight);

        //Bitmap newImage2 = imageGenerator.mapColors(imageXZ, Color.WHITE);

        //imageGenerator.createMagentaStain(newImage2, 1, Color.MAGENTA, 2);

        Pair<List<PointF>, Bitmap> result = generateListTrajectory(imageXZ);

        return new Pair(result.second, result.first);
    }

    private List<Point3D> generateListPointsTraversable(Bitmap depthMap)
    {
        Bitmap resizedDepthMap = imageGenerator.resizeBitmap(depthMap, constants.ResizedWidth, constants.ResizedHeight);
        List<Point3D> coordinateList = new ArrayList<Point3D>();

        for (int y = 0; y < resizedDepthMap.getWidth(); y++) {
            for (int x = 0; x < resizedDepthMap.getHeight(); x++) {
                Point2D pointPixel = new Point2D(x, y);
                int depthPixel = resizedDepthMap.getPixel(x, y);
                Point3D realCoordinate = imageRedesigner.getRealPositions(pointPixel, depthPixel, constants.SceneWidth, constants.SceneHeight);
                coordinateList.add(realCoordinate);
            }
        }

        return coordinateList;
    }

    private Pair<List<PointF>, Bitmap> generateListTrajectory(Bitmap image){
        try
        {
            Node root = rrtHelper.getMidpointOfFirstWhiteLine(image);
            Node goal = rrtHelper.getMidpointOfFirstWhiteLineTop(image);

            Pair<List<Node>, Bitmap> pairRtt = rrt.findPath(image, root, goal, constants.Iterations);

            if (!pairRtt.first.isEmpty())
            {
                List<Node> first = pairRtt.first;

                first = aStar.findShortestPath(first);
                first = rrtHelper.getEquidistantNodes(first,constants.NumPoints);

                return new Pair<>(rrtHelper.convertNodesToPositions(first, constants.ImageWidth, constants.SceneWidth), pairRtt.second);
            }
        } catch (Exception e) {
            return new Pair<>(new ArrayList<PointF>(), imageGenerator.createEmptyImage(100, 100));
        }
        return null;
    }
}