package com.example.speakease

import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.speakease.Constants.USERS_PATH
import com.example.speakease.Constants.USER_PRESENCE
import com.example.speakease.adapter.UserAdapter
import com.example.speakease.databinding.ActivityMainBinding
import com.example.speakease.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var users: ArrayList<User>
    private lateinit var usersAdapter: UserAdapter
    private lateinit var dialog: ProgressDialog
    private lateinit var user: User
    private lateinit var currentUserId: String

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

        database = FirebaseDatabase.getInstance()
        users = ArrayList()

        currentUserId = FirebaseAuth.getInstance().uid!!

        database.reference.child(USERS_PATH)
            .child(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    user = snapshot.getValue(User::class.java)!!
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun setupUsersList() {
        usersAdapter = UserAdapter(users)
        binding.mRec.layoutManager = GridLayoutManager(this, 2)
        binding.mRec.adapter = usersAdapter

        database.reference.child(USERS_PATH).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                users.clear()
                for (userSnapshot in snapshot.children) {
                    val user: User = userSnapshot.getValue(User::class.java)!!
                    if (user.uid != FirebaseAuth.getInstance().uid) users.add(user)
                }
                usersAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setUserOnlineStatus() {
        database.reference.child(USER_PRESENCE).child(currentUserId)
            .setValue("Online")
    }

    override fun onResume() {
        super.onResume()
        setUserOnlineStatus()
    }

    override fun onPause() {
        super.onPause()
        database.reference.child(USER_PRESENCE)
            .child(currentUserId)
            .setValue("Offline")
    }
}