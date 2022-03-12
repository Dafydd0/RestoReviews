package com.example.reviewappfinalisimo

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_recycler_view.*
import kotlinx.android.synthetic.main.activity_review.*

class UserBookmarksRecyclerViewActivity : AppCompatActivity(), OnUserItemClickListenerBookmark {

    private lateinit var email: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recycler_view)

        val bundle: Bundle? = intent.extras
        email = bundle?.getString("email").toString()

        rvUsers.adapter.apply {
            rvUsers.layoutManager = LinearLayoutManager(this@UserBookmarksRecyclerViewActivity)
        }
        fetchData()
    }

    override fun onResume() {
        super.onResume()
        fetchData()
    }

    // Search all restaurants in bookmarks of an user
    private fun fetchData() {
        var userBookmark: MutableList<UserBookmark> = mutableListOf()
        FirebaseFirestore.getInstance().collection("user_bookmark")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    if (document.id.contains(email)) {
                        val user = document.toObject(UserBookmark::class.java)
                        if (user.bookmark) {
                            userBookmark.add(user)
                            rvUsers.adapter = UserBookmarksAdapter(this, userBookmark, this)
                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error trying to fetch data", Toast.LENGTH_LONG)
                    .show()
            }
    }

    //When clicking on a recyclerView item, info will be sent to ViewPlaceActivity
    override fun onItemClick(item: UserBookmark, position: Int) {
            val intent = Intent(this, ViewPlaceActivity::class.java).apply {
                putExtra("email", item.email)
                putExtra("restaurant", item.restaurant)
                putExtra("bookmark", item.bookmark)
                putExtra("imageUrl", item.imageUrl)
                putExtra("rating", item.rating)
                putExtra("placeAddress", item.placeAddress)
                putExtra("calledByReview", false)
                putExtra("placeUrl", item.placeUrl)
            }
            startActivity(intent)
    }
}