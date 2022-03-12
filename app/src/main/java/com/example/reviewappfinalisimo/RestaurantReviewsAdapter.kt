package com.example.reviewappfinalisimo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class RestaurantReviewsAdapter(
    private val context: Context,
    private val userReviews: List<UserReview>,
    private var clickListener: OnUserItemClickListenerReview
) : RecyclerView.Adapter<RestaurantReviewsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(R.layout.item_view_all_restaurants, parent, false)
        )

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.initialize(userReviews[position], clickListener, context, holder)
    }

    override fun getItemCount(): Int {
        return userReviews.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val ratingBar: RatingBar = itemView.findViewById(R.id.item_ratingBar)
        private val username: TextView = itemView.findViewById(R.id.item_username)
        private val imageUrl: ImageView = itemView.findViewById(R.id.item_image)
        private val comment: TextView = itemView.findViewById(R.id.item_comment)

        fun initialize(
            item: UserReview,
            action: OnUserItemClickListenerReview,
            context: Context,
            holder: ViewHolder
        ) { //Set the recycler view info of each restaurant review
            ratingBar.rating = item.rating.toFloat()
            ratingBar.setIsIndicator(true)
            comment.text = item.comment

            fetchUser(item)

            if (item.imageUrl != "") {
                Picasso.with(context)
                    .load(
                        item.imageUrl
                    ).resize(750, 750).centerCrop().into(holder.imageUrl)
            }

            itemView.setOnClickListener {
                action.onItemClick(item, adapterPosition)
            }
        }

        //Fetch all user with the email given
        private fun fetchUser(item: UserReview) {
            FirebaseFirestore.getInstance().collection("user")
                .get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        if (document.id.contains(item.email)) {
                            val user: User = document.toObject(User::class.java)
                            username.text = user.username
                        }
                    }
                }
                .addOnFailureListener {
                }
        }
    }
}

