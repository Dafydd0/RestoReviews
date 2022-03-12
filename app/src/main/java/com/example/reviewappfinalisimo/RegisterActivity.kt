package com.example.reviewappfinalisimo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.dialog_view.view.*

class RegisterActivity : AppCompatActivity() {

    private lateinit var signIpButton: Button
    private lateinit var email: EditText
    private lateinit var repeatEmail: EditText
    private lateinit var password: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        setup()
    }

    private fun setup() {

        signIpButton = findViewById(R.id.signinbtn)
        email = findViewById(R.id.emailEditText)
        repeatEmail = findViewById(R.id.repeatEmailEditText)
        password = findViewById(R.id.passwordEditText)

        signIpButton.setOnClickListener {
            //Check that email is valid and email and repeat email fields match and register an user in firebase
            if (email.text.isNotEmpty() && password.text.isNotEmpty() && email.text.contains("@") && repeatEmail.text.contains("@")) {
                if (email.text.toString() == repeatEmail.text.toString()) {
                    FirebaseAuth.getInstance()
                        .createUserWithEmailAndPassword(
                            email.text.toString(),
                            password.text.toString()
                        ).addOnCompleteListener {

                            if (it.isSuccessful) {
                                showHome(it.result?.user?.email ?: "")
                            } else {
                                showAlertErrorAuth()
                            }
                        }
                }else{
                    showAlertErrorMismatchEmails()
                }
            }else{
                showAlertErrorFields()
            }
        }
    }

    private fun showHome(email: String) {
        val intent = Intent(this, GoogleMapsActivity::class.java).apply {
            putExtra("email", email)
        }
        startActivity(intent)
    }

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

    private fun showAlertErrorMismatchEmails() {

        val view = View.inflate(this, R.layout.dialog_view_error_mismatchemail, null)

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