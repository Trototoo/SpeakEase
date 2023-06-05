package com.example.speakease

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.speakease.Constants.CHATS
import com.example.speakease.Constants.MESSAGE
import com.example.speakease.Constants.MESSAGE_PHOTO
import com.example.speakease.Constants.USER_PRESENCE
import com.example.speakease.adapter.MessagesAdapter
import com.example.speakease.databinding.ActivityChatBinding
import com.example.speakease.model.Message
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
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private lateinit var dialog: ProgressDialog
    private lateinit var senderUid: String
    private lateinit var receiverUid: String
    private lateinit var currentUserId: String

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

        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        dialog = ProgressDialog(this).apply {
            setMessage("Uploading Image...")
            setCancelable(false)
        }
        messages = ArrayList()

        receiverUid = intent.getStringExtra("uid")!!
        currentUserId = FirebaseAuth.getInstance().uid!!

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
                database.reference.child(USER_PRESENCE).child(senderUid).setValue("Typing...")
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed({ database.reference.child(USER_PRESENCE).child(senderUid).setValue("Online") }, 1000)
            }
        }
    }

    private fun setPresenceStatus() {
        database.reference.child(USER_PRESENCE).child(receiverUid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val status = snapshot.getValue(String::class.java)
                        binding.status.text = status
                        binding.status.visibility = if (status == "Offline") View.GONE else View.VISIBLE
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun fetchAndSetChatMessages() {
        database.reference.child(CHATS).child(senderRoom).child(MESSAGE)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messages.clear()
                    for (snapshot1 in snapshot.children) {
                        val message = snapshot1.getValue(Message::class.java)!!
                        message.messageId = snapshot1.key
                        messages.add(message)
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun sendMessage() {
        var messageTxt = binding.messageBox.text.toString()

        if (messageTxt.isEmpty()) {
            return
        }

        messageTxt = messageTxt.trimEnd('\n')

        val date = Date()
        val message = Message(messageTxt, senderUid, date.time)

        binding.messageBox.setText("")

        val randomKey = database.reference.push().key!!
        val lastMsgObj = mapOf("lastMsg" to message.message!!, "lastMsgTime" to date.time)

        database.reference.child(CHATS).child(senderRoom).updateChildren(lastMsgObj)
        database.reference.child(CHATS).child(receiverRoom).updateChildren(lastMsgObj)

        database.reference.child(CHATS).child(senderRoom).child(MESSAGE).child(randomKey)
            .setValue(message)
            .addOnSuccessListener {
                database.reference.child(CHATS).child(receiverRoom).child(MESSAGE).child(randomKey)
                    .setValue(message)
            }
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
        database.reference.child(USER_PRESENCE).child(currentUserId).setValue("Online")
    }

    override fun onPause() {
        super.onPause()
        database.reference.child(USER_PRESENCE).child(currentUserId).setValue("Offline")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 25 && resultCode == RESULT_OK) {
            val selectedImage = data?.data ?: return

            val reference = storage.reference.child(CHATS)
                .child(Calendar.getInstance().timeInMillis.toString())

            dialog.show()

            reference.putFile(selectedImage).addOnCompleteListener { task ->
                dialog.dismiss()

                if (task.isSuccessful) {
                    reference.downloadUrl.addOnSuccessListener { uri ->
                        val filePath = uri.toString()

                        val date = Date()
                        val message = Message(MESSAGE_PHOTO, senderUid, date.time).apply {
                            imageUrl = filePath
                        }

                        binding.messageBox.setText("")

                        val randomKey = database.reference.push().key ?: return@addOnSuccessListener

                        val lastMsgObj = mapOf("lastMsg" to message.message, "lastMsgTime" to date.time)

                        database.reference.child(CHATS).updateChildren(lastMsgObj)
                        database.reference.child(CHATS).child(receiverRoom).updateChildren(lastMsgObj)

                        database.reference.child(CHATS).child(senderRoom).child(MESSAGE).child(randomKey)
                            .setValue(message).addOnSuccessListener {
                                database.reference.child(CHATS).child(receiverRoom).child(MESSAGE).child(randomKey)
                                    .setValue(message)
                            }
                    }
                }
            }
        }
    }
}