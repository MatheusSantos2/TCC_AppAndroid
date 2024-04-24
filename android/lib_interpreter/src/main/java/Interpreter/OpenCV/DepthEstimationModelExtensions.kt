package Interpreter.OpenCV

import Utils.ImageHelper
import android.graphics.Bitmap
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.applyColorMap
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage

class DepthEstimationModelExtensions {

    fun depthEstimationVisualization(originalImage: Bitmap, resultImage: Bitmap): Pair<Bitmap, Mat>
    {
        val originalTensorImage = TensorImage(DataType.FLOAT32)
        val resultTensorImage = TensorImage(DataType.FLOAT32)

        originalTensorImage.load(originalImage)
        resultTensorImage.load(resultImage)

        val outputMat = Mat(resultImage.height, resultImage.width, CvType.CV_8UC3)

        Imgproc.cvtColor(ImageHelper.bitmapToMatGray(resultImage), outputMat, Imgproc.COLOR_GRAY2RGB)
        applyColorMap(outputMat, outputMat, Imgproc.COLORMAP_JET)

        val alpha = 0.02
        val beta = 1.0 - alpha
        val blendedMat = Mat(originalImage.height, originalImage.width, CvType.CV_8UC3)
        Core.addWeighted(ImageHelper.bitmapToMat(originalImage), alpha, outputMat, beta, 0.0, blendedMat)

        return Pair(ImageHelper.matToBitmap(blendedMat)!!, blendedMat)
    }
}