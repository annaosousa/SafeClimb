package com.example.myapplication.ui.fragment

import android.app.TimePickerDialog
import android.app.DatePickerDialog
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.TimePicker
import android.widget.Toast
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.regions.Regions
import com.example.myapplication.databinding.FragmentSelectHistoryBinding
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.ScanRequest
import com.amazonaws.services.dynamodbv2.model.ScanResult
import com.example.myapplication.R
import com.example.myapplication.ui.adapter.HistoryAdapter
import com.example.myapplication.ui.item.HistoryItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import java.text.SimpleDateFormat


class SelectHistoryFragment : Fragment() {

    private final val TAG: String = "SelectHistoryFragment"

    private lateinit var recyclerView: RecyclerView
    private lateinit var historyAdapter: HistoryAdapter

    private var _binding: FragmentSelectHistoryBinding? = null
    private val binding get() = _binding!!
    private var mountainName: String? = null

    // Variáveis para armazenar data e hora selecionadas
    private var selectedDate: String? = null
    private var selectedTime: String? = null

    private var history: List<HistoryItem> = mutableListOf()

    private lateinit var client: AmazonDynamoDBClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectHistoryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        mountainName = arguments?.getString("mountain_name")
        binding.titleSelectHistory.text = "Select history for $mountainName"

        // Initialize RecyclerView
        // Initialize RecyclerView
        recyclerView = root.findViewById(R.id.recyclerViewHistory)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Inicializar o provedor de credenciais do Amazon Cognito
        val credentialsProvider = CognitoCachingCredentialsProvider(
            requireContext(),
            "eu-north-1:7c834096-3ddd-4282-a809-4390e3e5686a", // ID do grupo de identidades
            Regions.EU_NORTH_1 // Região
        )

        val thread = Thread {
            requireActivity().runOnUiThread {
                historyAdapter = HistoryAdapter(history)
                recyclerView.adapter = historyAdapter
            }
        }

        thread.start()

        // Inicializar o cliente para acesso ao DynamoDB
        client = AmazonDynamoDBClient(credentialsProvider, ClientConfiguration())

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
                    Log.d(TAG, "$selectedHour : $selectedMinute")
                    selectedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                    binding.editTextTimer.setText(selectedTime)
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
                    selectedDate = String.format("%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear)
                    binding.editDate.setText(selectedDate)
                },
                year, month, day
            )
            datePickerDialog.show()
        }

        binding.buttonFindHistory.setOnClickListener {
            // Verifica se a data e a hora foram selecionadas
            if (selectedDate != null && selectedTime != null) {
                val inputDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val outputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val formattedDate = outputDateFormat.format(inputDateFormat.parse(selectedDate!!)!!)

                val formattedTime = "$selectedTime:00"
                // Inicia a consulta no DynamoDB
                val thread = Thread {
                    val fetchedHistory = queryDynamoDB(formattedDate, formattedTime)
                    requireActivity().runOnUiThread {
                        if (fetchedHistory.isNotEmpty()) {
                            historyAdapter.updateData(fetchedHistory)
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "No data found for $mountainName",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                thread.start()

            } else {
                // Exibir mensagem de erro em um modal
                showErrorDialog("Por favor, selecione uma data e uma hora.")
            }
        }
    }


    private fun queryDynamoDB(date: String, time: String): List<HistoryItem> {
        val historyList = mutableListOf<HistoryItem>()

        try {
            val timestamp = "$date#$time"
            Log.d(TAG, timestamp)
            val mountainNameAttributeValue = AttributeValue().withS(mountainName)
            val timeStampAttributeValue = AttributeValue().withS(timestamp)
            // Configurar os parâmetros da consulta, agora filtrando pelo nome da montanha
            var request = ScanRequest()
                .withTableName("safe_climb")
                .withFilterExpression("mountain = :mountainName and #ts = :datetime")
                .withExpressionAttributeNames(mapOf("#ts" to "timestamp"))
                .addExpressionAttributeValuesEntry(":mountainName", mountainNameAttributeValue)
                .addExpressionAttributeValuesEntry(":datetime", timeStampAttributeValue)

            // Adicionar log para verificar o estado da solicitação
            Log.d("DynamoDB", "Executing scan with request: $request")

            // Executar o scan no DynamoDB
            var result = client.scan(request)

            if (result.items.isEmpty()) {
                val initialTimestamp = "$date#00:00:00"
                val finalTimestamp = "$date#23:59:59"
                val initialTimestampAttributeValue = AttributeValue().withS(initialTimestamp)
                val finalTimestampAttributeValue = AttributeValue().withS(finalTimestamp)

                request = ScanRequest()
                    .withTableName("safe_climb")
                    .withFilterExpression("mountain = :mountainName and #ts >= :initial_datetime and #ts <= :final_datetime")
                    .withExpressionAttributeNames(mapOf("#ts" to "timestamp"))
                    .addExpressionAttributeValuesEntry(":mountainName", mountainNameAttributeValue)
                    .addExpressionAttributeValuesEntry(":initial_datetime", initialTimestampAttributeValue)
                    .addExpressionAttributeValuesEntry(":final_datetime", finalTimestampAttributeValue)

                result = client.scan(request)
            }

            Log.d("DynamoDB", "Scan result: $result")

            val dateFormat = SimpleDateFormat("yyyy-MM-dd#HH:mm:ss", Locale.getDefault())

            for (item in result.items) {
                val timestamp = item["timestamp"]?.s

                if (timestamp != null) {
                    try {
                        val date = dateFormat.parse(timestamp)
                        if (date != null) {
                            // Criar uma instância de Calendar a partir da data
                            val calendar = Calendar.getInstance()
                            calendar.time = date

                            val year = calendar.get(Calendar.YEAR)
                            val month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: "Unknown"
                            val day = calendar.get(Calendar.DAY_OF_MONTH)

                            val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
                            val minute = calendar.get(Calendar.MINUTE)
                            val periodOfDay = if (hourOfDay < 12) "AM" else "PM"
                            val hour = String.format("%02d:%02d", if (hourOfDay % 12 == 0) 12 else hourOfDay % 12, minute)

                            // Extrair dados de outros campos da tabela DynamoDB
                            val windSpeed = item["wind_speed"]?.n?.let { "$it km/h" } ?: "Unknown"
                            val humidity = item["humidity"]?.n?.let { "$it%" } ?: "Unknown"
                            val temperature = item["temperature"]?.n?.let { "$it°C" } ?: "Unknown"
                            val precipitation = item["precipitation"]?.n?.toDoubleOrNull()?.let { "$it mm" } ?: "Unknown"
                            val soil = item["soil_moisture"]?.n?.toDoubleOrNull()?.let { "$it%" } ?: "Unknown"

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
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("DynamoDB", "Error executing scan", e)
        }

        return historyList
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Erro")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}