package com.manoj.backgroundvideorecorder.features.recording.domain

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File

class CameraXManager(private val context: Context) {

    fun setupCamera(
        lifecycleOwner: LifecycleOwner,
        cameraFacing: Int,
        videoResolution: String
    ): VideoCapture<Recorder>? {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraProvider = cameraProviderFuture.get()

        val quality = when (videoResolution) {
            "480p" -> Quality.SD
            "720p" -> Quality.HD
            "1080p" -> Quality.FHD
            else -> Quality.FHD
        }
        val qualitySelector = QualitySelector.from(quality)

        val recorder = Recorder.Builder()
            .setExecutor(ContextCompat.getMainExecutor(context))
            .setQualitySelector(qualitySelector)
            .build()
            
        val videoCapture = VideoCapture.withOutput(recorder)

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(cameraFacing)
            .build()

        cameraProvider.unbindAll()

        try {
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                videoCapture
            )
            return videoCapture
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
