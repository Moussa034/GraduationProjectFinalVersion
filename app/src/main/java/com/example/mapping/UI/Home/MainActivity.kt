package com.example.mapping.UI.Home

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.mapping.ApiCalls
import com.example.mapping.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnSuccessListener
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
//Moussa's Version 2
class MainActivity : AppCompatActivity() {

    private val qgisCloudMapUrl = "https://qgiscloud.com/Moussa03/modifiedGPmap/?l=modifiedCracksLayes&bl=mapnik&t=modifiedGPmap&e=3483012%2C3484240%2C3491140%2C3487960"
    private val baseUrl = "https://qgiscloud.com/Moussa03/modifiedGPmap/"
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val webView: WebView = findViewById(R.id.webView)
        val searchEditText: EditText = findViewById(R.id.searchEditText)
        val searchButton: Button = findViewById(R.id.searchButton)

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Enable JavaScript and other settings
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.cacheMode = WebSettings.LOAD_DEFAULT

        // Override WebViewClient to handle errors
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                // Handle error
                // You can show a custom error message or take other actions
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                // Handle any actions needed after the page has finished loading
            }
        }

        // Clear cache to avoid ERR_CACHE_MISS
        webView.clearCache(true)

        // Load the QGIS Cloud map URL
        webView.loadUrl(qgisCloudMapUrl)

        // Set up button click listener
        searchButton.setOnClickListener {
            val crackNumber = searchEditText.text.toString()
            if (crackNumber.isNotEmpty()) {
                // Perform search or fetch feature data based on the crack number
                fetchFeatureData()
            } else {
                // Handle empty input if necessary
                Log.e("MainActivity", "Search input is empty")
            }
        }

        // Fetch initial feature data
        fetchFeatureData()

        // Check location permissions and get the last known location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            getLastKnownLocation()
        }
    }

    private fun getLastKnownLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener(this, OnSuccessListener<Location> { location ->
                    // Got last known location. In some rare situations, this can be null.
                    if (location != null) {
                        // Store the location object in userLocation variable
                        userLocation = location
                        Log.e("MainActivity", "Latitude: ${location.latitude}, Longitude: ${location.longitude}")
                    }
                })
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    getLastKnownLocation()
                } else {
                    // Permission denied
                }
                return
            }
        }
    }

    private fun fetchFeatureData() {
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

        lifecycleScope.launch {
            try {
                val response = service.getFeature()
                if (response.code()==200) {
                    val featureCollection = response.body()
                    featureCollection?.features?.let { features ->
                        if (features.isNotEmpty()) {
                            // Process the first feature or implement your logic
                            val feature = features[0]
                            val properties = feature.properties
                            val geometry = feature.geometry

                            // Trigger an alarm or notification based on the feature data
                     //       triggerAlarm(properties, geometry)
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

    private fun triggerAlarm(properties: Map<String, Any>, geometry: Geometry) {
        // Implement your alarm or notification logic here
        // For example, check if a certain property matches a condition
        val alertProperty = properties["alert"]
        if (alertProperty == "trigger") {
            // Trigger the alarm or notification
            Log.d("QgisCloud", "Alarm triggered based on feature data")
        }
    }

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
    }
}

interface QgisCloudService {
    @GET("wfs?service=WFS&version=1.0.0&request=GetFeature&outputFormat=application/json")
    suspend fun getFeature(
        @Query("typeName") typeName: String
    ): retrofit2.Response<FeatureCollection>
}

data class FeatureCollection(
    val type: String,
    val features: List<Feature>
)

data class Feature(
    val type: String,
    val properties: Map<String, Any>,
    val geometry: Geometry
)

data class Geometry(
    val type: String,
    val coordinates: List<Double>
)
