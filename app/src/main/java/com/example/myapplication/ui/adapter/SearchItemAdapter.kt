package com.example.myapplication.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.navigation.findNavController
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
            "Morro do Araçatuba" -> R.drawable.morro_do_aracutuba
            "Pico Marumbi" -> R.drawable.pico_marumbi
            "Morro do Canal" -> R.drawable.morro_do_canal
            else -> R.drawable.default_image
        }
        holder.itemImageView.setImageResource(imageResourceId)

        // Adiciona o listener de clique para a imagem
        holder.itemImageView.setOnClickListener {
            val bundle = Bundle().apply {
                putString("mountain_name", item.name)
                putString("mountain_description", item.description)
                putInt("image_resource_id", imageResourceId)
            }

            // Usa o NavController para navegar para o fragmento de destino
            val navController = holder.itemView.findNavController()
            navController.navigate(R.id.navigation_result, bundle)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }
}
