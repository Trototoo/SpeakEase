package com.example.speakease

import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.speakease.Constants.CHATS
import com.example.speakease.Constants.MESSAGE
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
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
        senderUid = FirebaseAuth.getInstance().uid!!

        senderRoom = senderUid + receiverUid
        receiverRoom = receiverUid + senderUid

        adapter = MessagesAdapter(this, messages, senderRoom, receiverRoom)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        supportActionBar?.setDisplayShowTitleEnabled(false)
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
}