package com.utfpr.safeclimb.ui.fragment

import android.annotation.SuppressLint
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
import com.utfpr.safeclimb.databinding.FragmentSelectHistoryBinding
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.QueryRequest
import com.utfpr.safeclimb.R
import com.utfpr.safeclimb.ui.adapter.HistoryAdapter
import com.utfpr.safeclimb.ui.item.HistoryItem
import java.util.*

// Tempo de 24 horas em milissegundos
const val TIMESTAMP_OFFSET = 86400000

@SuppressLint("SetTextI18n")
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
                // Obtem dia, mes e ano da data selecionada
                val (day, month, year) = selectedDate!!.split("/").map { it.toInt() }

                // Obtem horas e minutos do horario selecionado
                val (hours, minutes) = selectedTime!!.split(":").map { it.toInt() }

                val calendar = Calendar.getInstance()

                // Utiliza calendar para definir a variavel de timestamp
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month - 1)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                calendar.set(Calendar.HOUR_OF_DAY, hours)
                calendar.set(Calendar.MINUTE, minutes)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                val timestamp = calendar.timeInMillis

                // Inicia a consulta no DynamoDB
                val thread = Thread {
                    val fetchedHistory = queryDynamoDB(timestamp)
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


    @SuppressLint("DefaultLocale")
    private fun queryDynamoDB(timestamp: Long): List<HistoryItem> {
        val historyList = mutableListOf<HistoryItem>()

        try {
            val mountainNameAttributeValue = AttributeValue().withS(mountainName)
            val timeStampAttributeValue = AttributeValue().withS("$timestamp")

            val request = QueryRequest()
                .withTableName("safe_climb")
                .withKeyConditionExpression("mountain = :mountainName AND #ts <= :final_datetime")
                .withExpressionAttributeNames(mapOf("#ts" to "timestamp"))
                .addExpressionAttributeValuesEntry(":mountainName", mountainNameAttributeValue)
                .addExpressionAttributeValuesEntry(":final_datetime", timeStampAttributeValue)
                .withScanIndexForward(false)
                .withLimit(100)

            val result = client.query(request)


            for (item in result.items) {
                val rawTimestamp = item["timestamp"]?.s
                if (rawTimestamp.isNullOrEmpty()) continue

                try {
                    val longTimestamp = rawTimestamp.trim().toLong()
                    val date = Date(longTimestamp)

                    if (date != null) {
                        val calendar = Calendar.getInstance()
                        calendar.time = date

                        val year = calendar.get(Calendar.YEAR)
                        val month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: "Unknown"
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