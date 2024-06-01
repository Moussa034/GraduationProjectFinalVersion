package com.example.mapping

data class FeatureResponseModel(
    val features: List<Feature>,
    val type: String
)

data class Feature(
    val geometry: Any,
    val id: String,
    val properties: Properties,
    val type: String
)

data class Properties(
    val boody: Int,
    val boody_1: Int,
    val date: String,
    val detection: String,
    val frame_numb: String,
    val latitude: Double,
    val longitude: Double,
    val orig_fid: Int,
    val qc_id: Int,
    val time: String,
    val timestamp: String
)