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

class UserBookmarksAdapter(
    private val context: Context,
    private val userBookmark: List<UserBookmark>,
    private var clickListener: OnUserItemClickListenerBookmark,
) : RecyclerView.Adapter<UserBookmarksAdapter.ViewHolder>() {


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(R.layout.item_view_bookmark, parent, false)
        )

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.initialize(userBookmark[position], clickListener, context, holder)
    }

    override fun getItemCount(): Int {
        return userBookmark.size
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val ratingBar: RatingBar = itemView.findViewById(R.id.item_ratingBar)
        private val restaurantName: TextView = itemView.findViewById(R.id.item_title)
        private val imageUrl: ImageView = itemView.findViewById(R.id.item_image)
        private val bookmarkView: ImageView = itemView.findViewById(R.id.item_bookmark)
        private val location: TextView = itemView.findViewById(R.id.item_location)

        fun initialize(
            item: UserBookmark,
            action: OnUserItemClickListenerBookmark,
            context: Context,
            holder: ViewHolder,
        ) {
            restaurantName.text = item.restaurant

            //Change bookmark icon
            if (item.bookmark) {
                bookmarkView.setBackgroundResource(R.drawable.ic_baseline_bookmark_24)
            } else {
                bookmarkView.setBackgroundResource(R.drawable.ic_baseline_bookmark_border_24)
            }//Modify bookmark when clicked
            bookmarkView.setOnClickListener {   // To change bookmark dynamically on recyclerView
                if (!item.bookmark) {
                    item.bookmark = true
                    bookmarkView.setBackgroundResource(R.drawable.ic_baseline_bookmark_24)
                    modifyBookmark(
                        item.email,
                        item.restaurant,
                        item.bookmark,
                        item.imageUrl,
                        item.rating,
                        item.placeAddress
                    )
                } else {
                    item.bookmark = false
                    bookmarkView.setBackgroundResource(R.drawable.ic_baseline_bookmark_border_24)
                    modifyBookmark(
                        item.email,
                        item.restaurant,
                        item.bookmark,
                        item.imageUrl,
                        item.rating,
                        item.placeAddress
                    )

                }
            }

            if (item.imageUrl != "") {
                Picasso.with(context)
                    .load(
                        item.imageUrl
                    ).resize(750, 750).centerCrop().into(holder.imageUrl)
            }

            ratingBar.rating = item.rating.toFloat()
            ratingBar.setIsIndicator(true)

            location.text = item.placeAddress

            itemView.setOnClickListener {
                action.onItemClick(item, adapterPosition)
            }
        }
    }
}

//Modify bookmark info (add or remove)
private fun modifyBookmark(
    email: String,
    restaurant: String,
    bookmark: Boolean,
    imageUrl: String,
    rating: String,
    placeAddress: String,
) {

    val db = FirebaseFirestore.getInstance()
    val user = UserBookmark(
        email,
        bookmark,
        restaurant, imageUrl, rating, placeAddress
    )

    val ref2 = db.collection("user_bookmark")
        .document(user.email + "." + user.restaurant)  // To recognise every review on the DB

    ref2.set(user)
}


interface OnUserItemClickListenerBookmark {
    fun onItemClick(item: UserBookmark, position: Int)
}