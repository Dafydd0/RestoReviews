package com.example.reviewappfinalisimo.Common

import com.example.reviewappfinalisimo.Model.Results
import com.example.reviewappfinalisimo.Remote.IGoogleAPIService
import com.example.reviewappfinalisimo.Remote.RetrofitClient

object Common {
    private val GOOGLE_API_URL = "https://maps.googleapis.com/"

    var currentResult: Results?= null

    val googleApiService: IGoogleAPIService
        get() = RetrofitClient
            .getClient(GOOGLE_API_URL).create(IGoogleAPIService::class.java)
}