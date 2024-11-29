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

        val mountainName = arguments?.getString("mountain_name")?: "Default Location"
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
        client.setRegion(Region.getRegion(Regions.US_EAST_1))

        // Buscar dados do DynamoDB
        fetchDataFromDynamoDB(mountainName)

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

    @SuppressLint("SetTextI18n")
    private fun fetchDataFromDynamoDB(locationName: String) {
        val attributeValue = AttributeValue().withS(locationName)

        val request = ScanRequest()
            .withTableName("safe_climb")
            .withFilterExpression("mountain = :mountainName")
            .addExpressionAttributeValuesEntry(":mountainName", attributeValue)

        Thread {
            try {
                val scanResult: ScanResult = client.scan(request)
                val items = scanResult.items
                if (items.isNotEmpty()) {
                    val latestItem = items.maxByOrNull { item ->
                        val timestamp = item["timestamp"]?.s
                        parseTimestamp(timestamp)?.time ?: 0L
                    }

                    latestItem?.let {
                        val payload = it["payload"]?.m
                        val windSpeed = payload?.get("wind_speed")?.n?.toDoubleOrNull() ?: 0.0
                        val humidity = payload?.get("humidity")?.n?.toIntOrNull() ?: 0
                        val temperature = payload?.get("temperature")?.n?.toDoubleOrNull() ?: 0.0
                        val precipitation = payload?.get("precipitation")?.n?.toIntOrNull() ?: 0
                        val soil = payload?.get("soil_moisture")?.n?.toDoubleOrNull() ?: 0.0

                        val isConditionsGood = (temperature in 10.0..20.0) &&
                                (humidity in 30..60) &&
                                (windSpeed in 0.0..20.0)

                        activity?.runOnUiThread {
                            if (isConditionsGood) {
                                binding.resultWeather.text = "These are great conditions. Let's climb?"
                                binding.resultWeather.setTextColor(resources.getColor(R.color.green, null))
                            } else {
                                binding.resultWeather.text = "These are bad conditions. How about another day?"
                                binding.resultWeather.setTextColor(resources.getColor(R.color.route_line, null))
                            }

                            // Exibindo as condições climáticas
                            binding.textWindy.text = "Windy: %.1f km/h".format(windSpeed)
                            binding.textHumidity.text = "Humidity: $humidity%"
                            binding.textTemperature.text = "Temperature: %.1f°C".format(temperature)
                            binding.textPrecipitation.text = "Precipitation: $precipitation mm"
                            binding.textSoil.text = "Soil moisture: $soil%"
                        }
                    }
                } else {
                    showToast("Nenhum dado encontrado")
                }
            } catch (e: Exception) {
                showToast("Erro ao buscar dados: ${e.message}")
            }
        }.start()
    }

    private fun parseTimestamp(timestamp: String?): Date? {
        if (timestamp == null) return null
        return try {
            val format = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
            format.parse(timestamp)
        } catch (e: Exception) {
            null
        }
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
