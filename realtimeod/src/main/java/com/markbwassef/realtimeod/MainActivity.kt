package com.markbwassef.realtimeod

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import com.markbwassef.realtimeod.ui.theme.AILiftTheme

class MainActivity : ComponentActivity() {

    lateinit var cameraDevice: CameraDevice
    lateinit var cameraManager: CameraManager
    lateinit var handler: Handler
    lateinit var textureView: TextureView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val handlerThread = HandlerThread("video")
        handlerThread.start()
        handler = Handler(handlerThread.looper)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        getCameraPermission()




        setContent {
            AILiftTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    AndroidView(
                        factory = {
                            textureView = TextureView(this)
                            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener{
                                override fun onSurfaceTextureAvailable(
                                    surface: SurfaceTexture,
                                    width: Int,
                                    height: Int
                                ) {
                                    openCamera()
                                }

                                override fun onSurfaceTextureSizeChanged(
                                    surface: SurfaceTexture,
                                    width: Int,
                                    height: Int
                                ) {

                                }

                                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                    return false
                                }

                                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

                                }
                            }
                            textureView
                        }
                    )
                }
            }
        }
    }

    fun getCameraPermission(){
        if(ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
            getCameraPermission()
        }
    }

    fun openCamera(){
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        cameraManager.openCamera(cameraManager.cameraIdList[0], object :
            CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera

                var surfaceTexture = textureView.surfaceTexture
                var surface = Surface(surfaceTexture)

                var captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                captureRequest.addTarget(surface)

                cameraDevice.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback()  {
                    override fun onConfigured(session: CameraCaptureSession) {
                        session.setRepeatingRequest(captureRequest.build(), null, null)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {

                    }
                }, handler)
            }

            override fun onClosed(camera: CameraDevice) {
                super.onClosed(camera)
            }

            override fun onDisconnected(camera: CameraDevice) {

            }

            override fun onError(camera: CameraDevice, error: Int) {

            }
        }, handler)
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AILiftTheme {
        Greeting("Android")
    }
}






