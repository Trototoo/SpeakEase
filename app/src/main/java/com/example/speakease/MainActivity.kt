package com.example.speakease

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.speakease.adapter.UserAdapter
import com.example.speakease.databinding.ActivityMainBinding
import com.example.speakease.repositories.FirebaseRepository

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var usersAdapter: UserAdapter
    private lateinit var dialog: ProgressDialog
    private val repository = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initialize()
        setupUsersList()
        setUserOnlineStatus()
    }

    private fun initialize() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dialog = ProgressDialog(this).apply {
            setMessage("Uploading Image...")
            setCancelable(false)
        }

        usersAdapter = UserAdapter(arrayListOf())
        binding.mRec.layoutManager = GridLayoutManager(this, 2)
        binding.mRec.adapter = usersAdapter
    }

    private fun setupUsersList() {
        repository.getUsers(
            onSuccess = { users ->
                usersAdapter.updateUsers(users)
            },
            onError = { error -> Log.e("Error", error.message) }
        )
    }

    private fun setUserOnlineStatus() {
        repository.setUserPresenceStatus("Online")
    }

    override fun onResume() {
        super.onResume()
        setUserOnlineStatus()
    }

    override fun onPause() {
        super.onPause()
        repository.setUserPresenceStatus("Offline")
    }
}