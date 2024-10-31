package com.example.myapplication.ui.fragment

import android.app.TimePickerDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.FragmentSelectHistoryBinding
import java.util.*

class SelectHistoryFragment : Fragment() {

    private var _binding: FragmentSelectHistoryBinding? = null
    private val binding get() = _binding!!
    private var mountainName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectHistoryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        mountainName = arguments?.getString("mountain_name")
        binding.titleSelectHistory.text = "Select history for $mountainName"

        setupListeners()

        return root
    }

    private fun setupListeners() {
        binding.editTextTimer.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(
                requireContext(),
                { _: TimePicker, selectedHour: Int, selectedMinute: Int ->
                    binding.editTextTimer.setText(String.format("%02d:%02d", selectedHour, selectedMinute))
                },
                hour, minute, true
            )
            timePickerDialog.show()
        }

        binding.editDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
                    binding.editDate.setText(String.format("%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear))
                },
                year, month, day
            )
            datePickerDialog.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}