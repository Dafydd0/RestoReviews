package com.example.reviewappfinalisimo

//Will store the review info
data class UserReview(
    var email: String = "",
    var uid: String = "",
    var restaurant: String = "",
    var rating: String = "",
    var comment: String = "",
    var location: String = "",
    var imageUrl: String = "",
)
