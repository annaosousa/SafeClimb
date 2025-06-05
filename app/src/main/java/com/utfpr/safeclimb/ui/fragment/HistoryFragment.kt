package com.utfpr.safeclimb.ui.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.QueryRequest
import com.utfpr.safeclimb.FirstActivity
import com.utfpr.safeclimb.R
import com.utfpr.safeclimb.databinding.FragmentHistoryBinding
import com.utfpr.safeclimb.ui.adapter.HistoryAdapter
import com.utfpr.safeclimb.ui.item.HistoryItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HistoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var historyAdapter: HistoryAdapter
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var client: AmazonDynamoDBClient

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        val view = binding.root

        if (!FirstActivity.isLoggedIn()) {
            recyclerView = binding.recyclerViewHistory
            historyAdapter = HistoryAdapter(emptyList())
            client = AmazonDynamoDBClient()
            val dialog = CheckAuthenticationDialogFragment()
            dialog.show(
                childFragmentManager,
                CheckAuthenticationDialogFragment.TAG
            )
            dialog.setNavController(findNavController())
        }
        else {

            val locationName = arguments?.getString("mountain_name") ?: "Default Location"
            binding.titleHistory.text = "History in $locationName"

            // Initialize RecyclerView
            recyclerView = view.findViewById(R.id.recyclerViewHistory)
            recyclerView.layoutManager = LinearLayoutManager(requireContext()).apply {
                reverseLayout = true
                stackFromEnd = true
            }

            // Inicializar o provedor de credenciais do Amazon Cognito
            val credentialsProvider = CognitoCachingCredentialsProvider(
                requireContext(),
                "eu-north-1:7c834096-3ddd-4282-a809-4390e3e5686a", // ID do grupo de identidades
                Regions.EU_NORTH_1 // Região
            )

            // Inicializar o cliente para acesso ao DynamoDB
            client = AmazonDynamoDBClient(credentialsProvider, ClientConfiguration())

            // Busca os dados do DynamoDB em uma thread separada
            val thread = Thread {
                val fetchedHistory = fetchHistoryFromDynamoDB(locationName)
                requireActivity().runOnUiThread {
                    if (fetchedHistory.isNotEmpty()) {
                        historyAdapter = HistoryAdapter(fetchedHistory)
                        recyclerView.adapter = historyAdapter
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "No data found for $locationName",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            thread.start()

            val viewAnotherDateButton = view.findViewById<Button>(R.id.viewAnotherDate)
            viewAnotherDateButton.setOnClickListener {
                val mountainName = arguments?.getString("mountain_name")

                // Crie um bundle e passe o nome da montanha
                val bundle = Bundle().apply {
                    putString("mountain_name", mountainName)
                }

                findNavController().navigate(R.id.navigation_select_history, bundle)
            }
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("DefaultLocale")
    private fun fetchHistoryFromDynamoDB(locationName: String): List<HistoryItem> {
        val historyList = mutableListOf<HistoryItem>()

        try {
            val attributeValue = AttributeValue().withS(locationName)
            // Configurar os parâmetros da consulta, agora filtrando pelo nome da montanha
            val request = QueryRequest()
                .withTableName("safe_climb")
                .withKeyConditionExpression("mountain = :mountainName")
                .withExpressionAttributeValues(mapOf(":mountainName" to attributeValue))
                .withScanIndexForward(false)
                .withLimit(100)

            // Adicionar log para verificar o estado da solicitação
            Log.d("DynamoDB", "Executing scan with request: $request")

            // Executar o scan no DynamoDB
            val result = client.query(request)

            Log.d("DynamoDB", "Scan result: $result")

            // Formato esperado para o campo `timestamp`
            val dateFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())

            for (item in result.items) {
                val rawTimestamp = item["timestamp"]?.s
                if (rawTimestamp.isNullOrEmpty()) continue

                try {
                    val longTimestamp = rawTimestamp.trim().toLong()
                    val dateUnix = Date(longTimestamp)

                    // Retornar a data formatada
                    val formattedTimestamp = dateFormat.format(dateUnix)

                    // Parse da data no formato yyyyMMddHHmmss
                    val date = dateFormat.parse(formattedTimestamp)
                    if (date != null) {
                        val calendar = Calendar.getInstance()
                        calendar.time = date

                        val year = calendar.get(Calendar.YEAR)
                        val month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH) ?: "Unknown"
                        val day = calendar.get(Calendar.DAY_OF_MONTH)

                        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
                        val minute = calendar.get(Calendar.MINUTE)
                        val periodOfDay = if (hourOfDay < 12) "AM" else "PM"
                        val hour = String.format(
                            "%02d:%02d",
                            if (hourOfDay % 12 == 0) 12 else hourOfDay % 12,
                            minute
                        )

                        // Extrair dados do campo `payload`
                        val payload = item["payload"]?.m
                        if (payload != null) {
                            val windSpeed = payload["wind_speed"]?.n?.toDoubleOrNull()?.let { "$it km/h" } ?: "Unknown"
                            val humidity = payload["humidity"]?.n?.toDoubleOrNull()?.let { "$it%" } ?: "Unknown"
                            val temperature = payload["temperature"]?.n?.toDoubleOrNull()?.let { "${it/10}°C" } ?: "Unknown"
                            val precipitation = payload["precipitation"]?.n?.toDoubleOrNull()?.let { "$it %" } ?: "Unknown"
                            val soil = payload["soil_moisture"]?.n?.toDoubleOrNull()?.let { "$it%" } ?: "Unknown"

                            // Adicionar o item ao histórico
                            val historyItem = HistoryItem(
                                month = month,
                                day = day,
                                year = year,
                                hour = hour,
                                periodOfDay = periodOfDay,
                                windSpeed = windSpeed,
                                humidity = humidity,
                                temperature = temperature,
                                precipitation = precipitation,
                                soil = soil
                            )
                            historyList.add(historyItem)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("DynamoDB", "Error parsing date: $rawTimestamp", e)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("DynamoDB", "Error executing scan", e)
        }

        return historyList
    }

}
