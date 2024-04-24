package Infraestructure.Services;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.util.Pair;

import org.opencv.core.Mat;
import org.opencv.core.Point3;

import java.util.ArrayList;
import java.util.List;

import Infraestructure.Services.ImageServices.ImageRedesignerService;
import Infraestructure.Services.ImageServices.SceneRedesignService;
import Infraestructure.Services.StrategyServices.AStar;
import Infraestructure.Services.StrategyServices.RRT;
import Infraestructure.Services.StrategyServices.RTTHelper;
import Models.Node;
import Utils.ImageHelper;
import Variables.Constants;

public class TrajectoryEstimatorService
{
    private AStar aStar = new AStar();
    private ImageRedesignerService imageRedesigner = new ImageRedesignerService();
    private RRT rrt = new RRT();
    private RTTHelper rrtHelper = new RTTHelper();
    private Constants constants = new Constants();

    public Pair<Bitmap, List<PointF>> getTraversableZone(Bitmap originalMap, Bitmap depthMap, Mat depthMat)
    {
        ArrayList<Point3> scene = SceneRedesignService.calculate3DCoordinates(depthMat);
        Bitmap topView = new SceneRedesignService().generateTopViewBitmap(scene, depthMat);

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
            return new Pair<>(new ArrayList<PointF>(), ImageHelper.Companion.createEmptyBitmap(100, 100, Color.WHITE));
        }
        return null;
    }
}