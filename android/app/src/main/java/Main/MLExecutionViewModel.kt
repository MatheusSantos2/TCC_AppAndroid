package Main

import Infraestructure.VehicleTrafficZone.TrajectoryEstimator
import Infraestructure.VehicleTrafficZone.TrajectoryValidator
import Interpreter.MLExecutors.DepthEstimationModelExecutor
import Interpreter.MLExecutors.SemanticSegmentationModelExecutor
import Interpreter.Models.ModelViewResult
import Utils.ImageHelper
import Utils.StringHelper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.graphics.Bitmap
import android.graphics.PointF
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "MLExecutionViewModel"

class MLExecutionViewModel : ViewModel()
{
  private lateinit var message: String
  private val viewModelJob = Job()
  private val viewModelScope = CoroutineScope(viewModelJob)
  private val _resultingBitmap = MutableLiveData<ModelViewResult>()
  val resultingBitmap: LiveData<ModelViewResult>
  get() = _resultingBitmap

  private var imageResult: Pair<Bitmap, MutableList<PointF>>? = null
    set(value) {
      field = value?.let { kotlin.Pair(value.first, value.second) }
    }

  fun onApplyModel(filePath: String, depthEstimationModel: DepthEstimationModelExecutor?,
                   semanticSegmentation: SemanticSegmentationModelExecutor?,
                   inferenceThread: ExecutorCoroutineDispatcher)
  {
    viewModelScope.launch(inferenceThread)
    {

      try
      {
        var bitmapsResult = getMLResult(filePath, semanticSegmentation!!, depthEstimationModel!!)

        fillImageResult(bitmapsResult.first, bitmapsResult.second, bitmapsResult.third)
        fillPointList()

        var result =  ModelViewResult(bitmapsResult.first, bitmapsResult.second, imageResult!!.first, ImageHelper.decodeBitmap(File(filePath)),message)
        _resultingBitmap.postValue(result)
      }
      catch (e: Exception) {
        Log.e(TAG, "Fail to execute ImageSegmentationModelExecutor: ${e.message}")
        var result =  ModelViewResult(ImageHelper.createEmptyBitmap(100, 100), ImageHelper.createEmptyBitmap(100, 100), ImageHelper.createEmptyBitmap(100, 100), ImageHelper.decodeBitmap(File(filePath)),"Fail to execute ImageSegmentationModelExecutor")
        _resultingBitmap.postValue(result)
      }
    }
  }

  private fun getMLResult(filePath: String, semanticSegmentation : SemanticSegmentationModelExecutor, depthEstimationModel : DepthEstimationModelExecutor):  Triple<Bitmap, Bitmap, Bitmap>
  {
    var contentImage = ImageHelper.decodeBitmap(File(filePath))
    var contentImage2 = ImageHelper.decodeBitmap(File(filePath))

    var semanticResult = semanticSegmentation?.execute(contentImage)
    var depthResult = depthEstimationModel?.execute(contentImage2)

    return Triple(depthResult.bitmapResult, semanticResult.bitmapResult, depthResult.bitmapOriginal)
  }

  private fun fillImageResult(depthBitmap : Bitmap, semanticBitmap : Bitmap, originalBitmap: Bitmap)
  {
    imageResult = Pair(ImageHelper.createEmptyBitmap(100, 100),mutableListOf())!!

    imageResult = if (!TrajectoryValidator().isTraversableInCenter(depthBitmap))
    {
      var imageResult2 = TrajectoryValidator()
              .processTraversablePixels(originalBitmap, semanticBitmap, depthBitmap)
      Pair(imageResult2.first, imageResult2.second.toMutableList())
    }
    else
    {
      var imageResult2 = TrajectoryEstimator()
              .getTraversableZone(semanticBitmap, depthBitmap, 0)
      Pair(imageResult2.first, imageResult2.second.toMutableList())
    }
  }

  private fun fillPointList()
  {
    if (imageResult!!.second.size != 0)
    {
      message = StringHelper().convertPointsToString(imageResult!!.second)
    }
    else
    {
      Log.w(TAG, "Fail in Trajectory Estimator Process")
    }
  }
}
