package Infraestructure.Services;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.util.Pair;

import org.opencv.core.Mat;
import org.opencv.core.Point3;

import java.util.ArrayList;
import java.util.List;

import Infraestructure.Services.Image.ImageGenerator;
import Infraestructure.Services.Image.ImageRedesigner;
import Infraestructure.Services.Image.SceneRedesign;
import Infraestructure.Services.Strategy.AStar;
import Infraestructure.Services.Strategy.RRT;
import Infraestructure.Services.Strategy.RTTHelper;
import Models.Node;
import Variables.Constants;

public class TrajectoryEstimator
{
    private AStar aStar = new AStar();
    private ImageRedesigner imageRedesigner = new ImageRedesigner();
    private ImageGenerator imageGenerator = new ImageGenerator();
    private RRT rrt = new RRT();
    private RTTHelper rrtHelper = new RTTHelper();
    private Constants constants = new Constants();

    public Pair<Bitmap, List<PointF>> getTraversableZone(Bitmap originalMap, Bitmap depthMap, Mat depthMat)
    {
        ArrayList<Point3> scene = SceneRedesign.calculate3DCoordinates(depthMat);
        Bitmap topView = new SceneRedesign().generateTopViewBitmap(scene, depthMat);

        Pair<List<PointF>, Bitmap> result = generateListTrajectory(topView);

        return new Pair(result.second, result.first);
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