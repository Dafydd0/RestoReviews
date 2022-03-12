package com.example.reviewappfinalisimo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_recycler_view.*

class UserReviewsRecyclerViewActivity : AppCompatActivity(), OnUserItemClickListenerReview {

    private lateinit var email: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recycler_view)

        val bundle:Bundle? = intent.extras
        email = bundle?.getString("email").toString()

        rvUsers.adapter.apply {
            rvUsers.layoutManager = LinearLayoutManager(this@UserReviewsRecyclerViewActivity)
        }
        fetchData()
    }

    override fun onResume() {
        super.onResume()
        fetchData()
    }

    // Search all reviews posted by an user
    private fun fetchData() {
        var userReviews: MutableList<UserReview> = mutableListOf()
        FirebaseFirestore.getInstance().collection("user_review")
            .get()
            .addOnSuccessListener { result ->
                for (document in result){
                    if (document.id.contains(email)) {
                        val user = document.toObject(UserReview::class.java)
                        userReviews.add(user)
                        rvUsers.adapter = UserReviewsAdapter(this, userReviews, this)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error RecyclerViewActivity", Toast.LENGTH_LONG).show()
            }
    }

    //When clicking on a recyclerView item, info will be sent to ReviewActivity
    override fun onItemClick(item: UserReview, position: Int) {
        val intent = Intent(this, ReviewActivity::class.java).apply {
            putExtra("email", item.email)
            putExtra("restaurant", item.restaurant)
            putExtra("rating", item.rating)
            putExtra("imageUrl", item.imageUrl)
            putExtra("comment", item.comment)
            putExtra("placeAddress", item.location)
            putExtra("edit", true)
        }
        startActivity(intent)
    }
}