package Infraestructure.Services.StrategyServices;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.util.Pair;

import org.opencv.core.Point3;

import java.util.ArrayList;
import java.util.List;

import Infraestructure.Services.ImageServices.ImageRedesignerService;
import Infraestructure.Services.ImageServices.SceneRedesignService;
import Variables.Constants;

public class TrajectoryEstimatorService
{
    private ImageRedesignerService imageRedesigner = new ImageRedesignerService();
    private Constants constants = new Constants();

    public Pair<List<PointF>, Bitmap> getTraversableZone(Bitmap originalMap, Bitmap depthMap)
    {
        ArrayList<Point3> scene = SceneRedesignService.calculate3DCoordinates(depthMap);
        Bitmap topView = new SceneRedesignService().generateTopViewBitmap(scene, depthMap);

        Pair<List<PointF>, Bitmap> result =  TrajectoryGeneratorService.drawCirclesOnBitmap(topView);

        return new Pair(result.second, result.first);
    }
}