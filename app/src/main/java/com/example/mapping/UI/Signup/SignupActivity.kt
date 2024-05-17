package com.example.mapping.UI.Signup

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mapping.UI.Home.MainActivity
import com.example.mapping.SignupModel
import com.example.mapping.UI.Login.LoginActivity
import com.example.mapping.USERS
import com.example.mapping.databinding.ActivitySignupBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.database

class SignupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding
    private val auth = Firebase.auth
    private val ref = Firebase.database.reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onClick()
    }

    private fun onClick() {
        binding.signupButton.setOnClickListener {
            validate(
                binding.signupName.text.toString(),
                binding.signupEmail.text.toString(),
                binding.signupUsername.text.toString(),
                binding.signupPassword.text.toString(),
                binding.signupRePassword.text.toString()
            )

        }
        binding.loginRedirect.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun validate(
        name: String,
        email: String,
        username: String,
        password: String,
        confPassword: String
    ) {
        if (name.isEmpty()) {
            binding.signupName.error = "Required"
        } else if (email.isEmpty()) {
            binding.signupEmail.error = "Required"
        } else if (username.isEmpty()) {
            binding.signupUsername.error = "Required"
        } else if (password.isEmpty()) {
            binding.signupPassword.error = "Required"
        } else if (confPassword.isEmpty()) {
            binding.signupRePassword.error = "Required"
        } else if (!password.equals(confPassword)) {
            binding.signupRePassword.error = "Not Matched"
        } else {
            Signup(name, email, username, password)
        }

    }

    private fun Signup(name: String, email: String, username: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                sendDataToDataBase(name, email, username)
            }
            .addOnFailureListener {
                Toast.makeText(this, "User Already Exists", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendDataToDataBase(name: String, email: String, username: String) {
        ref.child(USERS).child(auth.uid.toString())
            .setValue(SignupModel(name, username, email, auth.uid.toString()))
            .addOnSuccessListener {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }.addOnFailureListener {
                Toast.makeText(this,it.message, Toast.LENGTH_SHORT).show()
            }
    }
}