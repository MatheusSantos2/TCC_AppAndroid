package Infraestructure.Services.StrategyServices;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.util.Pair;

import org.opencv.core.Point3;

import java.util.List;

import Infraestructure.Services.ImageServices.ImageRedesignerService;
import Infraestructure.Services.ImageServices.SceneRedesignService;
import Variables.Constants;

public class TrajectoryGeneratorService
{
    private ImageRedesignerService imageRedesigner = new ImageRedesignerService();
    private Constants constants = new Constants();

    public Pair<List<PointF>, Bitmap> getTraversableZone(Bitmap originalMap, Bitmap depthMap)
    {
        List<Point3> listCoor = SceneRedesignService.processDepthMap(depthMap);
        printPoint3List(listCoor);

        Bitmap topView = SceneRedesignService.generateTopViewBitmap(listCoor, depthMap);

        Pair<List<PointF>, Bitmap> result =  TrajectoryGeneratorHelper.drawCirclesOnBitmap(topView);

        return new Pair(result.second, result.first);
    }

    public void printPoint3List(List<Point3> point3List) {
        for (Point3 point3 : point3List) {
            System.out.println("Point3: (" + point3.x + ", " + point3.y + ", " + point3.z + ")");
        }
    }
}