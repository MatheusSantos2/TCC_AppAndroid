package Interpreter.Models

import android.graphics.Bitmap
import org.opencv.core.Mat

data class ModelExecutionResult(
  val bitmapResult: Bitmap,
  val bitmapOriginal: Bitmap,
  val blendMat: Mat
)
