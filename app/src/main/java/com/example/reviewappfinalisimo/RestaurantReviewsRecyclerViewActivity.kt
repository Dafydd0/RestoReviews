package com.example.reviewappfinalisimo

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_recycler_view.*


class RestaurantReviewsRecyclerViewActivity : AppCompatActivity(), OnUserItemClickListenerReview {

    private lateinit var email: String
    private lateinit var restaurant: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recycler_view)

        val bundle: Bundle? = intent.extras
        email = bundle?.getString("email").toString()
        restaurant = bundle?.getString("restaurant").toString()

        rvUsers.adapter.apply {
            rvUsers.layoutManager = LinearLayoutManager(this@RestaurantReviewsRecyclerViewActivity)
        }
        fetchData()
    }

    override fun onResume() {
        super.onResume()
        fetchData()
    }

    //Fetch data of restaurant given
    private fun fetchData() {
        var userReviews: MutableList<UserReview> = mutableListOf()
        FirebaseFirestore.getInstance().collection("user_review")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    if (document.id.contains(restaurant)) {
                        val user = document.toObject(UserReview::class.java)
                        userReviews.add(user)
                        rvUsers.adapter = RestaurantReviewsAdapter(this, userReviews, this)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error trying to fetch data", Toast.LENGTH_LONG).show()
            }
    }

    //When clicking on a recyclerView item, info will be sent to ReviewActivity, just if the user is the owner of the review
    override fun onItemClick(item: UserReview, position: Int) {
        if (email != "guest_email") {
            if (email == item.email) {
                val intent = Intent(this, ReviewActivity::class.java).apply {
                    putExtra("email", item.email)
                    putExtra("restaurant", item.restaurant)
                    putExtra("rating", item.rating)
                    putExtra("imageUrl", item.imageUrl)
                    putExtra("placeAddress", item.location)
                    putExtra("comment", item.comment)
                    putExtra("edit", true)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this,
                    "You can't modify a review which isn't yours",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
}