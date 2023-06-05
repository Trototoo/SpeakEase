package com.example.speakease

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.speakease.Constants.IMAGE_PICK_REQUEST_CODE
import com.example.speakease.Constants.PROFILE_PATH
import com.example.speakease.Constants.USERS_PATH
import com.example.speakease.databinding.ActivitySetupProfileBinding
import com.example.speakease.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
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
        binding = ActivitySetupProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initFirebase()
        setupUI()
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
            startActivityForResult(intent, IMAGE_PICK_REQUEST_CODE)
        }

        binding.continueBtn02.setOnClickListener { updateProfile() }
    }

    private fun uploadImage(reference: StorageReference, uri: Uri, onSuccess: (String) -> Unit) {
        reference.putFile(uri).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                reference.downloadUrl.addOnCompleteListener { downloadUrlTask ->
                    if (downloadUrlTask.isSuccessful) {
                        val downloadUri = downloadUrlTask.result
                        val imageUrl = downloadUri.toString()
                        onSuccess(imageUrl)
                    }
                }
            }
        }
    }

    private fun updateProfile() {
        val name = binding.nameBox.text.toString()
        if (name.isEmpty()) {
            binding.nameBox.error = "Please type your name"
            return
        }

        dialog.show()

        selectedImage?.let {
            val reference = storage.reference.child(PROFILE_PATH).child(auth.uid!!)
            uploadImage(reference, it) { imageUrl ->
                saveUserToFirebase(name, imageUrl)
            }
        } ?: run {
            saveUserToFirebase(name, "No Image")
        }
    }

    private fun saveUserToFirebase(name: String, imageUrl: String) {
        val uid = auth.uid
        val phone = auth.currentUser?.phoneNumber
        val user = User(uid!!, name, phone, imageUrl)
        database.reference
            .child(USERS_PATH)
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

        if (requestCode == IMAGE_PICK_REQUEST_CODE && resultCode == RESULT_OK && data != null && data.data != null) {
            val uri = data.data!!
            val reference = storage.reference
                .child(PROFILE_PATH)
                .child("${Date().time}")

            uploadImage(reference, uri) { imageUrl ->
                val obj = HashMap<String, Any>()
                obj["image"] = imageUrl
                database.reference
                    .child(USERS_PATH)
                    .child(auth.uid!!)
                    .updateChildren(obj)
            }
            binding.imageView.setImageURI(data.data)
            selectedImage = data.data
        }
    }
}