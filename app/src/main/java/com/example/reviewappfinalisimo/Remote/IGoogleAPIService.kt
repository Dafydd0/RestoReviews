package com.example.reviewappfinalisimo.Remote

import com.example.reviewappfinalisimo.Model.MyPlaces
import com.example.reviewappfinalisimo.Model.PlaceDetail
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Url

interface IGoogleAPIService {
    @GET
    fun getNearbyPlaces(@Url url: String): Call<MyPlaces>

    @GET
    fun getDetailPlace(@Url url: String): Call<PlaceDetail>
}