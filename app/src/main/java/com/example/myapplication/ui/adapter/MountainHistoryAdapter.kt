package com.example.myapplication.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.ui.data.ClimbData

class MountainHistoryAdapter(private val dataSet: MutableList<ClimbData>) :
    RecyclerView.Adapter<MountainHistoryAdapter.ViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var mountainImage: ImageView
        var mountainName: TextView
        var climbDate: TextView
        var climbDuration: TextView

        init {
            // Define click listener for the ViewHolder's View
            mountainImage = view.findViewById(R.id.mountain_image)
            mountainName = view.findViewById(R.id.mountain_name)
            climbDate = view.findViewById(R.id.climb_date)
            climbDuration = view.findViewById(R.id.climb_duration)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.item_mountain_history, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.mountainImage.setImageResource(getMountainImage(dataSet[position].mountainName))
        viewHolder.mountainName.text = dataSet[position].mountainName
        viewHolder.climbDate.text = dataSet[position].climbDate
        viewHolder.climbDuration.text = dataSet[position].climbDuration
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

    fun getMountainImage(name: String): Int {
        val imageResourceId = when (name) {
            "Pico do Paraná" -> R.drawable.pico_do_parana
            "Pico do Caratuva" -> R.drawable.pico_do_caratuva
            "Morro do Anhangava" -> R.drawable.morro_do_anhangava
            "Morro do Araçatuba" -> R.drawable.morro_do_aracutuba
            "Pico Marumbi" -> R.drawable.pico_marumbi
            "Morro do Canal" -> R.drawable.morro_do_canal
            else -> R.drawable.default_image
        }
        return imageResourceId
    }

}