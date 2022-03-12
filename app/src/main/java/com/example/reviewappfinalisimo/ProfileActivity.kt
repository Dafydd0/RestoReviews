package com.example.reviewappfinalisimo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var editUsernameButton: Button
    private lateinit var logOutButton: Button
    private lateinit var usernameET: EditText
    lateinit var email: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        //Get parameters
        val bundle: Bundle? = intent.extras
        email = bundle?.getString("email").toString()
        setup()
    }

    private fun setup() {

        usernameET = findViewById(R.id.usernameET)
        editUsernameButton = findViewById(R.id.changeUsernameBtn)
        logOutButton = findViewById(R.id.logOutBtn)

        //Fetch user in DB and modify the edit text field to display the username
        fetchUser()

        editUsernameButton.setOnClickListener {
            if (usernameET.text.isNotEmpty()) {
                addUser(email, usernameET.text.toString())
                Toast.makeText(this, "You have modified you username correctly", Toast.LENGTH_SHORT)
                    .show()
                val intent = Intent(this, GoogleMapsActivity::class.java).apply {
                    putExtra("email", email)
                    putExtra("displayWithoutZoom", true)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "You need to add an username", Toast.LENGTH_SHORT).show()
            }
        }
        logOutButton.setOnClickListener { //Lot out from the app
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, AuthActivity::class.java)
            startActivity(intent)
        }
    }

    //Fetch user in DB and modify the edit text field to display the username
    private fun fetchUser() {
        FirebaseFirestore.getInstance().collection("user")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    if (document.id.contains(email)) {
                        val user: User = document.toObject(User::class.java)
                        usernameET.setText(user.username)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error trying to fetch user", Toast.LENGTH_LONG).show()
            }
    }


    //Add username to an user
    private fun addUser(email: String, username: String) {

        val uid = FirebaseAuth.getInstance().uid ?: ""
        val db = FirebaseFirestore.getInstance()
        val user = User(email, uid, username)

        val ref =
            db.collection("user").document(user.email)  // To recognise every review on the DB

        ref.set(user)
    }
}