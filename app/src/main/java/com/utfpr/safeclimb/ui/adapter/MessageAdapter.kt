package com.utfpr.safeclimb.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.utfpr.safeclimb.R
import com.utfpr.safeclimb.ui.data.Message

class MessageAdapter(private val messages: List<Message>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val USER_MESSAGE = 1
    private val BOT_MESSAGE = 2

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUserMessage) USER_MESSAGE else BOT_MESSAGE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == USER_MESSAGE) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.message_item_user, parent, false)
            UserMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.message_item_bot, parent, false)
            BotMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is UserMessageViewHolder) {
            holder.bind(message)
        } else if (holder is BotMessageViewHolder) {
            holder.bind(message)
        }
    }

    override fun getItemCount() = messages.size

    inner class UserMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMessageUser: TextView = view.findViewById(R.id.tv_message_user)
        private val imgPerson: ImageView = view.findViewById(R.id.imgPerson)

        fun bind(message: Message) {
            tvMessageUser.text = message.text
            imgPerson.visibility = View.VISIBLE
        }
    }

    inner class BotMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMessageBot: TextView = view.findViewById(R.id.tv_message_bot)
        private val imgBot: ImageView = view.findViewById(R.id.imgBot)

        fun bind(message: Message) {
            tvMessageBot.text = message.text
            imgBot.visibility = View.VISIBLE
        }
    }
}
