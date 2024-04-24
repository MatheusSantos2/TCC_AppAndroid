package Main

import Infraestructure.Services.TrajectoryEstimatorService
import Interpreter.MLExecutors.DepthEstimationModelExecutor
import Interpreter.Models.ModelViewResult
import Utils.ImageHelper
import Utils.StringHelper
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.opencv.core.Mat
import java.io.File

private const val TAG = "MLExecutionViewModel"

class MLExecutionViewModel : ViewModel()
{
  private val viewModelJob = Job()
  private val viewModelScope = CoroutineScope(viewModelJob)
  private val _resultingBitmap = MutableLiveData<ModelViewResult>()

  val resultingBitmap: LiveData<ModelViewResult>
    get() = _resultingBitmap

  fun onApplyModel(filePath: String, depthEstimationModel: DepthEstimationModelExecutor?,
                   inferenceThread: ExecutorCoroutineDispatcher)
  {
    viewModelScope.launch(inferenceThread)
    {
      try
      {
        var bitmapsMlResult = getMLResult(filePath, depthEstimationModel!!)

        var resultTransversalZone = TrajectoryEstimatorService()
                                    .getTraversableZone(bitmapsMlResult.first, bitmapsMlResult.second, bitmapsMlResult.third)

        var resultModelView =  ModelViewResult(
                                  bitmapsMlResult.first,
                                  bitmapsMlResult.second,
                                  resultTransversalZone!!.first,
                                  ImageHelper.decodeBitmap(File(filePath)),
                                  StringHelper().convertPointsToString(resultTransversalZone!!.second))

        _resultingBitmap.postValue(resultModelView)
      }
      catch (e: Exception)
      {
        Log.e(TAG, "Fail to execute MLExecutionViewModel: ${e.message}")
        var resultModelView =  ModelViewResult(ImageHelper.createEmptyBitmap(100, 100), ImageHelper.createEmptyBitmap(100, 100), ImageHelper.createEmptyBitmap(100, 100), ImageHelper.decodeBitmap(File(filePath)),"Fail to execute ImageSegmentationModelExecutor")

        _resultingBitmap.postValue(resultModelView)
      }
    }
  }

  private fun getMLResult(filePath: String, depthEstimationModel : DepthEstimationModelExecutor):  Triple<Bitmap, Bitmap, Mat>
  {
    var contentImage = ImageHelper.decodeBitmap(File(filePath))

    var depthResult = depthEstimationModel?.execute(contentImage)

    return Triple(depthResult.bitmapOriginal, depthResult.bitmapResult, depthResult.blendMat)
  }
}
