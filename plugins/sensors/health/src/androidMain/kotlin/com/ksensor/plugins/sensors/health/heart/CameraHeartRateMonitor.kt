package com.ksensor.plugins.sensors.health.heart

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.ksensor.core.context.KSensorContext
import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.SensorData
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class CameraHeartRateMonitor(private val context: Context) : HeartRateMonitor {
    private var cameraExecutor: ExecutorService? = null
    private val ppgAlgorithm = PPGAlgorithm(targetSamplingRate = 30.0)
    private var lastBpm = 0f
    
    private val isRunning = AtomicBoolean(false)

    override fun isSupported(): Boolean {
        return try {
            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
            cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
        } catch (e: Exception) {
            false
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun start(): Flow<KSensorResponse<SensorData.HeartRate>> = callbackFlow {
        if (isRunning.getAndSet(true)) {
            close()
            return@callbackFlow
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(Size(640, 480))
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor!!) { imageProxy ->
                    processImage(imageProxy, this@callbackFlow)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                val lifecycleOwner = KSensorContext.getActivity() as? LifecycleOwner
                
                if (lifecycleOwner == null) {
                    close(IllegalStateException("No LifecycleOwner (Activity) found to bind camera"))
                    return@addListener
                }

                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner, 
                    cameraSelector, 
                    imageAnalysis
                )
                
                camera.cameraControl.enableTorch(true)
                
            } catch (e: Exception) {
                Log.e("CameraMonitor", "Use case binding failed", e)
                close(e)
            }
        }, ContextCompat.getMainExecutor(context))

        awaitClose {
            stop()
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImage(image: ImageProxy, scope: ProducerScope<KSensorResponse<SensorData.HeartRate>>) {
        try {
            val planes = image.image?.planes ?: return
            val yBuffer = planes[0].buffer
            val vBuffer = planes[2].buffer
            
            val width = image.width
            val height = image.height
            
            // ROI: Central 20%
            val roiSize = 0.2
            val startX = (width * (0.5 - roiSize / 2)).toInt()
            val startY = (height * (0.5 - roiSize / 2)).toInt()
            val endX = (width * (0.5 + roiSize / 2)).toInt()
            val endY = (height * (0.5 + roiSize / 2)).toInt()
            
            var redSum = 0.0
            var count = 0
            
            val yRowStride = planes[0].rowStride
            val uvRowStride = planes[2].rowStride
            val uvPixelStride = planes[2].pixelStride
            
            for (y in startY until endY step 2) {
                for (x in startX until endX step 2) {
                    val yIndex = y * yRowStride + x
                    val uvIndex = (y / 2) * uvRowStride + (x / 2) * uvPixelStride
                    
                    if (yIndex < yBuffer.remaining() && uvIndex < vBuffer.remaining()) {
                        val yVal = yBuffer.get(yIndex).toInt() and 0xFF
                        val vVal = vBuffer.get(uvIndex).toInt() and 0xFF
                        
                        // R = Y + 1.402 * (V - 128)
                        val r = yVal + 1.402 * (vVal - 128)
                        redSum += r
                        count++
                    }
                }
            }
            
            if (count > 0) {
                val avgRed = redSum / count
                val result = ppgAlgorithm.processSample(avgRed, System.currentTimeMillis())
                
                if (result != null) {
                    // Always use the algorithm's smoothed BPM (which now persists)
                    // only emit 0 if no finger is detected
                    val currentBpm = if (result.fingerDetected) result.bpm else 0f
                    
                    scope.trySend(KSensorResponse(SensorData.HeartRate(
                        heartRate = currentBpm,
                        source = SensorData.HeartRateSource.CAMERA_PPG,
                        confidence = result.confidence,
                        quality = result.quality,
                        timestamp = System.currentTimeMillis()
                    )))
                }
            }
        } catch (e: Exception) {
            Log.e("CameraMonitor", "Error processing image", e)
        } finally {
            image.close()
        }
    }

    private fun stop() {
        isRunning.set(false)
        cameraExecutor?.shutdown()
        cameraExecutor = null
        
        ContextCompat.getMainExecutor(context).execute {
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                Log.e("CameraMonitor", "Error unbinding camera", e)
            }
        }
    }
}
