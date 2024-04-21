package Interpreter.MLExecutors

import Interpreter.Models.ModelExecutionResult
import Interpreter.OpenCV.OpenCVHelper
import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import java.io.FileInputStream
import java.io.IOException
import java.nio.channels.FileChannel
import org.tensorflow.lite.Interpreter
import Utils.ImageHelper
import java.nio.*

class DepthEstimationModelExecutor(context: Context)
{
  private var inputData: FloatBuffer
  private var fullTimeExecutionTime = 0L
  private var numberThreads = 4
  private var outputData: FloatBuffer
  private var interpreter: Interpreter

  companion object
  {
    const val TAG = "DepthInterpreter"
    private const val depthEstimationModel = "model_depth.tflite"
    private const val imageInputSizeWidth = 640
    private const val imageInputSizeHeight = 192
    private const val imageOutputSizeWidth = 160
    private const val imageOutputSizeHeight = 48
    private const val imageWidthSizeDefault = 128
    private const val imageHeightSizeDefault = 96
  }

  init
  {
    interpreter = getInterpreter(context, depthEstimationModel)

    outputData = ByteBuffer.allocateDirect(1 * 160 * 48 * 1 * 4).apply {
      order(ByteOrder.nativeOrder())
    }.asFloatBuffer()

    inputData = ByteBuffer.allocateDirect(1 * 192 * 640 * 3 * 4).apply {
      order(ByteOrder.nativeOrder())
    }.asFloatBuffer()
  }

  @Throws(IOException::class)
  private fun getInterpreter(context: Context, modelName: String, useGpu: Boolean = false): Interpreter
  {
      try
      {
          val tfliteOptions = Interpreter.Options()
          tfliteOptions.setNumThreads(numberThreads)

          return Interpreter(loadModelFile(context, modelName), tfliteOptions)
      }
      catch (e: Exception)
      {
          Log.e(TAG, "Fail to create Interpreter: ${e.message}")
          throw e
      }
  }

  @Throws(IOException::class)
  private fun loadModelFile(context: Context, modelFile: String): MappedByteBuffer
  {
    val fileDescriptor = context.assets.openFd(modelFile)
    val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
    val fileChannel = inputStream.channel
    val startOffset = fileDescriptor.startOffset
    val declaredLength = fileDescriptor.declaredLength
    val retFile = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    fileDescriptor.close()
    return retFile
  }

  fun close()
  {
    interpreter.close()
  }

  fun execute(data: Bitmap): ModelExecutionResult
  {
    try
    {
      fullTimeExecutionTime = SystemClock.uptimeMillis()
      val originalBitmap = ImageHelper.scaleBitmapAndKeepRatio(data, imageInputSizeHeight, imageInputSizeWidth)
      val inputArray = ImageHelper.bitmapToArray(originalBitmap)

      inputData.rewind()
      inputData.put(inputArray)

      interpreter.run(inputData, outputData)

      val outputBitmap = ImageHelper.convertFloatBufferToBitmapGrayScale(outputData, imageOutputSizeWidth, imageOutputSizeHeight)
      val outputBitmapResized = ImageHelper.scaleBitmapAndKeepRatio(outputBitmap, imageHeightSizeDefault, imageWidthSizeDefault)
      val originalBitmapResized = ImageHelper.scaleBitmapAndKeepRatio(originalBitmap, imageHeightSizeDefault, imageWidthSizeDefault)

      val output = OpenCVHelper().depthEstimationVisualization(originalBitmapResized, outputBitmapResized)
      fullTimeExecutionTime = SystemClock.uptimeMillis() - fullTimeExecutionTime
      Log.d(TAG, "Total time execution $fullTimeExecutionTime")

      return ModelExecutionResult(output, originalBitmapResized)
    }
    catch (e: Exception)
    {
      val exceptionLog = "something went wrong: ${e.message}"
      Log.d(TAG, exceptionLog)

      val emptyBitmap = ImageHelper.createEmptyBitmap(imageInputSizeWidth, imageInputSizeHeight)
      return ModelExecutionResult(emptyBitmap, emptyBitmap)
    }
  }
}
