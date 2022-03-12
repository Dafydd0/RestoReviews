package com.example.reviewappfinalisimo

//Will store the place info when added to bookmark
data class UserBookmark(
    var email: String = "",
    var bookmark: Boolean = false,
    var restaurant: String = "",
    var imageUrl: String = "",
    var rating: String = "",
    var placeAddress: String = "",
    var placeUrl: String = ""
)
