package com.ksensor.plugins.sensors.health.heart

import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.SensorData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.AVFoundation.*
import platform.CoreMedia.*
import platform.CoreVideo.*
import platform.Foundation.*
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue
import kotlinx.cinterop.*
import kotlinx.coroutines.channels.ProducerScope

@OptIn(ExperimentalForeignApi::class)
class IosCameraHeartRateMonitor : IosHeartRateMonitor {
    private val ppgAlgorithm = PPGAlgorithm(targetSamplingRate = 30.0)
    private var session: AVCaptureSession? = null
    
    private val delegate = object : NSObject(), AVCaptureVideoDataOutputSampleBufferDelegateProtocol {
        @OptIn(UnsafeNumber::class)
        override fun captureOutput(
            output: AVCaptureOutput,
            didOutputSampleBuffer: CMSampleBufferRef?,
            fromConnection: AVCaptureConnection
        ) {
            processFrame(didOutputSampleBuffer)
        }
    }

    private var callbackScope: ProducerScope<KSensorResponse<SensorData.HeartRate>>? = null

    override fun isSupported(): Boolean {
        return AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) != null
    }

    override fun start(): Flow<KSensorResponse<SensorData.HeartRate>> = callbackFlow {
        callbackScope = this
        
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
        if (device == null) {
            close(IllegalStateException("No camera available"))
            return@callbackFlow
        }

        try {
            session = AVCaptureSession()
            session!!.beginConfiguration()

            val input = AVCaptureDeviceInput.deviceInputWithDevice(device, null) as AVCaptureDeviceInput
            if (session!!.canAddInput(input)) {
                session!!.addInput(input)
            }

            val output = AVCaptureVideoDataOutput()
            output.alwaysDiscardsLateVideoFrames = true
            output.setSampleBufferDelegate(delegate, dispatch_get_main_queue())
            
            val settings = mapOf(kCVPixelBufferPixelFormatTypeKey as Any? to kCVPixelFormatType_32BGRA.toLong() as Any?)
            output.videoSettings = settings as Map<Any?, *>

            if (session!!.canAddOutput(output)) {
                session!!.addOutput(output)
            }

            session!!.commitConfiguration()

            // Turn on Torch
            if (device.hasTorch && device.isTorchModeSupported(AVCaptureTorchModeOn)) {
                device.lockForConfiguration(null)
                device.torchMode = AVCaptureTorchModeOn
                device.unlockForConfiguration()
            }

            session!!.startRunning()
        } catch (e: Exception) {
            close(e)
        }

        awaitClose {
            stop()
        }
    }

    private fun stop() {
        session?.stopRunning()
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
        if (device != null && device.hasTorch) {
            device.lockForConfiguration(null)
            device.torchMode = AVCaptureTorchModeOff
            device.unlockForConfiguration()
        }
        session = null
        callbackScope = null
    }

    @OptIn(UnsafeNumber::class)
    private fun processFrame(didOutputSampleBuffer: CMSampleBufferRef?) {
        val buffer = CMSampleBufferGetImageBuffer(didOutputSampleBuffer) ?: return
        CVPixelBufferLockBaseAddress(buffer, kCVPixelBufferLock_ReadOnly)
        
        val width = CVPixelBufferGetWidth(buffer).toInt()
        val height = CVPixelBufferGetHeight(buffer).toInt()
        val baseAddress = CVPixelBufferGetBaseAddress(buffer) ?: return
        val bytesPerRow = CVPixelBufferGetBytesPerRow(buffer).toInt()
        
        val data = baseAddress.reinterpret<ByteVar>()
        
        // ROI: Central 20%
        val roiSize = 0.2
        val startX = (width * (0.5 - roiSize / 2)).toInt()
        val startY = (height * (0.5 - roiSize / 2)).toInt()
        val endX = (width * (0.5 + roiSize / 2)).toInt()
        val endY = (height * (0.5 + roiSize / 2)).toInt()
        
        var redSum = 0.0
        var count = 0
        
        // Format is BGRA
        for (y in startY until endY step 4) {
            for (x in startX until endX step 4) {
                val offset = y * bytesPerRow + x * 4
                val r = data[offset + 2].toInt() and 0xFF
                redSum += r
                count++
            }
        }
        
        CVPixelBufferUnlockBaseAddress(buffer, kCVPixelBufferLock_ReadOnly)
        
        if (count > 0) {
            val avgRed = redSum / count
            val timestamp = (NSDate().timeIntervalSince1970 * 1000).toLong()
            val result = ppgAlgorithm.processSample(avgRed, timestamp)
            
            if (result != null) {
                val currentBpm = if (result.fingerDetected) result.bpm else 0f
                callbackScope?.trySend(KSensorResponse(SensorData.HeartRate(
                    heartRate = currentBpm,
                    source = SensorData.HeartRateSource.CAMERA_PPG,
                    confidence = result.confidence,
                    quality = result.quality,
                    timestamp = timestamp
                )))
            }
        }
    }
}
