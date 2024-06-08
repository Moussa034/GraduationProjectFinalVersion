package com.example.mapping.UI.CrackReporting
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import okhttp3.*
import com.example.mapping.R
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.app.Application
import android.content.Context
import android.os.Environment
import android.view.View
import com.example.mapping.UI.Home.MainActivity
import com.example.mapping.UI.Profile.ProfileActivity
import com.example.mapping.databinding.ActivityProfileBinding
import com.example.mapping.databinding.CameraCaptureBinding
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.database.database
import org.json.JSONArray
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.logging.Handler

class cameraCapture : AppCompatActivity() {

    private lateinit var captureIV: ImageView
    private lateinit var imageURL: Uri
    private lateinit var address: TextView
    private lateinit var city: TextView
    private lateinit var country: TextView
    private lateinit var processing : TextView
    private lateinit var wait: TextView
    private val PICK_IMAGE_REQUEST = 1
    private val SERVER_URL = "http://192.168.1.8:5000/"
    private var uri: Uri? = null
    private val REQUEST_CODE = 100
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    var long = 0.0
    var lat = 0.0

    private val contract = registerForActivityResult(ActivityResultContracts.TakePicture()) {
        try {
            captureIV.setImageURI(null)
            captureIV.setImageURI(imageURL)

            // Resize the captured image
            val resizedBitmap = resizeImage(imageURL, 640, 640)

            // Convert the resized image to a binary list
            val imageBinaryList = resizedBitmap?.let { convertBitmapToByteList(it) }

            // Send the binary data to the server
            if (imageBinaryList != null) {
                sendBinaryDataToServer(object : ApiResponseCallback {
                    override fun onSuccess(response: String) {
                        try {
                            val intent =
                                Intent(applicationContext, PredictedImage::class.java).apply {
                                    putExtra("IMAGE_URI", bitmapToUri(applicationContext, resizedBitmap).toString())
                                    putExtra("Longitude", long.toString())
                                    putExtra("Latitude", lat.toString())
                                    putExtra("DATA", response)
                                    println(response)
                                }
                            startActivity(intent)
                        } catch (e: Exception) {
                            val y = 5
                        }
                    }

                    override fun onError(error: String) {
                        val z = 66
                    }
                }, imageBinaryList)
            }
        } catch (e: Exception) {
            val z = 8
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            FirebaseApp.initializeApp(this)

            setContentView(R.layout.camera_capture)
            enableEdgeToEdge()
            imageURL = CreateImagUrl()
            captureIV = findViewById(R.id.captureImage)
            address = findViewById(R.id.Address)
            city = findViewById(R.id.city)
            country = findViewById(R.id.country)
            processing = findViewById(R.id.hiddenTextView)
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

            val captureImgBtn = findViewById<Button>(R.id.captureButton)
            captureImgBtn.setOnClickListener {
                val handler = android.os.Handler()
                contract.launch(imageURL)
                handler.postDelayed({
                    getLastLocation()
                }, 1000)

                captureImgBtn.visibility=View.GONE

                handler.postDelayed({
                    processing.visibility = View.VISIBLE
                }, 1000)

            }

        } catch (e: Exception) {
            val x = 5
        }
    }


    fun bitmapToUri(context: Context, bitmap: Bitmap): Uri? {
        // Get the external storage directory
        val imagesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File(imagesDir, "image.png") // Create a new file in the Pictures directory

        try {
            // Open an OutputStream to write the Bitmap data to the file
            val outputStream: OutputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream) // Compress the bitmap to PNG format
            outputStream.flush()
            outputStream.close()

            // Return the Uri of the saved file
            return Uri.fromFile(imageFile)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun CreateImagUrl(): Uri {
        val image = File(filesDir, "camera_photos.png")
        return FileProvider.getUriForFile(
            this,
            "com.example.mapping.UI.CrackReporting.FileProvider",
            image
        )
    }

    private fun getLastLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        val latitude = location.latitude
                        val longitude = location.longitude
                        getAddress(latitude, longitude)
                    }
                }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_CODE
            )
        }
    }

    private fun getAddress(latitude2: Double, longitude2: Double) {
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses = geocoder.getFromLocation(latitude2, longitude2, 1)

        if (addresses != null && addresses.isNotEmpty()) {
            val addressLine = addresses[0]?.getAddressLine(0)
            val city2 = addresses[0]?.locality
            val country2 = addresses[0]?.countryName

            address.text = addressLine
            city.text = city2
            country.text = country2
            long = longitude2
            lat = latitude2

        } else {
            println("No address found")
        }
    }

    private fun resizeImage(imageUri: Uri, width: Int, height: Int): Bitmap? {
        val inputStream = contentResolver.openInputStream(imageUri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        return Bitmap.createScaledBitmap(originalBitmap, width, height, true)
    }

    private fun convertBitmapToByteList(bitmap: Bitmap): List<Int> {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return byteArray.map { it.toInt() and 0xFF }
    }

    private fun sendBinaryDataToServer(callback: ApiResponseCallback, binaryData: List<Int>) {
        try {
            Thread {
                val client = OkHttpClient.Builder()
                    .connectTimeout(240, TimeUnit.SECONDS)
                    .readTimeout(240, TimeUnit.SECONDS)
                    .build()

                val jsonData = JSONObject()
                jsonData.put("binary_list", binaryData.toString())
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonData.toString().toRequestBody(mediaType)
                val request = Request.Builder()
                    .url(SERVER_URL)
                    .post(requestBody)
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        callback.onError("Unsuccessful response: ${e.message}")
                        e.printStackTrace()
                    }

                    override fun onResponse(call: Call, response: Response) {
                        try {
                            val responseBody = response.body?.string()
                            responseBody?.let {
                                callback.onSuccess(responseBody)
                            } ?: callback.onError("Empty response body")
                        } catch (e: Exception) {
                            Log.e("Success", "ErrorL " + e.message)
                        }
                    }
                })
            }.start()
        } catch (e: Exception) {
            val x = 5
        }
    }

}
