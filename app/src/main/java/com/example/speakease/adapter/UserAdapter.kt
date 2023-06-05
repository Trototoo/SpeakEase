package com.example.speakease.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.speakease.ChatActivity
import com.example.speakease.R
import com.example.speakease.databinding.ItemProfileBinding
import com.example.speakease.model.User

class UserAdapter(private val userList: ArrayList<User>):
    RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(val binding: ItemProfileBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemProfileBinding.inflate(inflater, parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.binding.username.text = user.name
        Glide.with(holder.itemView.context).load(user.profileImage)
            .placeholder(R.drawable.avatar)
            .into(holder.binding.profile)
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, ChatActivity::class.java).apply {
                putExtra("name", user.name)
                putExtra("image", user.profileImage)
                putExtra("uid", user.uid)
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = userList.size
}