package Utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.nio.FloatBuffer

abstract class ImageHelper
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

    fun convertBitmapToFloatBuffer(bitmapIn: Bitmap, width: Int, height: Int, mean: Float = 0.0f, std: Float = 255.0f): FloatBuffer
    {
      val inputImage = FloatBuffer.allocate(1 * width * height * 3)
      inputImage.order()
      inputImage.rewind()

      val intValues = IntArray(width * height)
      bitmapIn.getPixels(intValues, 0, width, 0, 0, width, height)
      var pixel = 0
      for (y in 0 until height) {
        for (x in 0 until width) {
          val value = intValues[pixel++]

          inputImage.put(((value shr 16 and 0xFF) - mean) / std)
          inputImage.put(((value shr 8 and 0xFF) - mean) / std)
          inputImage.put(((value and 0xFF) - mean) / std)
        }
      }

      inputImage.rewind()
      return inputImage
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

    fun convertFloatBufferToBitmapRGB(floatBuffer: FloatBuffer, width: Int, height: Int): Bitmap
    {
      floatBuffer?.rewind()
      val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
      val pixels = IntArray(width * height)

      for (i in 0 until width * height)
      {
        val a = 0xFF
        val r: Float = floatBuffer?.get(i)!! * 31.0f
        val g: Float = floatBuffer?.get(i)!! * 63.0f
        val b: Float = floatBuffer?.get(i)!! * 31.0f
        floatBuffer?.rewind()

        pixels[i] = a shl 24 or (r.toInt() and 0x1F shl 11) or (g.toInt() and 0x3F shl 5) or (b.toInt() and 0x1F)
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
  }
}
