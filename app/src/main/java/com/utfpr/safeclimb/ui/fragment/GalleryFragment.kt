package com.utfpr.safeclimb.ui.fragment

import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.utfpr.safeclimb.FirstActivity
import com.utfpr.safeclimb.databinding.FragmentGalleryBinding
import com.utfpr.safeclimb.ui.adapter.GalleryAdapter

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val navController = findNavController()

        var dataset = mutableListOf<Uri>()
        populateWithAppImageUris(dataset)

        val adapter = GalleryAdapter(dataset)
        adapter.setNavController(navController)

        val recyclerView: RecyclerView = binding.galleryView
        recyclerView.layoutManager = GridLayoutManager(context,3)
        recyclerView.adapter = adapter

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun populateWithAppImageUris(targetList: MutableList<Uri>) {

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME
        )

        val selection = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf("SafeClimb_" + FirstActivity.getEmail())

        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        val queryUri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        try {
            requireContext().contentResolver.query(
                queryUri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)

                    val contentUri: Uri = Uri.withAppendedPath(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    )
                    targetList.add(contentUri)
                }
            }
        }
        catch (e: Exception) {
        }
    }
}