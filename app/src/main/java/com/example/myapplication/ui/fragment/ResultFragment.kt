package com.example.myapplication.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentResultBinding

class ResultFragment : Fragment() {

    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)

        val mountainName = arguments?.getString("mountain_name")
        val mountainDescription = arguments?.getString("mountain_description")
        val imageResourceId = arguments?.getInt("image_resource_id")

        binding.resultTextView.text = mountainDescription
        imageResourceId?.let { binding.imagePlaceholder.setImageResource(it) }

        val query = arguments?.getString("search_query") // Obtendo a string passada
        binding.resultTextView.text = query

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewMap.setOnClickListener {
            findNavController().navigate(R.id.navigation_map)
        }

        binding.viewHistory.setOnClickListener {
            findNavController().navigate(R.id.navigation_history)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

