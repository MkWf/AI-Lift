package com.markbwassef.ailift

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.media.Image
import android.os.Bundle
import android.provider.MediaStore
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview

import androidx.lifecycle.MutableLiveData
import com.markbwassef.ailift.ml.SsdMobilenetV11Metadata1
import com.markbwassef.ailift.ui.theme.AILiftTheme
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

class MainActivity : ComponentActivity() {

    lateinit var model: SsdMobilenetV11Metadata1
    private val imageLive = MutableLiveData<Bitmap>()
    lateinit var labels: List<String>
    val imageProcessor = ImageProcessor
        .Builder().add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR)).build()
    val paint = Paint()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        labels = FileUtil.loadLabels(this, "labels.txt")

        setContent {
            AILiftTheme {
                val image = imageLive.observeAsState().value
                var prediction = remember {"New Prediction"}

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column() {
                        image?.asImageBitmap()?.let {
                            Image(
                                bitmap = it,
                                contentDescription = null,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ){
                            Button(onClick ={
                                val intent = Intent()
                                intent.type = "image/*"
                                intent.action = Intent.ACTION_GET_CONTENT
                                startActivityForResult(intent, 100)
                            }){
                                Text(text = "Select Image")
                            }
                            Button(onClick ={
                                model = SsdMobilenetV11Metadata1.newInstance(this@MainActivity)

                                var tensorImage = TensorImage.fromBitmap(image)
                                tensorImage = imageProcessor.process(tensorImage)

                                val outputs = model.process(tensorImage)
                                val locations = outputs.locationsAsTensorBuffer.floatArray
                                val classes = outputs.classesAsTensorBuffer.floatArray
                                val scores = outputs.scoresAsTensorBuffer.floatArray
                                val numberOfDetections = outputs.numberOfDetectionsAsTensorBuffer.floatArray

                                var mutableBitmap = image?.copy(Bitmap.Config.ARGB_8888, true)
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
                                        canvas?.drawRect(RectF((locations[x+1]*width),
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

                            }){
                                Text(text = "Predict")
                            }
                        }
                        Text(
                            text = prediction
                        )
                    }
                }
            }
        }

      //  model = SsdMobilenetV11Metadata1.newInstance(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 100){
            var uri = data?.data
            imageLive.value = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
            //get_predictions()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        model.close()
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AILiftTheme {

    }
}