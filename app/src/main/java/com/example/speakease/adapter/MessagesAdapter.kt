package com.example.speakease.adapter

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.speakease.Constants.CHATS
import com.example.speakease.Constants.MESSAGE
import com.example.speakease.Constants.MESSAGE_PHOTO
import com.example.speakease.R
import com.example.speakease.databinding.DeleteLayoutBinding
import com.example.speakease.databinding.ReceiveMsgBinding
import com.example.speakease.databinding.SendMsgBinding
import com.example.speakease.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MessagesAdapter(
    private val context: Context,
    private var messages: MutableList<Message>,
    private val senderRoom: String,
    private val receiverRoom: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val ITEM_SENT = 1
        const val ITEM_RECEIVE = 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(context)
        return if (viewType == ITEM_SENT) {
            val view = inflater.inflate(R.layout.send_msg, parent, false)
            SentMsgHolder(view)
        } else {
            val view = inflater.inflate(R.layout.receive_msg, parent, false)
            ReceiveMsgHolder(view)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (FirebaseAuth.getInstance().uid == messages[position].senderId) {
            ITEM_SENT
        } else {
            ITEM_RECEIVE
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is SentMsgHolder) {
            setupSentMessage(holder.binding, message)
        } else if (holder is ReceiveMsgHolder) {
            setupReceivedMessage(holder.binding, message)
        }
    }

    private fun setupSentMessage(binding: SendMsgBinding, message: Message) {
        if (message.message == MESSAGE_PHOTO) {
            binding.image.visibility = View.VISIBLE
            binding.message.visibility = View.GONE
            binding.mLinear.visibility = View.GONE
            Glide.with(context)
                .load(message.imageUrl)
                .placeholder(R.drawable.placeholder)
                .into(binding.image)
        } else {
            binding.image.visibility = View.GONE
            binding.message.visibility = View.VISIBLE
            binding.message.text = message.message
        }

        binding.root.setOnLongClickListener {
            showDeleteDialog(message)
            false
        }
    }

    private fun setupReceivedMessage(binding: ReceiveMsgBinding, message: Message) {
        if (message.message == MESSAGE_PHOTO) {
            binding.image.visibility = View.VISIBLE
            binding.message.visibility = View.GONE
            binding.mLinear.visibility = View.GONE
            Glide.with(context)
                .load(message.imageUrl)
                .placeholder(R.drawable.placeholder)
                .into(binding.image)
        } else {
            binding.image.visibility = View.GONE
            binding.message.visibility = View.VISIBLE
            binding.message.text = message.message
        }

        binding.root.setOnLongClickListener {
            showDeleteDialog(message)
            false
        }
    }

    private fun showDeleteDialog(message: Message) {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.delete_layout, null)
        val binding = DeleteLayoutBinding.bind(view)
        val dialog = AlertDialog.Builder(context)
            .setTitle("Delete Message")
            .setView(binding.root)
            .create()

        binding.everyone.setOnClickListener {
            message.message = "This message is removed"
            message.imageUrl = null
            message.messageId?.let { messageId ->
                FirebaseDatabase.getInstance().reference.child(CHATS)
                    .child(senderRoom)
                    .child(MESSAGE)
                    .child(messageId).setValue(message)
                FirebaseDatabase.getInstance().reference.child(CHATS)
                    .child(receiverRoom)
                    .child(MESSAGE)
                    .child(messageId).setValue(message)
            }
            dialog.dismiss()
        }

        binding.delete.setOnClickListener {
            message.messageId?.let { messageId ->
                FirebaseDatabase.getInstance().reference.child(CHATS)
                    .child(senderRoom)
                    .child(MESSAGE)
                    .child(messageId).setValue(null)
            }
            dialog.dismiss()
        }

        binding.cancel.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    override fun getItemCount(): Int = messages.size

    inner class SentMsgHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: SendMsgBinding = SendMsgBinding.bind(itemView)
    }

    inner class ReceiveMsgHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: ReceiveMsgBinding = ReceiveMsgBinding.bind(itemView)
    }
}