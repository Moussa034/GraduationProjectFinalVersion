package com.example.mapping.UI.CrackReporting

interface ApiResponseCallback {
    fun onSuccess(response: String)
    fun onError(error: String)
}
