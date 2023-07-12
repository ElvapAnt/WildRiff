package com.rmas.wildriff.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rmas.wildriff.data.User
import com.rmas.wildriff.databinding.ItemLeaderboardBinding

class LeaderboardAdapter : RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder>() {
    private var usersList: List<User> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardViewHolder {
        val binding = ItemLeaderboardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LeaderboardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LeaderboardViewHolder, position: Int) {
        val user = usersList[position]
        holder.bind(user)
    }

    override fun getItemCount(): Int {
        return usersList.size
    }

    fun submitList(data: List<User>) {
        val previousSize = usersList.size
        usersList = data

        if (previousSize < usersList.size) {
            notifyItemRangeInserted(previousSize, usersList.size - previousSize)
        } else if (previousSize > usersList.size) {
            notifyItemRangeRemoved(usersList.size, previousSize - usersList.size)
        }
    }

    inner class LeaderboardViewHolder(private val binding: ItemLeaderboardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.textUsername.text = user.username
            if (user.score == 0.0f) {
                binding.textScore.text = "Score: 0"
            } else {
                binding.textScore.text = "Score: ${user.score}"
            }
        }
    }
}