package com.example.speakease

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.speakease.databinding.ActivitySetupProfileBinding
import com.example.speakease.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.util.Date

class SetupProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private lateinit var dialog: ProgressDialog
    private var selectedImage: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_profile)
    }

    private fun initFirebase() {
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
    }

    private fun setupUI() {
        supportActionBar?.hide()

        dialog = ProgressDialog(this).apply {
            setMessage("Updating Profile...")
            setCancelable(false)
        }

        binding.imageView.setOnClickListener {
            val intent = Intent().apply {
                action = Intent.ACTION_GET_CONTENT
                type = "image/*"
            }
            startActivityForResult(intent, 45)
        }

        binding.continueBtn02.setOnClickListener { updateProfile() }
    }

    private fun updateProfile() {
        val name = binding.nameBox.text.toString()
        if (name.isEmpty()) {
            binding.nameBox.error = "Please type your name"
            return
        }

        dialog.show()

        selectedImage?.let {
            uploadImageToFirebase(it, name)
        } ?: run {
            saveUserToFirebase(name, "No Image")
        }
    }

    private fun uploadImageToFirebase(uri: Uri, name: String) {
        val reference = storage.reference.child("Profile").child(auth.uid!!)
        reference.putFile(uri).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                reference.downloadUrl.addOnCompleteListener { downloadUrlTask ->
                    if (downloadUrlTask.isSuccessful) {
                        val downloadUri = downloadUrlTask.result
                        val imageUrl = downloadUri.toString()

                        saveUserToFirebase(name, imageUrl)
                    }
                }
            }
        }
    }

    private fun saveUserToFirebase(name: String, imageUrl: String) {
        val uid = auth.uid
        val phone = auth.currentUser?.phoneNumber
        val user = User(uid!!, name, phone, imageUrl)
        database.reference
            .child("users")
            .child(uid)
            .setValue(user)
            .addOnCompleteListener {
                dialog.dismiss()
                navigateToMainActivity()
            }
    }

    private fun navigateToMainActivity() {
        Intent(this, MainActivity::class.java).also {
            startActivity(it)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data != null && data.data != null) {
            val uri = data.data
            val reference = storage.reference
                .child("Profile")
                .child("${Date().time} ")

            reference.putFile(uri!!).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    reference.downloadUrl.addOnCompleteListener { uri ->
                        val filePath = uri.toString()
                        val obj = HashMap<String, Any>()
                        obj["image"] = filePath
                        database.reference
                            .child("users")
                            .child(auth.uid!!)
                            .updateChildren(obj)
                    }
                }
            }
            binding.imageView.setImageURI(data.data)
            selectedImage = data.data
        }
    }
}