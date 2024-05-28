package com.example.speakease

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.speakease.Constants.CHATS
import com.example.speakease.Constants.IMAGE_PICK_REQUEST_CODE_CHAT
import com.example.speakease.Constants.MESSAGE
import com.example.speakease.Constants.MESSAGE_PHOTO
import com.example.speakease.Constants.USER_PRESENCE
import com.example.speakease.adapter.MessagesAdapter
import com.example.speakease.databinding.ActivityChatBinding
import com.example.speakease.model.Message
import com.example.speakease.repositories.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.util.Calendar
import java.util.Date

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var adapter: MessagesAdapter
    private lateinit var messages: ArrayList<Message>
    private lateinit var senderRoom: String
    private lateinit var receiverRoom: String
    private lateinit var dialog: ProgressDialog
    private lateinit var senderUid: String
    private lateinit var receiverUid: String
    private val repository = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initialize()
        setupViewElements()
        fetchAndSetChatMessages()
    }

    private fun initialize() {
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        dialog = ProgressDialog(this).apply {
            setMessage("Uploading Image...")
            setCancelable(false)
        }
        messages = ArrayList()

        receiverUid = intent.getStringExtra("uid")!!
        senderUid = FirebaseAuth.getInstance().uid!!

        senderRoom = senderUid + receiverUid
        receiverRoom = receiverUid + senderUid

        adapter = MessagesAdapter(this, messages, senderRoom, receiverRoom)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun setupViewElements() {
        val nameText = intent.getStringExtra("name")
        val profile = intent.getStringExtra("image")

        binding.apply {
            name.text = nameText
            Glide.with(this@ChatActivity).load(profile)
                .placeholder(R.drawable.placeholder)
                .into(profile01)
            imageView2.setOnClickListener { finish() }
            sendBtn.setOnClickListener { sendMessage() }
            attachment.setOnClickListener { pickImage() }
            messageBox.addTextChangedListener(createTextWatcher())
        }

        setPresenceStatus()
    }

    private fun createTextWatcher(): TextWatcher {
        val handler = Handler()

        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                repository.setUserPresenceStatus("Typing...")
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed({ repository.setUserPresenceStatus("Online") }, 1000)
            }
        }
    }

    private fun setPresenceStatus() {
        repository.setUserPresenceStatus("Online")
        repository.getCurrentUser(
            onSuccess = { user -> binding.status.visibility = View.VISIBLE; binding.status.text = user.status },
            onError = { error -> binding.status.visibility = View.GONE }
        )
    }

    private fun fetchAndSetChatMessages() {
        repository.getChatMessages(
            senderRoom = senderRoom,
            onSuccess = { messages ->
                this.messages.clear()
                this.messages.addAll(messages)
                adapter.notifyDataSetChanged()
            },
            onError = { error -> Log.e("Error", error.message ?: "An error occurred") }
        )
    }

    private fun sendMessage() {
        val messageTxt = binding.messageBox.text.toString()
        val date = Date()
        val message = Message(messageTxt, senderUid, date.time)

        binding.messageBox.setText("")

        repository.sendMessage(senderRoom, receiverRoom, message)
    }

    private fun pickImage() {
        val intent = Intent().apply {
            action = Intent.ACTION_GET_CONTENT
            type = "image/*"
        }
        startActivityForResult(intent, 25)
    }

    override fun onResume() {
        super.onResume()
        setPresenceStatus()
    }

    override fun onPause() {
        super.onPause()
        repository.setUserPresenceStatus("Offline")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IMAGE_PICK_REQUEST_CODE_CHAT && resultCode == RESULT_OK) {
            val selectedImage = data?.data ?: return
            val reference = repository.getChatImageReference()
            dialog.show()

            repository.uploadImage(reference, selectedImage,
                onSuccess = { filePath ->
                    dialog.dismiss()
                    val date = Date()
                    val message = Message(MESSAGE_PHOTO, senderUid, date.time).apply {
                        imageUrl = filePath
                    }

                    binding.messageBox.setText("")

                    repository.sendMessage(senderRoom, receiverRoom, message)
                },
                onFailure = { exception ->
                    dialog.dismiss()
                })
        }
    }
}