package com.example.myapplication.ui.fragment

import android.content.Context
import android.content.Intent
import com.example.myapplication.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.FirstActivity
import com.example.myapplication.MainActivity
import com.example.myapplication.databinding.FragmentAuthBinding
import com.example.myapplication.databinding.FragmentHomeBinding


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val navController = findNavController()

        val buttonNavigate = binding.button
        buttonNavigate.setOnClickListener {
            val loggedIn = getCred()
            if(loggedIn){
                val intent = Intent(
                    context,
                    FirstActivity::class.java
                )
                startActivity(intent)
            }
            else
                navController.navigate(R.id.navigation_auth, null)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun getCred() : Boolean{
        try {
            val sharedPref = activity?.getSharedPreferences("preferences", Context.MODE_PRIVATE) ?: return false
            val loggedIn = sharedPref.getBoolean("user_logged", false)
            return loggedIn
        }catch (e: Exception){
            return false
        }

    }
}