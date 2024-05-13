package com.example.mapping

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.loginsignupsql.DatabaseHelper
import com.example.mapping.databinding.ActivityLoginBinding
import com.example.mapping.databinding.ActivitySignupBinding

class SignupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding =ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseHelper = DatabaseHelper(this)

        binding.signupButton.setOnClickListener{
            val signupName =binding.signupName.text.toString()
            val signupEmail =binding.signupEmail.text.toString()
            val signupUsername =binding.signupUsername.text.toString()
            val signupPassword =binding.signupPassword.text.toString()
            signupDatabase(signupName,signupEmail,signupUsername,signupPassword)
        }
        binding.loginRedirect.setOnClickListener{
            val intent = Intent(this,LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

    }
    private fun signupDatabase(name: String,email: String,username: String, password:String){
        val insertedRowId =databaseHelper.insertUser(name,email,username,password)
        if (insertedRowId !=1L){
            Toast.makeText(this,"Signup Successful", Toast.LENGTH_SHORT).show()
            val intent = Intent(this,LoginActivity::class.java)
            startActivity(intent)
            finish()
        }else{
            Toast.makeText(this, "Signup Failed", Toast.LENGTH_SHORT).show()
        }
    }
}