package com.example.reviewappfinalisimo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class UserReviewsAdapter(private val context: Context,
                         private val userReviews: List<UserReview>,
                         private var clickListener: OnUserItemClickListenerReview
                  )
    : RecyclerView.Adapter<UserReviewsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_view, parent, false))

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.initialize(userReviews[position], clickListener, context, holder)
    }

    override fun getItemCount(): Int {
        return userReviews.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val ratingBar: RatingBar = itemView.findViewById(R.id.item_ratingBar)
        private val restaurantName: TextView = itemView.findViewById(R.id.item_title)
        private val imageUrl: ImageView = itemView.findViewById(R.id.item_image)
        private val comment: TextView = itemView.findViewById(R.id.item_comment)
        private val location: TextView = itemView.findViewById(R.id.item_location)

        fun initialize(item: UserReview, action: OnUserItemClickListenerReview, context: Context, holder: ViewHolder) {
            ratingBar.rating = item.rating.toFloat()
            ratingBar.setIsIndicator(true)
            restaurantName.text = item.restaurant
            comment.text = item.comment
            location.text = item.location

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
    }
}

interface OnUserItemClickListenerReview {
    fun onItemClick(item: UserReview, position: Int)
}