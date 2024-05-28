package com.example.speakease

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.speakease.databinding.ActivityVerificationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber

class VerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerificationBinding
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            if (isValidPhoneNumber(phoneNumber)) {
                navigateToOTPActivity(phoneNumber)
            } else {
                Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show()
            }
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

    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        val phoneUtil = PhoneNumberUtil.getInstance()

        return try {
            val numberProto: Phonenumber.PhoneNumber = phoneUtil.parse(phoneNumber, "ZZ")
            phoneUtil.isValidNumber(numberProto)
        } catch (e: NumberParseException) {
            false
        }
    }
}