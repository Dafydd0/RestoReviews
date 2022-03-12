package com.example.reviewappfinalisimo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.reviewappfinalisimo.Common.Common
import com.example.reviewappfinalisimo.Model.PlaceDetail
import com.example.reviewappfinalisimo.Remote.IGoogleAPIService
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_view_place.*
import kotlinx.android.synthetic.main.dialog_view.view.btn_confirm
import kotlinx.android.synthetic.main.dialog_view_existing_review.view.*
import retrofit2.Call
import retrofit2.Response

class ViewPlaceActivity : AppCompatActivity() {

    private lateinit var mService: IGoogleAPIService
    var mPlace: PlaceDetail? = null

    private var db = FirebaseFirestore.getInstance()

    //Instead of asking googleApi, we collect the place info as bundle
    private var bookmark = false
    private lateinit var email: String
    private lateinit var placeName: String
    private lateinit var placeAddress: String
    private lateinit var rating: String
    private var calledByReview = false

    private lateinit var bookmarkView: ImageView
    private lateinit var showOnMapButton: Button
    private lateinit var postReviewButton: Button
    private lateinit var viewAllReviewsButton: Button
    private lateinit var image: String
    private lateinit var placeUrl: String

    private val placeKey = "YOUR_API_KEY"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_place)

        val bundle: Bundle? = intent.extras
        email = bundle?.getString("email").toString()
        bookmark = bundle?.getBoolean("bookmark")!!
        calledByReview =
            bundle.getBoolean("calledByReview") //True -> called by review activity, False -> called by bookmark recyclerView

        bookmarkView = findViewById(R.id.place_bookmark)
        showOnMapButton = findViewById(R.id.btn_show_map)
        postReviewButton = findViewById(R.id.btn_post_review)
        viewAllReviewsButton = findViewById(R.id.btn_all_reviews)

        //Init service
        mService = Common.googleApiService

        // If calledByReview = true
        // Google data to local variables
        // If calledByReview = false
        // Local variables to google data

        if (calledByReview) {
            //Set empty for all textView
            place_name.text = ""
            place_address.text = ""

            //Load photo of place
            if (Common.currentResult!!.photos != null && Common.currentResult!!.photos!!.isNotEmpty()) {
                Picasso.with(this)
                    .load(
                        getPhotoOfPlace(
                            Common.currentResult!!.photos!![0].photo_reference!!,
                            1000
                        )
                    ).resize(750, 750).centerInside()
                    .into(photo)
                image = getPhotoOfPlace(
                    Common.currentResult!!.photos!![0].photo_reference!!,
                    1000
                )
            }
            //Load rating
            if (Common.currentResult!!.rating != null) {
                rating_bar.rating =
                    Common.currentResult!!.rating.toFloat()
                rating = rating_bar.rating.toString()
                rating_bar.setIsIndicator(true)

            } else
                rating_bar.visibility = View.GONE


            //Use service to fetch address and name
            mService.getDetailPlace(getPlaceDetailUrl(Common.currentResult!!.place_id!!))
                .enqueue(object : retrofit2.Callback<PlaceDetail> {
                    override fun onResponse(
                        call: Call<PlaceDetail>?,
                        response: Response<PlaceDetail>?,
                    ) {
                        mPlace = response!!.body()

                        place_address.text = mPlace!!.result!!.formatted_address
                        place_name.text = mPlace!!.result!!.name
                        placeAddress = place_address.text.toString()
                        placeName = place_name.text.toString()
                        placeUrl = mPlace!!.result!!.url.toString()

                    }

                    override fun onFailure(call: Call<PlaceDetail>?, t: Throwable?) {
                        Toast.makeText(baseContext, "" + t!!.message, Toast.LENGTH_SHORT).show()
                    }
                })
        } else {
            placeName = bundle.getString("restaurant").toString()
            placeAddress = bundle.getString("placeAddress").toString()
            image = bundle.getString("imageUrl").toString()
            rating = bundle.getString("rating").toString()
            placeUrl = bundle.getString("placeUrl").toString()

            place_address.text = placeAddress
            place_name.text = placeName
            rating_bar.rating = rating.toFloat()
            rating_bar.setIsIndicator(true)

            Picasso.with(this)
                .load(
                    image
                ).resize(750, 750).centerInside().into(photo)
        }

        // Manage the bookmark icon and modify it on the database if changes
        if (bookmark) {
            bookmarkView.setBackgroundResource(R.drawable.ic_baseline_bookmark_24)
        } else {
            bookmarkView.setBackgroundResource(R.drawable.ic_baseline_bookmark_border_24)
        }

        bookmarkView.setOnClickListener {
            if (email != "guest_email") {
                if (bookmark) {
                    bookmark = false
                    bookmarkView.setBackgroundResource(R.drawable.ic_baseline_bookmark_border_24)
                    modifyBookmark()
                } else {
                    bookmark = true
                    bookmarkView.setBackgroundResource(R.drawable.ic_baseline_bookmark_24)
                    modifyBookmark()
                }
            } else {
                showAlertAuthBookmark()
            }
        }

        // Open google maps activity
        showOnMapButton.setOnClickListener {
            val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(placeUrl))
            startActivity(mapIntent)
        }

        // Open the activity which shows all the restaurant reviews
        viewAllReviewsButton.setOnClickListener {
            val intent = Intent(this, RestaurantReviewsRecyclerViewActivity::class.java).apply {
                putExtra("email", email)
                putExtra("restaurant", placeName)
            }
            startActivity(intent)
        }

        // Open the activity which enables you to post a review
        postReviewButton.setOnClickListener {

            if (email == "guest_email") {
                //Can not post a review
                showAlertAuth()
            } else {
                foo(placeName) { result ->

                    if (result) { //The user has already posted a review
                        showAlertExistingReview(userReview2)
                    } else {
                        val intent = Intent(this, ReviewActivity::class.java).apply {
                            putExtra("email", email)
                            putExtra("restaurant", placeName)
                            putExtra("placeAddress", placeAddress)
                            putExtra("placeUrl", placeUrl)
                            putExtra("edit", false)
                        }
                        startActivity(intent)
                    }
                }
            }
        }
    }


    private fun getPlaceDetailUrl(placeId: String): String {
        val url = StringBuilder("https://maps.googleapis.com/maps/api/place/details/json")
        url.append("?placeid=$placeId")
        url.append("&key=$placeKey")
        return url.toString()
    }

    private fun getPhotoOfPlace(photoReference: String, maxWidth: Int): String {
        val url = StringBuilder("https://maps.googleapis.com/maps/api/place/photo")
        url.append("?maxwidth=$maxWidth")
        url.append("&photoreference=$photoReference")
        url.append("&key=$placeKey")
        return url.toString()
    }

    private fun modifyBookmark() {
        var user = UserBookmark(email, bookmark, placeName, image, rating, placeAddress, placeUrl)
        val ref = db.collection("user_bookmark")
            .document(user.email + "." + user.restaurant)
        ref.set(user)
    }

    // Check if an user has a review of a restaurant
    private lateinit var userReview2: UserReview
    private fun foo(restaurant: String, callback: (Boolean) -> Unit) {
        var exist = false
        FirebaseFirestore.getInstance().collection("user_review")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    if (document.id.contains(email) && document.id.contains(restaurant)) {
                        userReview2 = document.toObject(UserReview::class.java)
                        exist = true
                    }
                }
                callback.invoke(exist)
            }
    }

    private fun showAlertAuth() {
        val view = View.inflate(this, R.layout.dialog_view, null)

        val builder = AlertDialog.Builder(this)
        builder.setView(view)

        val dialog: AlertDialog = builder.create()

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)

        view.btn_confirm.setOnClickListener {
            val intent = Intent(this, AuthActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showAlertAuthBookmark() {
        val view = View.inflate(this, R.layout.dialog_view_bookmark, null)

        val builder = AlertDialog.Builder(this)
        builder.setView(view)

        val dialog: AlertDialog = builder.create()

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)

        view.btn_confirm.setOnClickListener {
            val intent = Intent(this, AuthActivity::class.java)
            startActivity(intent)
        }
    }

    //Shown prompt of existing review, if user wants to overwrite it, we need to call ReviewActivity with all the info of the already existing review
    private fun showAlertExistingReview(userReview: UserReview) {

        val view = View.inflate(this, R.layout.dialog_view_existing_review, null)

        val builder = AlertDialog.Builder(this)
        builder.setView(view)


        val dialog: AlertDialog = builder.create()
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)

        view.btn_confirm.setOnClickListener {
            val intent = Intent(this, ReviewActivity::class.java).apply {
                putExtra("email", email)
                putExtra("restaurant", placeName)
                putExtra("rating", userReview.rating)
                putExtra("imageUrl", userReview.imageUrl)
                putExtra("comment", userReview.comment)
                putExtra("placeAddress", userReview.location)
                putExtra("edit", true)
            }
            startActivity(intent)
        }
        view.btn_close.setOnClickListener {
            dialog.dismiss()
        }
    }
}