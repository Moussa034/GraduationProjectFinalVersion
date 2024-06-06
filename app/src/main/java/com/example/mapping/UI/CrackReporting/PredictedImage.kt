package com.example.mapping.UI.CrackReporting

import android.content.ContentResolver
import android.content.Context
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.database.database
import org.json.JSONArray
import org.json.JSONObject
import android.content.Intent
import android.graphics.BitmapFactory
import java.io.IOException
import com.example.mapping.R
import com.example.mapping.UI.Home.MainActivity

class PredictedImage : AppCompatActivity() {

    private lateinit var sendDB: Button
    private lateinit var message : TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.predicted_image)


            // Retrieve the URI from the Intent
            val imageUriString = intent.getStringExtra("IMAGE_URI")
            val imageUri = Uri.parse(imageUriString)

            val imageSize: Pair<Int, Int>? = getImageSize(this, imageUri) // 'this' is your activity or application context

            // boxes
            val data = intent.getStringExtra("DATA")
            val jsonData = JSONObject(data)
            var tmp = jsonData.getString("results")
            val result = tmp.substring(1, tmp.length - 1)
            val boxes = JSONObject(result).getJSONObject("boxes").getJSONArray("xyxy")
            val classes = JSONObject(result).getJSONArray("classes")
            val longitude = intent.getStringExtra("Longitude")?.toDouble()
            val latitude = intent.getStringExtra("Latitude")?.toDouble()

            val imageView = findViewById<RectangleImageView>(R.id.imageView)

            sendDB = findViewById(R.id.send2DB)
            message = findViewById(R.id.hiddenThankYou)
            sendDB.setOnClickListener {
                if (longitude != null && latitude != null) {
                    addToDb(classes, longitude, latitude)
                }
                message.visibility = View.VISIBLE
                sendDB.visibility = View.GONE
                val handler = android.os.Handler()

                handler.postDelayed({
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }, 2000)

            }
            imageView.setImageURI(imageUri)

            for (i in 0 until boxes.length()) {
                val box: JSONArray? = boxes.getJSONArray(i)
                if (box != null) {
                    imageView.addRectangle(
                        RectF(
                            (box.get(0) as Double).toFloat(),
                            (box.get(1) as Double).toFloat(),
                            (box.get(2) as Double).toFloat(),
                            (box.get(3) as Double).toFloat()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.message?.let { Log.e("PredictedImage", it) }
        }
    }


    fun addToDb(classes: JSONArray, longitude: Double, latitude: Double) {
        try {
            // Initialize Firebase Realtime Database
            val database = Firebase.database
            val myRef = database.getReference("cracks_types")


            val crackRecord: HashMap<String, Any> = HashMap<String, Any>()

            crackRecord["cracksClasses"] = classes.toString()
            crackRecord["Longitude"] = longitude
            crackRecord["Latitude"] = latitude
            if(classes.length()>0){
                myRef.push().setValue(crackRecord)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Successfully added", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed added to DB", Toast.LENGTH_LONG).show()
                    }
            }

        } catch (e: Exception) {
            e.message?.let { Log.e("fwefew", it) }
        }
    }

    fun getImageSize(context: Context, imageUri: Uri): Pair<Int, Int>? {
        var width = 0
        var height = 0
        try {
            val resolver: ContentResolver = context.contentResolver
            val options = BitmapFactory.Options().apply {
                // Set inJustDecodeBounds to true to decode only the image dimensions, not the whole bitmap
                inJustDecodeBounds = true
            }
            // Decode the dimensions using BitmapFactory.decodeStream
            BitmapFactory.decodeStream(resolver.openInputStream(imageUri), null, options)

            // Retrieve the width and height from the BitmapFactory.Options object
            width = options.outWidth
            height = options.outHeight
        } catch (e: IOException) {
            e.printStackTrace()
        }
        // Return the width and height as a Pair<Int, Int> or null if there was an error
        return if (width > 0 && height > 0) {
            Pair(width, height)
        } else {
            null
        }
    }
    }
