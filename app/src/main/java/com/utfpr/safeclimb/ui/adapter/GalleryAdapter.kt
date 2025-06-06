package com.utfpr.safeclimb.ui.adapter

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.utfpr.safeclimb.R

class GalleryAdapter(private val dataSet: List<Uri>) :
    RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

        private lateinit var navController: NavController

        fun setNavController(navController: NavController) {
            this.navController = navController
        }

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var imageView: ImageView

        init {
            // Define click listener for the ViewHolder's View
            imageView = view.findViewById(R.id.image)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.item_picture, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.imageView.setImageURI(dataSet[position])

        viewHolder.imageView.setOnClickListener {
            val context = viewHolder.itemView.context
            val uri = dataSet[position]
            viewImage(context,uri)
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

    fun viewImage(context: Context, imageUri: Uri) {
//        val intent = Intent(Intent.ACTION_VIEW).apply {
//            setDataAndType(imageUri, "image/*")
//            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//        }
//        try {
//            context.startActivity(intent)
//        } catch (e: android.content.ActivityNotFoundException) {
//            return
//        }
        val args = Bundle().apply { putString("imageUri", imageUri.toString()) }
        navController.navigate(R.id.navigation_image_details, args)

    }

}