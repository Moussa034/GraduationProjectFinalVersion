package com.example.mapping.UI.Home

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
import androidx.lifecycle.lifecycleScope
import com.example.mapping.R
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

class MainActivity : AppCompatActivity() {

    private val qgisCloudMapUrl = "https://qgiscloud.com/AbdoZahran/TRIAL3/"
    private val baseUrl = "https://wms.qgiscloud.com/AbdoZahran/TRIAL3/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val webView: WebView = findViewById(R.id.webView)
        val searchEditText: EditText = findViewById(R.id.searchEditText)
        val searchButton: Button = findViewById(R.id.searchButton)

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
                fetchFeatureData(crackNumber)
            } else {
                // Handle empty input if necessary
                Log.e("MainActivity", "Search input is empty")
            }
        }

        // Fetch initial feature data
        fetchFeatureData("Cracks")
    }

    private fun fetchFeatureData(typeName: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(QgisCloudService::class.java)

        lifecycleScope.launch {
            try {
                val response = service.getFeature(typeName)
                if (response.isSuccessful) {
                    val featureCollection = response.body()
                    featureCollection?.features?.let { features ->
                        if (features.isNotEmpty()) {
                            // Process the first feature or implement your logic
                            val feature = features[0]
                            val properties = feature.properties
                            val geometry = feature.geometry

                            // Trigger an alarm or notification based on the feature data
                            triggerAlarm(properties, geometry)
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
