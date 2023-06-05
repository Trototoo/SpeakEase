package com.example.speakease

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.speakease.databinding.ActivityVerificationBinding
import com.google.firebase.auth.FirebaseAuth

class VerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerificationBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        checkIfUserIsAlreadyLogged()
        hideActionBar()
        setContinueButtonOnClick()
    }

    private fun checkIfUserIsAlreadyLogged() {
        if (auth.currentUser != null) {
            navigateToMainActivity()
        } else {
            binding.editNumber.requestFocus()
        }
    }

    private fun hideActionBar() {
        supportActionBar?.hide()
    }

    private fun setContinueButtonOnClick() {
        binding.continueBtn.setOnClickListener {
            val phoneNumber = binding.editNumber.text.toString()
            navigateToOTPActivity(phoneNumber)
        }
    }

    private fun navigateToMainActivity() {
        Intent(this, MainActivity::class.java).also {
            startActivity(it)
            finish()
        }
    }

    private fun navigateToOTPActivity(phoneNumber: String) {
        Intent(this, OTPActivity::class.java).also {
            it.putExtra("phoneNumber", phoneNumber)
            startActivity(it)
        }
    }
}