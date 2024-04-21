package Interpreter.OpenCV

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.applyColorMap
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage

class OpenCVHelper {

    fun depthEstimationVisualization(originalImage: Bitmap, resultImage: Bitmap): Bitmap
    {
        val originalTensorImage = TensorImage(DataType.FLOAT32)
        val resultTensorImage = TensorImage(DataType.FLOAT32)

        originalTensorImage.load(originalImage)
        resultTensorImage.load(resultImage)

        val outputMat = Mat(resultImage.height, resultImage.width, CvType.CV_8UC3)

        Imgproc.cvtColor(bitmapToMatGray(resultImage), outputMat, Imgproc.COLOR_GRAY2RGB)
        applyColorMap(outputMat, outputMat, Imgproc.COLORMAP_JET)

        val alpha = 0.02
        val beta = 1.0 - alpha
        val blendedMat = Mat(originalImage.height, originalImage.width, CvType.CV_8UC3)
        Core.addWeighted(bitmapToMat(originalImage), alpha, outputMat, beta, 0.0, blendedMat)

        return matToBitmap(blendedMat)!!
    }

    private fun matToBitmap(mat: Mat): Bitmap?
    {
        val bitmap: Bitmap
        val numChannels = mat.channels()

        if (numChannels == 1)
        {
            val grayMat = Mat(mat.height(), mat.width(), CvType.CV_8UC1)
            Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_GRAY2RGBA)
            bitmap = Bitmap.createBitmap(grayMat.width(), grayMat.height(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(grayMat, bitmap)
        }
        else if (numChannels == 3)
        {
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2RGBA)
            bitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(mat, bitmap)
        }
        else if (numChannels == 4)
        {
            bitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(mat, bitmap)
        } else return null

        return bitmap
    }

    fun bitmapToMat(bitmap: Bitmap): Mat
    {
        val mat = Mat(bitmap.height, bitmap.width, CvType.CV_8UC4)
        Utils.bitmapToMat(bitmap, mat)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2BGR)
        return mat
    }

    fun bitmapToMatGray(bitmap: Bitmap): Mat
    {
        val mat = Mat(bitmap.height, bitmap.width, CvType.CV_8UC4)
        Utils.bitmapToMat(bitmap, mat)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY)
        return mat
    }
}