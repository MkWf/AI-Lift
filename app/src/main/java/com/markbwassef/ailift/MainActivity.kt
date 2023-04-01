package com.markbwassef.ailift

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
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
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import com.markbwassef.ailift.ui.theme.AILiftTheme

class MainActivity : ComponentActivity() {

   // lateinit var model: SsdMobilenetV11Metadata1
   // val labels = FileUtil.loadLabels(this, "labels.txt")
    private val imageLive = MutableLiveData<Bitmap>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AILiftTheme {
                val image = imageLive.observeAsState().value

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

                            }){
                                Text(text = "Predict")
                            }
                        }
                        Text(
                            text = "Prediction"
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
            get_predictions()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
       // model.close()
    }

    fun get_predictions(){
      //  val image = TensorImage.fromBitmap(bitmap)
       // val outputs = model.process(image)
      //  val locations = outputs.locationsAsTensorBuffer
      //  val classes = outputs.classesAsTensorBuffer
      //  val scores = outputs.scoresAsTensorBuffer
      //  val numberOfDetections = outputs.numberOfDetectionsAsTensorBuffer
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AILiftTheme {

    }
}