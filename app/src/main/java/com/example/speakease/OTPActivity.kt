package com.example.speakease

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.example.speakease.Constants.USERS_PATH
import com.example.speakease.databinding.ActivityOtpactivityBinding
import com.example.speakease.model.User
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.concurrent.TimeUnit

class OTPActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOtpactivityBinding
    private lateinit var verificationId: String
    private lateinit var auth: FirebaseAuth
    private lateinit var dialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpactivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupProgressDialog()
        hideActionBar()

        auth = FirebaseAuth.getInstance()

        val phoneNumber = intent.getStringExtra("phoneNumber") ?: ""
        binding.phoneLble.text = "Verify $phoneNumber"

        verifyPhoneNumber(phoneNumber)
        handleOtpCompletion()
    }

    private fun setupProgressDialog() {
        dialog = ProgressDialog(this@OTPActivity).apply {
            setMessage("Sending OTP...")
            setCancelable(false)
            show()
        }
    }

    private fun hideActionBar() {
        supportActionBar?.hide()
    }

    private fun verifyPhoneNumber(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this@OTPActivity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(p0: PhoneAuthCredential) {
                    // TODO("Not yet implemented")
                }

                override fun onVerificationFailed(p0: FirebaseException) {
                    // TODO("Not yet implemented")
                }

                override fun onCodeSent(
                    verifyId: String,
                    forceResendingToken: PhoneAuthProvider.ForceResendingToken
                ) {
                    super.onCodeSent(verifyId, forceResendingToken)
                    dialog.dismiss()
                    verificationId = verifyId
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
                    binding.otpView.requestFocus()
                }
            }).build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun handleOtpCompletion() {
        binding.otpView.setOtpCompletionListener { otp ->
            val credential = PhoneAuthProvider.getCredential(verificationId, otp)
            auth.signInWithCredential(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    checkUserProfile()
                } else {
                    Toast.makeText(this@OTPActivity, "Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance().getReference(USERS_PATH)
        val userRef = database.child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val user = dataSnapshot.getValue(User::class.java)
                    if (user != null) {
                        if (!user.name.isNullOrEmpty() && !user.profileImage.isNullOrEmpty()) {
                            startNewActivity(MainActivity::class.java)
                        } else {
                            startNewActivity(SetupProfileActivity::class.java)
                        }
                    }
                } else {
                    startNewActivity(SetupProfileActivity::class.java)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle possible errors.
                Toast.makeText(this@OTPActivity, "Failed to read value.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun startNewActivity(clazz: Class<*>) {
        Intent(this@OTPActivity, clazz).apply {
            startActivity(this)
            finishAffinity()
        }
    }
}
