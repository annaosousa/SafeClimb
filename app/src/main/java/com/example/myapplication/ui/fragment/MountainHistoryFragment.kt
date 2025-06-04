package com.example.myapplication.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.FirstActivity
import com.example.myapplication.databinding.FragmentMountainHistoryBinding
import com.example.myapplication.ui.adapter.MountainHistoryAdapter
import com.example.myapplication.ui.data.ClimbData
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.sql.Timestamp
import java.util.concurrent.TimeUnit


class MountainHistoryFragment : Fragment() {

    private var _binding: FragmentMountainHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentMountainHistoryBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val navController = findNavController()

        var dataset = mutableListOf<ClimbData>()
        retrieveClimb(dataset)

        val adapter = MountainHistoryAdapter(dataset.asReversed())

        val recyclerView: RecyclerView = binding.climbRecycler
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun retrieveClimb(dataset: MutableList<ClimbData>) {
        val filename = FirstActivity.getEmail() + "_climbs"

        try {
            requireContext().openFileInput(filename).use { fis ->
                DataInputStream(BufferedInputStream(fis)).use { dis ->
                    var fileAvailable = true
                    while(fileAvailable)
                    {
                        var nameSize = dis.read()
                        if(nameSize != -1)
                        {
                            val nameBytes = ByteArray(nameSize)
                            dis.readFully(nameBytes)
                            val name = String(nameBytes, Charsets.UTF_8)
                            val time = dis.readLong()
                            val duration = dis.readLong()
                            val timeStamp = Timestamp(time)

                            val data = ClimbData("", "", "")
                            data.mountainName = name
                            data.climbDate = getMonth((timeStamp.month + 1)) + " " + (timeStamp.day + 1).toString() + ", " + (timeStamp.year + 1900).toString()
                            data.climbDuration = "Total time: " + TimeUnit.MILLISECONDS.toHours(duration).toString() + "h " +
                                    (TimeUnit.MILLISECONDS.toMinutes(duration)%60).toString() + "min " +
                                    (TimeUnit.MILLISECONDS.toSeconds(duration)%60).toString() + "sec"
                            dataset.add(data)
                        }
                        else
                            fileAvailable = false
                    }
                }
            }
        }catch (e: Exception){
            return
        }
    }

    fun getMonth(month: Int): String{
        val monthString = when(month) {
            0 -> "January"
            1 -> "February"
            2 -> "March"
            3 -> "April"
            4 -> "May"
            5 -> "June"
            6 -> "July"
            7 -> "August"
            8 -> "September"
            9 -> "October"
            10 -> "November"
            11 -> "December"
            else -> "Invalid month"
        }
        return monthString
    }
}