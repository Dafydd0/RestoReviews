package com.example.reviewappfinalisimo

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_review.*
import kotlinx.android.synthetic.main.dialog_view.view.*
import java.util.*

class ReviewActivity : AppCompatActivity() {

    private lateinit var ratingBar: RatingBar
    private lateinit var restaurantText: TextView
    private lateinit var commentText: EditText
    private lateinit var uploadButton: Button
    private lateinit var finishButton: Button
    private lateinit var imageView: ImageView

    private lateinit var email: String
    private lateinit var restaurant: String
    private lateinit var rating: String
    private lateinit var imageUrl: String
    private lateinit var comment: String
    private lateinit var placeAddress: String
    private var editBool: Boolean = false

    private val channelID = "channelID"
    private val channelName = "channelName"
    private val notificationId = 0
    private lateinit var notification: Notification
    private lateinit var notificationManager: NotificationManagerCompat

    private var isEdit = false  // Check if is a review edit or a new one
    private var bookmark = false  // Modify in a future
    private var photoUploaded = false
    private lateinit var database: DatabaseReference

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        //Get all parameters
        val bundle: Bundle? = intent.extras
        email = bundle?.getString("email").toString()
        restaurant = bundle?.getString("restaurant").toString()
        rating = bundle?.getString("rating").toString()
        imageUrl = bundle?.getString("imageUrl").toString()
        comment = bundle?.getString("comment").toString()
        bookmark = bundle?.getBoolean("bookmark")!!
        placeAddress = bundle.getString("placeAddress").toString()
        editBool = bundle.getBoolean("edit")!! //Edit = true, is a modify of a review
        setup()
    }

    private fun setup() {

        restaurantText = findViewById(R.id.restaurantNameTextView)
        ratingBar = findViewById(R.id.ratingBar)
        commentText = findViewById(R.id.commentEditText)
        uploadButton = findViewById(R.id.uploadPhotoButton)
        finishButton = findViewById(R.id.finishButton)
        imageView = findViewById(R.id.image)
        database = Firebase.database.reference

        restaurantText.text = restaurant

        if (editBool) { //If it's an edit, configure the previous values
            configIfEdit()
        }

        finishButton.setOnClickListener { //Upload the review and upload the photo to firebase

            rating = ratingBar.rating.toString()
            comment = commentText.text.toString()
            Toast.makeText(this,
                "Your review is being posted, this may take a few seconds",
                Toast.LENGTH_LONG).show()
            if (!isEdit) {
                if (photoUploaded) {
                    uploadImageToFirebaseStorage()
                } else {
                    writeNewUserWithoutImage()
                }
            } else {   // Is an edit
                if (photoUploaded) {
                    uploadImageToFirebaseStorage()
                } else { //No upload photo clicked
                    if (imageUrl != "null" || imageUrl != "") {
                        writeNewUserWithImage(imageUrl)
                    } else {  //No upload photo clicked but was empty before
                        writeNewUserWithoutImage()
                    }
                }
            }
        }

        uploadButton.setOnClickListener {

            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/"
            startActivityForResult(intent, 0)
        }
    }

    private fun configIfEdit() { // Config the activity in case is being editing the review

        if (rating != "" || rating != null) {
            ratingBar.rating = rating.toFloat()

        }
        commentText.setText(comment)
        isEdit = true
        if (imageUrl != "") {
            Picasso.with(this)
                .load(
                    imageUrl
                ).resize(500, 500).centerInside().into(image)
        }
    }

    private fun uploadImageToFirebaseStorage() {

        if (selectedPhotoUri != null) {
            val filename = UUID.randomUUID().toString()
            val ref = FirebaseStorage.getInstance().getReference("/images/$filename")

            ref.putFile(selectedPhotoUri!!)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener {
                        writeNewUserWithImage(it.toString())
                    }
                }
                .addOnFailureListener {
                }
        }
    }

    // Display image on the activity
    private var selectedPhotoUri: Uri? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            // Proceed and check what the selected image was
            selectedPhotoUri = data.data
            // Show the photo on the imageView
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedPhotoUri)
            val bitmapDrawable = BitmapDrawable(bitmap)

            Glide.with(this)
                .load(bitmapDrawable).override(500, 500).centerInside().into(imageView)
            photoUploaded = true
        }
    }


    private fun showAlertSuccessReview() {

        val view = View.inflate(this, R.layout.dialog_view_success_review, null)

        val builder = AlertDialog.Builder(this)
        builder.setView(view)


        val dialog: AlertDialog = builder.create()
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)

        view.btn_confirm.setOnClickListener {
            val intent = Intent(this, GoogleMapsActivity::class.java).apply {
                putExtra("email", email)
                putExtra("displayWithoutZoom", true)
            }
            startActivity(intent)
        }
    }

    private fun showAlertSuccessModifiedReview() {

        val view = View.inflate(this, R.layout.dialog_view_success_modified_review, null)

        val builder = AlertDialog.Builder(this)
        builder.setView(view)


        val dialog: AlertDialog = builder.create()
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)

        view.btn_confirm.setOnClickListener {
            val intent = Intent(this, GoogleMapsActivity::class.java).apply {
                putExtra("email", email)
                putExtra("displayWithoutZoom", true)
            }
            startActivity(intent)
        }
    }

    //Write user without image and send notification
    private fun writeNewUserWithoutImage() {

        val uid = FirebaseAuth.getInstance().uid ?: ""
        val user = UserReview(email, uid, restaurant, rating, comment, placeAddress)
        val ref2 = db.collection("user_review")
            .document(user.email + "." + user.restaurant)  // To recognise every review on the DB

        ref2.set(user)
        getAllUsers()

        createNotificationChannel()

        if (isEdit) {
            showAlertSuccessModifiedReview()
            createNotification("Review modified successfully",
                "Your review of $restaurant has been modified successfully")
        } else {
            showAlertSuccessReview()
            createNotification("Review posted successfully",
                "Your review of $restaurant has been posted successfully")
        }
        notificationManager.notify(notificationId, notification)
    }

    //Write user with image and send notification
    private fun writeNewUserWithImage(imageUrl: String) {

        val uid = FirebaseAuth.getInstance().uid ?: ""
        val user = UserReview(email, uid, restaurant, rating, comment, placeAddress, imageUrl)
        val ref2 = db.collection("user_review")
            .document(user.email + "." + user.restaurant)  // To recognise every review on the DB

        ref2.set(user)
        getAllUsers()
        photoUploaded = false

        createNotificationChannel()

        if (isEdit) {
            showAlertSuccessModifiedReview()
            createNotification("Review modified successfully",
                "Your review of $restaurant has been modified successfully")
        } else {
            showAlertSuccessReview()
            createNotification("Review posted successfully",
                "Your review of $restaurant has been posted successfully")
        }
        notificationManager.notify(notificationId, notification)
    }

    private fun getAllUsers() {

        val listUserReviews: MutableList<UserReview> = mutableListOf()
        db.collection("user_review").get().addOnSuccessListener { result ->
            for (document in result) {
                val userReview: UserReview = document.toObject(UserReview::class.java)
                listUserReviews.add(userReview)
            }
        }
    }

    //Functions to create the channel of the notification and the notification
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(channelID, channelName, importance).apply {
                enableLights(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }


    private fun createNotification(title: String, text: String) {
        notification = NotificationCompat.Builder(this, channelID).also {
            it.setContentTitle("$title")
            it.setContentText("$text")
            it.setSmallIcon(R.drawable.ic_baseline_rate_review_24)
            it.priority = Notification.PRIORITY_MAX
            it.color = Color.BLUE
        }.build()

        notificationManager = NotificationManagerCompat.from(this)
    }
}
