package com.example.mapping.UI.Profile

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.mapping.R
import com.example.mapping.SignupModel
import com.example.mapping.UI.CrackReporting.cameraCapture
import com.example.mapping.UI.Home.MainActivity
import com.example.mapping.UI.Login.LoginActivity
import com.example.mapping.databinding.ActivityProfileBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.AdditionalUserInfo
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.values

class ProfileActivity : AppCompatActivity() {
    var pickedPhoto: Uri? = null
    var pickedBitMap: Bitmap? = null
    private lateinit var binding: ActivityProfileBinding
    private var fileImageUser: Uri? = null
    private lateinit var database:DatabaseReference
    private val auth = Firebase.auth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onClick()
        database=FirebaseDatabase.getInstance().reference
        retrieve()
    }
    private fun retrieve(){
        val myReference=database.child("users").child(auth.uid.toString())
        myReference.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val modelUser = dataSnapshot.getValue(SignupModel::class.java)
                bindingData(modelUser!!)
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                println("Failed to read value: ${error.toException()}")
            }
        })
    }
    private fun bindingData(userInfo: SignupModel){
        binding.userTextView.text=userInfo.userName
        binding.emailTextView.text=userInfo.email
        binding.nameTextView.text=userInfo.name
        binding.idTextView.text=userInfo.userId
        //diaa
    }

    private fun onClick() {
        binding.map1.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.camera1.setOnClickListener {
            val intent = Intent(this, cameraCapture::class.java)
            startActivity(intent)
            finish()
        }
        binding.profileImage.setOnClickListener{
            openGallery()
        }
        binding.logoutButton.setOnClickListener {
            auth.signOut()
            // After logout, redirect to the login activity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Close the current activity
        }

    }
    private fun openGallery() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        resultLauncher.launch(intent)
    }
    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val data: Intent? = it.data
                if (data != null) {
                    fileImageUser = data.data
                    binding.profileImage.setImageURI(fileImageUser)
                } else {
                    Toast.makeText(this, "Something went wrong try again", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }




    /*fun pickPhoto(view: View) {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) { // izin alınmadıysa
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                1
            )
        } else {
            val galeriIntext =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galeriIntext, 2)
        }
    }//diaa
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1) {
            if (grantResults.size > 0  && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val galeriIntext = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(galeriIntext,2)
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 2 && resultCode == Activity.RESULT_OK && data != null) {
            pickedPhoto = data.data
            if (pickedPhoto != null) {
                if (Build.VERSION.SDK_INT >= 28) {
                    val source = ImageDecoder.createSource(this.contentResolver,pickedPhoto!!)
                    pickedBitMap = ImageDecoder.decodeBitmap(source)
                    //profileImage.setImageBitmap(pickedBitMap)
                }
                else {
                    pickedBitMap = MediaStore.Images.Media.getBitmap(this.contentResolver,pickedPhoto)
                    //profileImage.setImageBitmap(pickedBitMap)
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }*/
}