package com.example.myapplication.ui.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.ScanRequest
import com.amazonaws.services.dynamodbv2.model.ScanResult
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentResultBinding
import java.text.SimpleDateFormat
import java.util.*

class ResultFragment : Fragment() {

    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!

    private lateinit var client: AmazonDynamoDBClient

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
        imageResourceId?.let { binding.imagePlaceholder.setImageResource(it) }

        // Inicializar o provedor de credenciais do Amazon Cognito
        val credentialsProvider = CognitoCachingCredentialsProvider(
            requireContext(),
            "eu-north-1:7c834096-3ddd-4282-a809-4390e3e5686a", // ID do grupo de identidades
            Regions.EU_NORTH_1 // Região
        )

        // Inicializando o cliente DynamoDB com as credenciais
        client = AmazonDynamoDBClient(credentialsProvider)
        client.setRegion(Region.getRegion(Regions.EU_NORTH_1))

        // Buscar dados do DynamoDB
        fetchDataFromDynamoDB(mountainName.toString())

        binding.viewMap.setOnClickListener {
            val bundle = Bundle().apply {
                putString("selected_mountain", mountainName)
            }
            findNavController().navigate(R.id.navigation_map_mountain, bundle)
        }

        binding.viewHistory.setOnClickListener {
            val bundle = Bundle().apply {
                putString("mountain_name", mountainName)
            }
            findNavController().navigate(R.id.navigation_history, bundle)
        }

        val query = arguments?.getString("search_query") // Obtendo a string passada
        binding.resultTextView.text = query

        return binding.root
    }

    private fun fetchDataFromDynamoDB(locationName: String) {
        // Criando um valor para o filtro de consulta
        val attributeValue = AttributeValue().withS(locationName)

        // Configurar os parâmetros da consulta, agora filtrando pelo nome da montanha
        val request = ScanRequest()
            .withTableName("safe_climb") // Substitua pelo nome da sua tabela no DynamoDB
            .withFilterExpression("mountain = :mountainName")
            .addExpressionAttributeValuesEntry(":mountainName", attributeValue)

        // Realizando o scan para pegar todos os itens
        Thread {
            try {
                val scanResult: ScanResult = client.scan(request)
                val items = scanResult.items
                if (items.isNotEmpty()) {
                    // Encontrar o item com o timestamp mais recente
                    val latestItem = items.maxByOrNull { item ->
                        val timestamp = item["timestamp"]?.s // "timestamp" é o nome da chave no DynamoDB
                        parseTimestamp(timestamp)
                    }

                    latestItem?.let {
                        val windSpeed = it["wind_speed"]?.n?.toIntOrNull() ?: 0
                        val humidity = it["humidity"]?.n?.toIntOrNull() ?: 0
                        val temperature = it["temperature"]?.n?.toIntOrNull() ?: 0
                        val precipitation = it["precipitation"]?.n?.toIntOrNull() ?: 0

                        // Verificando as condições climáticas
                        val isConditionsGood = (temperature in 10..20) &&
                                (humidity in 30..60) &&
                                (windSpeed in 0..20) &&
                                (precipitation == 0)  // Aceitando precipitação mínima

                        // Exibir a mensagem e ajustar a cor conforme as condições
                        activity?.runOnUiThread {
                            if (isConditionsGood) {
                                binding.resultWeather.text = "These are great conditions. Let's climb?"
                                binding.resultWeather.setTextColor(resources.getColor(R.color.green, null))
                            } else {
                                binding.resultWeather.text = "These are bad conditions. How about another day?"
                                binding.resultWeather.setTextColor(resources.getColor(R.color.route_line, null))
                            }

                            // Exibindo as condições climáticas
                            binding.textWindy.text = "Windy: $windSpeed km/h"
                            binding.textHumidity.text = "Humidity: $humidity%"
                            binding.textTemperature.text = "Temperature: $temperature°C"
                        }
                    }
                } else {
                    showToast("Nenhum dado encontrado")
                }
            } catch (e: Exception) {
                showToast("Erro ao buscar dados: ${e.message}")
            }
        }.start() // Inicia a execução da consulta em uma thread separada
    }

    private fun parseTimestamp(timestamp: String?): Date {
        if (timestamp == null) return Date(0)
        // Formato de timestamp que você está usando
        val format = SimpleDateFormat("yyyy-MM-dd#HH:mm:ss", Locale.getDefault())
        return format.parse(timestamp) ?: Date(0)
    }

    private fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
