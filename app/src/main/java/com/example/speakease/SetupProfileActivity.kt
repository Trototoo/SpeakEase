package com.example.speakease

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.speakease.Constants.IMAGE_PICK_REQUEST_CODE
import com.example.speakease.databinding.ActivitySetupProfileBinding
import com.example.speakease.repositories.FirebaseRepository

class SetupProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupProfileBinding
    private lateinit var dialog: ProgressDialog
    private var selectedImage: Uri? = null
    private val repository = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        supportActionBar?.hide()

        dialog = ProgressDialog(this).apply {
            setMessage("Updating Profile...")
            setCancelable(false)
        }

        binding.imageView.setOnClickListener { pickImage() }
        binding.continueBtn02.setOnClickListener { updateProfile() }
    }

    private fun pickImage() {
        val intent = Intent().apply {
            action = Intent.ACTION_GET_CONTENT
            type = "image/*"
        }
        startActivityForResult(intent, IMAGE_PICK_REQUEST_CODE)
    }

    private fun updateProfile() {
        val name = binding.nameBox.text.toString()
        if (name.isEmpty()) {
            binding.nameBox.error = "Please type your name"
            return
        }

        dialog.show()

        selectedImage?.let {
            val reference = repository.getUserProfileImageReference()
            repository.uploadImage(reference, it,
                onSuccess = { imageUrl -> saveUserToFirebase(name, imageUrl) },
                onFailure = { exception ->
                    dialog.dismiss()
                })
        } ?: saveUserToFirebase(name, "No Image")
    }

    private fun saveUserToFirebase(name: String, imageUrl: String) {
        repository.updateUserProfile(name, imageUrl,
            onSuccess = {
                dialog.dismiss()
                navigateToMainActivity()
            },
            onFailure = { exception ->
                dialog.dismiss()
            })
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
            binding.imageView.setImageURI(uri)
            selectedImage = uri
        }
    }
}
