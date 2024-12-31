package com.megleo.ai_deploy

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Paint.Cap
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.media.ImageReader
import android.os.Bundle
import android.util.Log
import android.view.ContentInfo
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.megleo.ai_deploy.ui.theme.AIDeployTheme
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

val TAG = "MainActivate"


class MainActivity : ComponentActivity() {
    private lateinit var cameraDevice: CameraDevice
    private lateinit var textureView: TextureView
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var cameraCaptureSession: CameraCaptureSession
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraId: String

    private fun checkPermissions() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 100)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
        setContentView(R.layout.activity_main)

        textureView = findViewById(R.id.textureView)
        val btnCapture: Button = findViewById(R.id.btnCapture)

        textureView.surfaceTextureListener = textureListener

        btnCapture.setOnClickListener{
            capturePhoto()
        }
    }
    private val textureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {}

        override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
            return  false
        }

        override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
        }
    }

    private fun openCamera() {
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            cameraId = cameraManager.cameraIdList[0]
            if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(cameraId, stateCallback, null)
            }
        }catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }
    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            startCameraPreview()
        }

        override fun onDisconnected(p0: CameraDevice) {
            cameraDevice.close()
        }

        override fun onError(p0: CameraDevice, p1: Int) {
            cameraDevice.close()
        }
    }

    private lateinit var imageReader: ImageReader

    private fun startCameraPreview() {
        val texture = textureView.surfaceTexture
        if (texture != null) {
            texture.setDefaultBufferSize(1920, 1080)
        }
        val surface = Surface(texture)
        imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 1)
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            image.close()
            saveImage(bytes)
        }, null)

        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder.addTarget(surface)

            // 创建CaptureSession，包括imageReader.surface
            cameraDevice.createCaptureSession(listOf(surface, imageReader.surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    cameraCaptureSession = session
                    updateView()
                }

                override fun onConfigureFailed(p0: CameraCaptureSession) {
                    Log.e(TAG, "Error when onConfigureFailed")
                }
            }, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }


    private fun updateView() {
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        try {
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun updatePreview() {
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        try {
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun capturePhoto() {
        try {
            // 创建拍照请求
            val captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(imageReader.surface) // 只将 ImageReader 作为目标

            // 设置拍照参数，例如自动对焦、自动曝光等
            // captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)

            // 拍照完成后，保持预览
            cameraCaptureSession.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                    super.onCaptureCompleted(session, request, result)
                    Toast.makeText(this@MainActivity, "Photo Captured", Toast.LENGTH_SHORT).show()
                    // updateView() // 拍照后继续预览
                }
            }, null)

        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun saveImage(bytes: ByteArray) {
        val file = File(getExternalFilesDir(null), "${System.currentTimeMillis()}.jpg")
        try {
            FileOutputStream(file).use {
                it.write(bytes)
                Toast.makeText(this, "Photo saved: ${file.absolutePath}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
