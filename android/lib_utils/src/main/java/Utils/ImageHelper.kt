package Utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.File
import java.nio.FloatBuffer

class ImageHelper
{
  companion object
  {
    private fun decodeExifOrientation(orientation: Int): Matrix {
      val matrix = Matrix()

      when (orientation) {
        ExifInterface.ORIENTATION_NORMAL, ExifInterface.ORIENTATION_UNDEFINED -> Unit
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90F)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180F)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270F)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1F, 1F)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1F, -1F)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
          matrix.postScale(-1F, 1F)
          matrix.postRotate(270F)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
          matrix.postScale(-1F, 1F)
          matrix.postRotate(90F)
        }

        else -> throw IllegalArgumentException("Invalid orientation: $orientation")
      }

      return matrix
    }

    fun setExifOrientation(filePath: String, value: String)
    {
      val exif = ExifInterface(filePath)
      exif.setAttribute(
        ExifInterface.TAG_ORIENTATION, value
      )
      exif.saveAttributes()
    }

    fun computeExifOrientation(rotationDegrees: Int, mirrored: Boolean) = when {
      rotationDegrees == 0 && !mirrored -> ExifInterface.ORIENTATION_NORMAL
      rotationDegrees == 0 && mirrored -> ExifInterface.ORIENTATION_FLIP_HORIZONTAL
      rotationDegrees == 180 && !mirrored -> ExifInterface.ORIENTATION_ROTATE_180
      rotationDegrees == 180 && mirrored -> ExifInterface.ORIENTATION_FLIP_VERTICAL
      rotationDegrees == 270 && mirrored -> ExifInterface.ORIENTATION_TRANSVERSE
      rotationDegrees == 90 && !mirrored -> ExifInterface.ORIENTATION_ROTATE_90
      rotationDegrees == 90 && mirrored -> ExifInterface.ORIENTATION_TRANSPOSE
      rotationDegrees == 270 && mirrored -> ExifInterface.ORIENTATION_ROTATE_270
      rotationDegrees == 270 && !mirrored -> ExifInterface.ORIENTATION_TRANSVERSE
      else -> ExifInterface.ORIENTATION_UNDEFINED
    }

    fun decodeBitmap(file: File): Bitmap
    {
      val exif = ExifInterface(file.absolutePath)
      val transformation =
        decodeExifOrientation(
          exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90
          )
        )

      val options = BitmapFactory.Options()
      val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
      return Bitmap.createBitmap(
        BitmapFactory.decodeFile(file.absolutePath),
        0, 0, bitmap.width, bitmap.height, transformation, true
      )
    }

    fun scaleBitmapAndKeepRatio(targetBmp: Bitmap, reqHeightInPixels: Int, reqWidthInPixels: Int): Bitmap
    {
      if (targetBmp.height == reqHeightInPixels && targetBmp.width == reqWidthInPixels)
      {
        return targetBmp
      }

      return changeBitmapSize(targetBmp, reqWidthInPixels, reqHeightInPixels)
    }

    fun createEmptyBitmap(imageWidth: Int, imageHeigth: Int, color: Int = 0): Bitmap
    {
      val ret = Bitmap.createBitmap(imageWidth, imageHeigth, Bitmap.Config.RGB_565)
      if (color != 0)
      {
        ret.eraseColor(color)
      }
      return ret
    }

    private fun changeBitmapSize(bm: Bitmap, newWidth: Int, newHeight: Int ): Bitmap
    {
      val width = bm.width;
      val height = bm.height;
      val scaleWidth = newWidth.toFloat() / width;
      val scaleHeight = newHeight.toFloat() / height;
      val matrix = Matrix();
      matrix.postScale(scaleWidth, scaleHeight);
      val resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
      bm.recycle();
      return resizedBitmap;
    }

    fun convertFloatBufferToBitmapGrayScale(floatBuffer: FloatBuffer, width: Int, height: Int): Bitmap
    {
      floatBuffer?.rewind()
      val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
      val pixels = IntArray(width * height)

      for (i in 0 until width * height)
      {
        val a = 0xFF
        val r: Float = floatBuffer?.get(i)!! * 255.0f
        val g: Float = floatBuffer?.get(i)!! * 255.0f
        val b: Float = floatBuffer?.get(i)!! * 255.0f
        floatBuffer?.rewind()

        pixels[i] = a shl 24 or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
      }
      bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
      return bitmap
    }

    fun bitmapToArray(bitmap: Bitmap): FloatArray {
      val width = bitmap.width
      val height = bitmap.height

      val pixels = IntArray(width * height)

      bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

      val pixelValues = FloatArray(width * height * 3)

      for (i in pixels.indices) {
        val pixel = pixels[i]

        val red = ((pixel shr 16) and 0xff) / 255.0f
        val green = ((pixel shr 8) and 0xff) / 255.0f
        val blue = (pixel and 0xff) / 255.0f

        pixelValues[i * 3] = red
        pixelValues[i * 3 + 1] = green
        pixelValues[i * 3 + 2] = blue
      }

      return pixelValues
    }

    fun matToBitmap(mat: Mat): Bitmap?
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

    @JvmStatic
    fun increaseResolution(bitmap: Bitmap, scaleFactor: Double): Bitmap? {
      // Converter Bitmap para Mat
      val src = Mat(bitmap.height, bitmap.width, CvType.CV_8UC4)
      Utils.bitmapToMat(bitmap, src)

      val newWidth = (bitmap.width * scaleFactor).toInt()
      val newHeight = (bitmap.height * scaleFactor).toInt()

      val dst = Mat(newHeight, newWidth, src.type())
      Imgproc.resize(src, dst, Size(newWidth.toDouble(), newHeight.toDouble()), 0.0, 0.0, Imgproc.INTER_CUBIC)

      // Converter Mat de volta para Bitmap
      val resizedBitmap = Bitmap.createBitmap(newWidth, newHeight, bitmap.config)
      Utils.matToBitmap(dst, resizedBitmap)
      return resizedBitmap
    }
  }
}
