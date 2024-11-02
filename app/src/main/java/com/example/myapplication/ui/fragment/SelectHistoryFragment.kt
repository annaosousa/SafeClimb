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
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.FragmentSelectHistoryBinding
import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.QueryRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class SelectHistoryFragment : Fragment() {

    private var _binding: FragmentSelectHistoryBinding? = null
    private val binding get() = _binding!!
    private var mountainName: String? = null

    // Variáveis para armazenar data e hora selecionadas
    private var selectedDate: String? = null
    private var selectedTime: String? = null

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
                // Inicia a consulta no DynamoDB
                CoroutineScope(Dispatchers.IO).launch {
                    queryDynamoDB(selectedDate!!, selectedTime!!)
                }
            } else {
                // Exibir mensagem de erro em um modal
                showErrorDialog("Por favor, selecione uma data e uma hora.")
            }
        }
    }

    private suspend fun queryDynamoDB(date: String, time: String) {
        val dynamoDbClient = DynamoDbClient { region = "us-east-1" }

        try {
            val queryRequest = QueryRequest {
                tableName = "table_name"
                keyConditionExpression = "data = :dateVal and hora = :timeVal"
                expressionAttributeValues = mapOf(
                    ":dateVal" to AttributeValue.S(date),
                    ":timeVal" to AttributeValue.S(time)
                )
            }

            val response = dynamoDbClient.query(queryRequest)

            CoroutineScope(Dispatchers.Main).launch {
                // Processa os resultados conforme necessário
                response.items?.forEach { item ->
                    println(item) // ou exiba no UI, como uma lista
                }
            }
        } catch (e: Exception) {
            // Exibe o erro na UI
            CoroutineScope(Dispatchers.Main).launch {
                showErrorDialog("Erro ao consultar o DynamoDB: ${e.message}")
            }
        } finally {
            dynamoDbClient.close()
        }
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