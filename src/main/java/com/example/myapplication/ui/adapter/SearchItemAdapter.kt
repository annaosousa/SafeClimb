package com.example.myapplication.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R

data class Item(val name: String, val description: String)

class SearchItemAdapter(private val items: List<Item>) : RecyclerView.Adapter<SearchItemAdapter.ItemViewHolder>() {

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemImageView: ImageView = view.findViewById(R.id.item_image)
        val itemNameTextView: TextView = view.findViewById(R.id.item_name)
        val itemDescriptionTextView: TextView = view.findViewById(R.id.item_description)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_view, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]

        holder.itemNameTextView.text = item.name
        holder.itemDescriptionTextView.text = item.description

        val imageResourceId = when (item.name) {
            "Pico do Paraná" -> R.drawable.pico_do_parana
            "Pico do Caratuva" -> R.drawable.pico_do_caratuva
            "Morro do Anhangava" -> R.drawable.morro_do_anhangava
            else -> R.drawable.default_image
        }
        holder.itemImageView.setImageResource(imageResourceId)
    }

    override fun getItemCount(): Int {
        return items.size
    }
}

