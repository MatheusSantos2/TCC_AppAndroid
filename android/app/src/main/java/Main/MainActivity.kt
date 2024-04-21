package Main

import Infraestructure.DataAccess.DataExportHelper
import Infraestructure.DataAccess.ImageDriveHelper
import Infraestructure.DataAccess.MonitoringSqlLiteHelper
import Infraestructure.Senders.TcpIpClient
import Interpreter.MLExecutors.DepthEstimationModelExecutor
import Interpreter.MLExecutors.SemanticSegmentationModelExecutor
import Interpreter.Models.ModelViewResult
import Infraestructure.Camera.CameraFragment
import Utils.StringHelper
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.text.TextUtils
import android.util.Log
import android.view.animation.AnimationUtils
import android.view.animation.BounceInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import com.bumptech.glide.Glide
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.opencv.android.OpenCVLoader
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors


private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
private const val REQUEST_CODE_PERMISSIONS = 10
private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity(), CameraFragment.OnCaptureFinished
{
  private lateinit var imageDrive: ImageDriveHelper
  private lateinit var cameraFragment: CameraFragment
  private lateinit var viewModel: MLExecutionViewModel
  private lateinit var viewFinder: FrameLayout
  private lateinit var resultImageViewDepth: ImageView
  private lateinit var resultImageViewSegmentation: ImageView
  private lateinit var originalImageView: ImageView
  private lateinit var captureButton: ImageButton
  private lateinit var pauseButton: ImageButton
  private lateinit var exportButton: ImageButton

  private lateinit var captureHandlerThread: HandlerThread
  private lateinit var captureHandler: Handler
  private lateinit var messageHandler: Handler

  private var lastSavedFile = ""
  private val folderPath = "owlvision/images"

  private var depthEstimationExecutor: DepthEstimationModelExecutor? = null
  private var semanticSegmentationExecutor: SemanticSegmentationModelExecutor? = null
  private var inferenceThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
  private var tcpIpClient = TcpIpClient(this)

  private var lensFacing = CameraCharacteristics.LENS_FACING_FRONT
  private var isCapturing = false

  private lateinit var database: MonitoringSqlLiteHelper

  private var messageBuffer = mutableListOf<String>()
  private var timerRunnable = object : Runnable {
    override fun run() {
      saveMessage()
      messageBuffer.clear()
      messageHandler.postDelayed(this, 6000L)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?)
  {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.tfe_is_activity_main)
    supportActionBar?.setDisplayShowTitleEnabled(false)

    if (OpenCVLoader.initDebug()) Log.d(TAG, "OpenCV - Sucess")
    else Log.d(TAG, "OpenCV - Fail")

    viewFinder = findViewById(R.id.view_finder)
    resultImageViewDepth = findViewById(R.id.result_imageview_depth)
    resultImageViewSegmentation = findViewById(R.id.result_imageview_segmentation)
    originalImageView = findViewById(R.id.original_imageview)
    captureButton = findViewById(R.id.capture_button)
    pauseButton = findViewById(R.id.pause_button)
    exportButton = findViewById(R.id.export_button)

    if (allPermissionsGranted()) {
      addCameraFragment()
    }
    else {
      ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
    }

    database = MonitoringSqlLiteHelper(this)
    imageDrive = ImageDriveHelper(this, folderPath)

    viewModel = AndroidViewModelFactory(application).create(MLExecutionViewModel::class.java)
    viewModel.resultingBitmap.observe(
      this,
      Observer { resultImage ->
        if (resultImage != null) {
          updateUIWithResults(resultImage)

          if(!resultImage.message.isNullOrEmpty()) {
            saveImages(resultImage)
            sendMessage("Trajectory", resultImage.message)
          }
        }
      }
    )

    createModelExecutor()
    animateCameraButton()

    setupControls()
    startDataReceiver()

    messageHandler = Handler()
    messageHandler.postDelayed(timerRunnable, 6000L)

  }

  private fun setupControls()
  {
    captureButton.setOnClickListener {
      it.clearAnimation()
      if (!isCapturing) {
        startCaptureTimer()

      }

      pauseButton.setOnClickListener{
        it.clearAnimation()
        if (isCapturing) {
          stopCaptureTimer()
        }
      }
    }

    exportButton.setOnClickListener{
      it.clearAnimation()
      exportData()
    }

    findViewById<ImageButton>(R.id.toggle_button).setOnClickListener {
      lensFacing =
              if (lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                CameraCharacteristics.LENS_FACING_FRONT
              } else {
                CameraCharacteristics.LENS_FACING_BACK
              }
      cameraFragment.setFacingCamera(lensFacing)
      addCameraFragment()
    }
  }

  private fun allPermissionsGranted() =
          REQUIRED_PERMISSIONS.all {
            checkPermission(it, Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED
          }

  private fun addCameraFragment()
  {
    cameraFragment = CameraFragment.newInstance()
    cameraFragment.setFacingCamera(lensFacing)
    supportFragmentManager.popBackStack()
    supportFragmentManager.beginTransaction().replace(R.id.view_finder, cameraFragment).commit()
  }

  private fun updateUIWithResults(modelViewResult: ModelViewResult)
  {
    setImageView(resultImageViewDepth, modelViewResult.bitmapResult)
    setImageView(resultImageViewSegmentation, modelViewResult.bitmapResult2)
    setImageView(originalImageView, modelViewResult.bitmapOriginal)
  }

  private fun setImageView(imageView: ImageView, image: Bitmap)
  {
    Glide.with(baseContext).load(image).override(512, 512).fitCenter().into(imageView)
  }

  private fun createModelExecutor()
  {
    if (depthEstimationExecutor != null && semanticSegmentationExecutor != null)
    {
      depthEstimationExecutor!!.close()
      depthEstimationExecutor = null

      semanticSegmentationExecutor!!.close()
      semanticSegmentationExecutor = null
    }
    try
    {
      depthEstimationExecutor = DepthEstimationModelExecutor(this)
      semanticSegmentationExecutor = SemanticSegmentationModelExecutor(this)
    }
    catch (e: Exception)
    {
      Log.e(TAG, "Fail to create Executors depthEstimation and semanticSegmentation - Exception: ${e.message}")
    }
  }

  private fun animateCameraButton()
  {
    val animation = AnimationUtils.loadAnimation(this, R.anim.scale_anim)
    animation.interpolator = BounceInterpolator()
    captureButton.animation = animation
    captureButton.animation.start()
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray)
  {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == REQUEST_CODE_PERMISSIONS)
    {
      if (allPermissionsGranted())
      {
        addCameraFragment()
        viewFinder.post { setupControls() }
      }
      else
      {
        Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
        finish()
      }
    }
  }

  private fun enableControls(enable: Boolean)
  {
    captureButton.isEnabled = enable
    pauseButton.isEnabled = !enable
  }


  private fun startCaptureTimer()
  {
    isCapturing = true
    enableControls(false)

    captureHandlerThread = HandlerThread("CaptureThread").apply { start() }
    captureHandler = Handler(captureHandlerThread.looper)

    captureHandler.postDelayed({
      runBlocking {
        capturePhoto()
        delay(5000)
        if (isCapturing) {
          startCaptureTimer()
        }
      }
    }, 0)
  }

  private fun capturePhoto() {
    cameraFragment.takePicture()
  }

  private fun stopCaptureTimer()
  {
    isCapturing = false
    captureHandler.removeCallbacksAndMessages(null)
    captureHandlerThread.quitSafely()
    enableControls(true)
  }

  override fun onCaptureFinished(file: File)
  {
    val msg = "Photo capture succeeded: ${file.absolutePath}"
    Log.d(TAG, msg)

    lastSavedFile = file.absolutePath
    viewModel.onApplyModel(file.absolutePath, depthEstimationExecutor, semanticSegmentationExecutor, inferenceThread)
  }

  override fun onResume() {
    super.onResume()
  }

  override fun onPause() {
    super.onPause()
  }

  private fun startDataReceiver()
  {
    val thread = Thread {
      try
      {
        if(!tcpIpClient.hasConnected())
          tcpIpClient.connect()

        var message :String
        while (true)
        {
          if(tcpIpClient.hasConnected())
          {
            message = tcpIpClient.receiveMessage()
            if(!TextUtils.isEmpty(message))
              messageBuffer.add(message)
          }
        }
      } catch (e: IOException) {
        e.printStackTrace()
      }
    }

    thread.start()
  }

  private fun saveMessage()
  {
    val copyBuffer = messageBuffer.toList()
    var count = 0
    for (message in copyBuffer)
    {
      database.insert(message)

      if(count > 100)
        break
      count++
    }
  }

  private fun sendMessage(label:String, message: String){
    val thread = Thread {

      if(!tcpIpClient.hasConnected())
        tcpIpClient.connect()

      try
      {
        if(tcpIpClient.hasConnected()){
          tcpIpClient.sendMessage(label, message)
        }
      }
      catch (e: IOException){
        Log.e(TAG, "Fail to send message - Exception: ${e.message}")
      }
    }
    thread.start()
  }

  private fun saveImages(modelViewResult: ModelViewResult)
  {
    val images = arrayOf(modelViewResult.bitmapResult, modelViewResult.bitmapResult2, modelViewResult.bitmapOriginal, modelViewResult.bitmapRRT)
    var count = 0

    for (image in images) {
      imageDrive.saveBitmap(image, count)
      count += 1
    }
  }

  private fun exportData() {
    DataExportHelper(this, folderPath).export()
  }
}
