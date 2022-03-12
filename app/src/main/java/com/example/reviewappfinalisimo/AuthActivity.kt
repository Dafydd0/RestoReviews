package com.example.reviewappfinalisimo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.dialog_view.view.*

enum class ProviderType {
    BASIC,
    GOOGLE
}


class AuthActivity : AppCompatActivity() {

    private val GOOGLE_SIGN_IN = 100

    private lateinit var signIpButton: Button
    private lateinit var loginButton: Button
    private lateinit var guestButton: Button
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var googleLoginButton: ImageButton


    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_ReviewAppFinalisimo)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        setup()
    }

    private fun setup() {
        supportActionBar?.hide()

        signIpButton = findViewById(R.id.signinbtn)
        loginButton = findViewById(R.id.loginbtn)
        guestButton = findViewById(R.id.guestbtn)
        email = findViewById(R.id.emailEditText)
        password = findViewById(R.id.passwordEditText)
        googleLoginButton = findViewById(R.id.google_login)


        signIpButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        loginButton.setOnClickListener { //Check if is a valid email and login into firebase
            if (email.text.isNotEmpty() && password.text.isNotEmpty() && email.text.contains("@")) {
                FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(
                        email.text.toString(),
                        password.text.toString()
                    ).addOnCompleteListener {

                        if (it.isSuccessful) {
                            showHome(it.result?.user?.email ?: "")
                        } else {
                            showAlertErrorAuth()
                        }
                    }
            } else {
                showAlertErrorFields()
            }
        }

        guestButton.setOnClickListener {
            showHome("guest_email")
        }


        googleLoginButton.setOnClickListener {

            val googleConf =
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id)).requestEmail()
                    .build()

            val googleClient = GoogleSignIn.getClient(this, googleConf)
            googleClient.signOut()
            startActivityForResult(googleClient.signInIntent, GOOGLE_SIGN_IN)
        }
    }

    private fun showHome(email: String) {
        val intent = Intent(this, GoogleMapsActivity::class.java).apply {
            putExtra("email", email)
        }
        startActivity(intent)
    }

    //We need to override this method to enable the Google Sign In, this login method will just work on debug mode, because in release mode we need a Google Developer account which costs 25$
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Toast.makeText(this, "We are trying to access to Google Services", Toast.LENGTH_LONG).show()

        if (requestCode == GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {

                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                    FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnCompleteListener {

                            if (it.isSuccessful) {
                                showHome(account.email ?: "")
                            } else {
                                showAlertErrorAuth()
                                Toast.makeText(this,
                                    "Error, fail on get credentials",
                                    Toast.LENGTH_LONG).show()
                            }
                        }
                }
            } catch (e: ApiException) {
                showAlertErrorAuth()
            }
        }
    }

    //Functions to show the different prompts
    private fun showAlertErrorAuth() {

        val view = View.inflate(this, R.layout.dialog_view_error_auth, null)

        val builder = AlertDialog.Builder(this)
        builder.setView(view)


        val dialog: AlertDialog = builder.create()
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)

        view.btn_confirm.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun showAlertErrorFields() {

        val view = View.inflate(this, R.layout.dialog_view_error_fields, null)

        val builder = AlertDialog.Builder(this)
        builder.setView(view)


        val dialog: AlertDialog = builder.create()
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)

        view.btn_confirm.setOnClickListener {
            dialog.dismiss()
        }
    }
}