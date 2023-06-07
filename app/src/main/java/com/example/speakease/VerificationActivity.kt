package com.example.speakease

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.speakease.databinding.ActivityVerificationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber

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

        try {
            // You should specify the region code.
            // If you're not sure about the region, you can just use "ZZ".
            val numberProto: Phonenumber.PhoneNumber = phoneUtil.parse(phoneNumber, "ZZ")
            return phoneUtil.isValidNumber(numberProto)
        } catch (e: NumberParseException) {
            println("NumberParseException was thrown: $e")
        }

        return false
    }
}