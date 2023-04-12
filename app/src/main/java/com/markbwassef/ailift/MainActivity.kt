package com.markbwassef.ailift

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
import com.markbwassef.ailift.ml.LandmarkClassification
import com.markbwassef.ailift.ml.SsdMobilenetV11Metadata1
import com.markbwassef.ailift.ui.theme.AILiftTheme
import org.tensorflow.lite.support.image.TensorImage

class MainActivity : ComponentActivity() {

    //lateinit var model: SsdMobilenetV11Metadata1
   // val labels = FileUtil.loadLabels(this, "labels.txt")
    private val imageLive = MutableLiveData<Bitmap>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var labels = application.assets.open("labels.txt").bufferedReader().readLines()

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
                                /*val model = SsdMobilenetV11Metadata1.newInstance(this@MainActivity)
                                // Creates inputs for reference.
                                val tensorImage = TensorImage.fromBitmap(image)
                                // Runs model inference and gets result.
                                val outputs = model.process(tensorImage)
                                val locations = outputs.locationsAsTensorBuffer
                                val classes = outputs.classesAsTensorBuffer
                                val scores = outputs.scoresAsTensorBuffer
                                val numberOfDetections = outputs.numberOfDetectionsAsTensorBuffer
                                prediction = numberOfDetections.toString()*/

                                val model = LandmarkClassification.newInstance(this@MainActivity)
                                val image = TensorImage.fromBitmap(image)

                                val outputs = model.process(image)
                                val probability = outputs.probabilityAsCategoryList

                                model.close()

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
       // model.close()
    }

}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AILiftTheme {

    }
}