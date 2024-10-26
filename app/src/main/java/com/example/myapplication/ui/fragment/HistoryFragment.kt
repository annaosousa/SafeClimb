package com.example.myapplication.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentHistoryBinding
import com.example.myapplication.ui.adapter.HistoryAdapter
import com.example.myapplication.ui.item.HistoryItem

class HistoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var historyAdapter: HistoryAdapter
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        val view = binding.root

        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.recyclerViewHistory)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Simulated data for the RecyclerView
        val historyList = listOf(
            HistoryItem("October", 11, 2024, "3:30", "PM", "15 km/h", "70%", "22°C"),
            HistoryItem("October", 12, 2024, "4:00", "PM", "10 km/h", "60%", "20°C"),
            HistoryItem("October", 13, 2024, "4:30", "PM", "15 km/h", "80%", "21°C"),
            HistoryItem("October", 14, 2024, "5:00", "PM", "10 km/h", "90%", "25°C"),
            HistoryItem("October", 15, 2024, "5:30", "PM", "9 km/h", "20%", "30°C"),
            HistoryItem("October", 16, 2024, "6:00", "PM", "13 km/h", "40%", "15°C"),
            HistoryItem("October", 17, 2024, "6:30", "PM", "11 km/h", "50%", "20°C")
        )

        historyAdapter = HistoryAdapter(historyList)
        recyclerView.adapter = historyAdapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewAnotherDateButton = view.findViewById<Button>(R.id.viewAnotherDate)
        viewAnotherDateButton.setOnClickListener {
            findNavController().navigate(R.id.navigation_select_history)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
