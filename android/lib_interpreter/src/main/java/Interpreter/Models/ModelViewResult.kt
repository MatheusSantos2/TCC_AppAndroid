package Interpreter.Models

import android.graphics.Bitmap

data class ModelViewResult(
  val bitmapResult: Bitmap,
  val bitmapResult2: Bitmap,
  val bitmapOriginal: Bitmap,
  val bitmapRRT: Bitmap,
  val message: String
)

