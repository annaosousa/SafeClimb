package com.example.myapplication.ui.fragment

import android.annotation.SuppressLint
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

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)

        val mountainName = arguments?.getString("mountain_name")
        val mountainDescription = arguments?.getString("mountain_description")
        val imageResourceId = arguments?.getInt("image_resource_id")

        binding.titleResult.text = "Current weather in \n$mountainName"

        binding.resultTextView.text = mountainDescription
        binding.resultTextView.text = mountainName
        imageResourceId?.let { binding.imagePlaceholder.setImageResource(it) }

        binding.viewMap.setOnClickListener {
            // Passando o nome da montanha como argumento para o MapMountainFragment
            val bundle = Bundle().apply {
                putString("selected_mountain", mountainName)
            }
            findNavController().navigate(R.id.navigation_map_mountain, bundle)
        }

        binding.viewHistory.setOnClickListener {
            // Passando o nome da montanha como argumento para o HistoryFragment
            val bundle = Bundle().apply {
                putString("mountain_name", mountainName)
            }
            findNavController().navigate(R.id.navigation_history, bundle)
        }

        val query = arguments?.getString("search_query") // Obtendo a string passada
        binding.resultTextView.text = query

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

