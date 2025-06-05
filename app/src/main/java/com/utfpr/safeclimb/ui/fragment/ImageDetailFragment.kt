package com.utfpr.safeclimb.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.utfpr.safeclimb.databinding.FragmentImageDetailBinding


class ImageDetailFragment : Fragment() {

    private var _binding: FragmentImageDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentImageDetailBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val navController = findNavController()

        val imageView = binding.imageViewDetail
//        val closeButton = binding.buttonCloseDetail

        val imageUri = arguments?.getString("imageUri")?.toUri()

        if(imageUri != null)
        {
            imageView.setImageURI(imageUri)
        }

//        closeButton.setOnClickListener {
//            navController.popBackStack()
//        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}