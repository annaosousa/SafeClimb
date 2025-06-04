package com.example.myapplication.ui.fragment

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Chronometer
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.myapplication.FirstActivity
import com.example.myapplication.databinding.FragmentSaveClimbBinding
import com.example.myapplication.ui.adapter.SearchItemAdapter
import com.example.myapplication.R


class SaveMountainFragment : Fragment() {

    private var _binding: FragmentSaveClimbBinding? = null
    private val binding get() = _binding!!

    private lateinit var chronometer: Chronometer
    private lateinit var editMountain: android.widget.EditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSaveClimbBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val navController = findNavController()

        chronometer = binding.timer
        val stoppedTime = arguments?.getLong("timeBase")
        if (stoppedTime != null) {
            chronometer.base = SystemClock.elapsedRealtime() - stoppedTime
        }

        val mountainsSpinner = binding.mountainsSpinner
        editMountain = binding.editMountain

        val arrayList: ArrayList<String> = SearchItemAdapter.mountainMap.entries.map { it.key } as ArrayList<String>
        arrayList.add("Other")

        val spinnerItem = com.example.myapplication.R.layout.fragment_spinner_item

        val adapter = ArrayAdapter(requireContext(), spinnerItem, arrayList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        mountainsSpinner.adapter = adapter

        mountainsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position) as String

                if (selectedItem == "Other") {
                    editMountain.visibility = View.VISIBLE
                } else {
                    editMountain.visibility = View.INVISIBLE
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }

        val saveButton = binding.saveButton
        saveButton.setOnClickListener {
            val mountainName = if (mountainsSpinner.selectedItem.toString() == "Other") {
                editMountain.text.toString()
            } else {
                mountainsSpinner.selectedItem.toString()
            }
            val time = System.currentTimeMillis()
            val fileSaved = saveClimb(mountainName,time,stoppedTime?:0)
            if(fileSaved) {
                Toast.makeText(context, "Climb saved!", Toast.LENGTH_SHORT).show()
                val options = navOptions {
                    popUpTo(R.id.navigation_home) {
                        inclusive = false
                    }
                }
                navController.navigate(R.id.navigation_mountain_history, null, options)
            }
            else{
                Toast.makeText(context, "Error saving climb!", Toast.LENGTH_SHORT).show()
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        chronometer.stop()
    }

    fun saveClimb(mountainName: String, time: Long, duration: Long ) : Boolean{
        val filename = FirstActivity.getEmail() + "_climbs"
        val nameBytes = mountainName.toByteArray(Charsets.UTF_8)
        val nameSize = nameBytes.size
        val timeArray = byteArrayOf(0,0,0,0,0,0,0,0)
        val durationArray = byteArrayOf(0,0,0,0,0,0,0,0)
        for(i in 0..7)
        {
            timeArray[i] = (time shr (8 * (7 - i))).toUByte().toByte()
            durationArray[i] = (duration shr (8 * (7 - i))).toUByte().toByte()
        }
        try {
            requireContext().openFileOutput(filename, Context.MODE_APPEND).use {
                it.write(nameSize)
                it.write(nameBytes)
                it.write(timeArray)
                it.write(durationArray)
            }
            return true
        }catch (e: Exception){
            return false
        }
    }

}