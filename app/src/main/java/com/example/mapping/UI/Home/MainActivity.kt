package com.example.mapping.UI.Home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.mapping.ApiCalls
import com.example.mapping.Feature
import com.example.mapping.R
import com.example.mapping.UI.CrackReporting.cameraCapture
import com.example.mapping.UI.Profile.ProfileActivity
import com.example.mapping.databinding.ActivityMainBinding
import com.example.mapping.databinding.ActivityProfileBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private val qgisCloudMapUrl =
        "https://qgiscloud.com/Moussa03/modifiedGPmap/?l=modifiedCracksLayes&bl=mapnik&t=modifiedGPmap&e=3483012%2C3484240%2C3491140%2C3487960"
    private val baseUrl = "https://qgiscloud.com/Moussa03/modifiedGPmap/"
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLocation: Location? = null
    private lateinit var pointsArray: Array<Array<Double>>
    private lateinit var userLocationArray: Array<Double>
    private var locationFetchJob: Job? = null
    private val locationFetchInterval = 10000L // 10 seconds
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onClick()

        initWebView()
        initializeLocationClient()

        lifecycleScope.launch {
            val fetchJob = fetchFeatureData()
            fetchJob.join() // Wait for fetchFeatureData() to complete
            checkLocationPermissionsAndFetchLocation()
        }
    }
    private fun initWebView() {
        val webView: WebView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
        webView.clearCache(true)

        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                // Handle error
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                // Handle any actions needed after the page has finished loading
            }
        }

        webView.loadUrl(qgisCloudMapUrl)
    }

    private fun initializeLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private suspend fun fetchFeatureData(): Job {
        val client = OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(150, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS).build()
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl).client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(ApiCalls::class.java)

        return lifecycleScope.launch {
            try {
                val response = service.getFeature()
                if (response.code() == 200) {
                    val featureCollection = response.body()
                    featureCollection?.features?.let { features ->
                        if (features.isNotEmpty()) {
                            pointsArray = convertFeaturesTo2DArray(features)
                            return@launch
                        }
                    }
                } else {
                    Log.e("QgisCloud", "Error: ${response.errorBody()}")
                }
            } catch (e: Exception) {
                Log.e("QgisCloud", "Exception: ${e.message}")
            }
        }
    }

    private fun checkLocationPermissionsAndFetchLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            startLocationFetchLoop()
        }
    }

    private fun startLocationFetchLoop() {
        locationFetchJob = lifecycleScope.launch {
            while (isActive) {
                val location = getLastKnownLocation()
                if (location != null) {
                    userLocation = location
                    //        userLocationArray = arrayOf(location.longitude, location.latitude)
                    userLocationArray = arrayOf(
                        3486164.43487, 3487510.21137
                    )
//Moussa send regards
                    Log.e(
                        "MainActivity",
                        "Latitude: ${location.latitude}, Longitude: ${location.longitude}"
                    )
                    checkProximityToPoints()
                } else {
                    Log.e("MainActivity", "Failed to get location.")
                }

                delay(locationFetchInterval)
            }
        }
    }

    private suspend fun getLastKnownLocation(): Location? {
        return withContext(Dispatchers.IO) {
            var location: Location? = null
            val job = CompletableDeferred<Unit>()
            if (ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    location = loc
                    job.complete(Unit)
                }.addOnFailureListener {
                    job.complete(Unit)
                }
            }
            job.await()
            location
        }
    }

    private fun checkProximityToPoints() {
        var location = 0
        for (point in pointsArray) {
            val distance = calculateDistance(
                userLocationArray[1], userLocationArray[0],
                point[1], point[0]
            )
            if (distance <= 500) {
                triggerAlarm()
                break
            } else {
                Log.e("MainActivity", "ALARM TRIGGERED")
                location++
            }
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000 // meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    private fun triggerAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound)
        mediaPlayer?.start()

        // Stop the sound after 5 seconds
        lifecycleScope.launch {
            delay(5000)
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationFetchLoop()
            } else {
                // Permission denied
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationFetchJob?.cancel() // Cancel the job when the activity is destroyed
        mediaPlayer?.release()
    }

    private fun convertFeaturesTo2DArray(features: List<Feature>): Array<Array<Double>> {
        return features.map { feature ->
            arrayOf(feature.properties.longitude, feature.properties.latitude)
        }.toTypedArray()
    }

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
    }

    private fun onClick() {

        binding.profile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.camera.setOnClickListener {
            val intent = Intent(this, cameraCapture::class.java)
            startActivity(intent)
            finish()
        }

    }
}
