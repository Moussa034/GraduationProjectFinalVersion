package com.example.mapping

import retrofit2.http.GET
import retrofit2.http.Query

interface ApiCalls {
    @GET("wms")
    suspend fun getFeature(
        @Query("SERVICE")SERVICE:String="WMS",
        @Query("VERSION")VERSION:String="1.3.0",
        @Query("REQUEST")REQUEST:String="GetFeatureInfo",
        @Query("LAYERS")LAYERS:String="modifiedCracksLayes",
        @Query("QUERY_LAYERS")QUERY_LAYERS:String="modifiedCracksLayes",
        @Query("INFO_FORMAT")INFO_FORMAT:String="application/json",
        @Query("CRS")CRS:String="EPSG:4326",
        @Query("feature_count") feature_count: String= "99999",
        @Query("I") I: String= "0",
        @Query("J") J: String= "0",


            ): retrofit2.Response<FeatureResponseModel>
}