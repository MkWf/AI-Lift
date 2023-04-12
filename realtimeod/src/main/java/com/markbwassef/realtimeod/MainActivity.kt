package com.markbwassef.realtimeod

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.markbwassef.realtimeod.ml.SsdMobilenetV11Metadata1
import com.markbwassef.realtimeod.ui.theme.AILiftTheme
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

class MainActivity : ComponentActivity() {

    lateinit var cameraDevice: CameraDevice
    lateinit var cameraManager: CameraManager
    lateinit var handler: Handler
    lateinit var textureView: TextureView
    lateinit var bitmap: Bitmap
    lateinit var model: SsdMobilenetV11Metadata1
    val imageProcessor = ImageProcessor
        .Builder().add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR)).build()
    val paint = Paint()
    lateinit var labels: List<String>
    private val imageLive = MutableLiveData<Bitmap>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val handlerThread = HandlerThread("video")
        handlerThread.start()
        handler = Handler(handlerThread.looper)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        getCameraPermission()
        labels = FileUtil.loadLabels(this, "labels.txt")

        setContent {
            AILiftTheme {
                val image = imageLive.observeAsState().value
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ){
                        AndroidView(
                            factory = {
                                textureView = TextureView(this@MainActivity)
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
                                        bitmap = textureView.bitmap!!

                                        model = SsdMobilenetV11Metadata1.newInstance(this@MainActivity)

                                        var tensorImage = TensorImage.fromBitmap(bitmap)
                                        tensorImage = imageProcessor.process(tensorImage)

                                        val outputs = model.process(tensorImage)
                                        val locations = outputs.locationsAsTensorBuffer.floatArray
                                        val classes = outputs.classesAsTensorBuffer.floatArray
                                        val scores = outputs.scoresAsTensorBuffer.floatArray
                                        val numberOfDetections = outputs.numberOfDetectionsAsTensorBuffer.floatArray

                                        var mutableBitmap = bitmap?.copy(Bitmap.Config.ARGB_8888, true)
                                        val canvas = mutableBitmap?.let { Canvas(it) }

                                        val height = mutableBitmap?.height!!
                                        val width = mutableBitmap?.width!!
                                        paint.textSize = height/15f
                                        paint.strokeWidth  = height/85f
                                        var x = 0
                                        scores.forEachIndexed { index, fl ->
                                            x = index
                                            x *= 4
                                            if(fl > 0.5){
                                                paint.style = Paint.Style.STROKE
                                                canvas?.drawRect(
                                                    RectF((locations[x+1]*width),
                                                        locations[x]*height, locations[x+3]*width, locations[x+2]*height
                                                    ), paint)
                                                paint.style = Paint.Style.FILL
                                                canvas?.drawText(
                                                    labels[classes[index].toInt()]+" "+fl.toString(),
                                                    locations[x+1]*width,
                                                    locations[x]*height,
                                                    paint)
                                            }
                                        }
                                        imageLive.value = mutableBitmap
                                    }
                                }
                                textureView
                            })
                        image?.asImageBitmap()?.let {
                            Image(
                                bitmap = it,
                                contentDescription = null,
                            )
                        }
                    }
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

    override fun onDestroy() {
        super.onDestroy()
        model.close()
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






