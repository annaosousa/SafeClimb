package com.example.myapplication.ui.fragment

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.FragmentChatBinding
import com.example.myapplication.ui.adapter.MessageAdapter
import com.example.myapplication.ui.data.Message
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private var isFirstMessageSent = false

    private val messages = mutableListOf<Message>()
    private lateinit var adapter: MessageAdapter

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private val esp32DeviceAddress = "00:00:00:00:00:00" // endereço MAC do ESP32
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // UUID padrão para SPP

    companion object {
        private const val BLUETOOTH_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Configura o RecyclerView
        adapter = MessageAdapter(messages)
        binding.recyclerViewMessages.adapter = adapter
        binding.recyclerViewMessages.layoutManager = LinearLayoutManager(context)

        // Configura o botão de envio de mensagem
        binding.btnSend.setOnClickListener {
            val userMessage = binding.etMessage.text.toString()
            if (userMessage.isNotBlank()) {
                sendMessage(userMessage)
            }
        }

        // Configura o comportamento de rolagem ao enviar mensagem
        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val userMessage = binding.etMessage.text.toString()
                if (userMessage.isNotBlank()) {
                    sendMessage(userMessage)
                }
                true
            } else {
                false
            }
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requestBluetoothPermissions()

        val phoneNumber = arguments?.getString("phoneNumber")
        val firstMessage = arguments?.getString("firstMessage")

        showLoadingOverlay()
        setupBluetoothConnection()

        if (phoneNumber != null) {
            sendPhoneNumberToESP32(phoneNumber) // Envia o número de telefone primeiro
        }

        if (firstMessage != null) {
            sendMessage(firstMessage) // Envia a primeira mensagem automaticamente após o número
        }
    }

    private fun sendPhoneNumberToESP32(phoneNumber: String) {
        if (outputStream != null) {
            try {
                outputStream?.write(phoneNumber.toByteArray())
                receiveBotMessage("Número de telefone enviado para o ESP32.")
            } catch (e: IOException) {
                receiveBotMessage("Erro ao enviar número de telefone: ${e.message}")
                Log.e("ChatFragment", "Erro ao enviar número de telefone para ESP32", e)
            }
        } else {
            receiveBotMessage("Conexão Bluetooth não estabelecida.")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                setupBluetoothConnection()
            } else {
                receiveBotMessage("Permissões de Bluetooth não concedidas.")
            }
        }
    }

    private fun requestBluetoothPermissions() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                BLUETOOTH_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun setupBluetoothConnection() {
        showLoadingOverlay() // Exibe o carregamento ao iniciar a conexão

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            receiveBotMessage("Bluetooth não disponível ou não ativado.")
            hideLoadingOverlay() // Esconde o carregamento se o Bluetooth não estiver disponível
            return
        }

        val device: BluetoothDevice? = bluetoothAdapter.getRemoteDevice(esp32DeviceAddress)
        Thread {
            try {
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // Saia do método se a permissão não foi concedida
                    return@Thread
                }
                bluetoothSocket = device?.createRfcommSocketToServiceRecord(uuid)
                bluetoothSocket?.connect()
                outputStream = bluetoothSocket?.outputStream
                inputStream = bluetoothSocket?.inputStream

                requireActivity().runOnUiThread {
                    hideLoadingOverlay() // Oculta o carregamento após a conexão
                    receiveBotMessage("Conectado ao ESP32 via Bluetooth.")
                    startListeningForMessages() // Inicia a escuta de mensagens do ESP32
                }
            } catch (e: IOException) {
                requireActivity().runOnUiThread {
                    hideLoadingOverlay() // Oculta o carregamento ao falhar
                    receiveBotMessage("Erro ao conectar com ESP32: ${e.message}")
                }
                Log.e("ChatFragment", "Erro ao conectar ao ESP32", e)
                try {
                    bluetoothSocket?.close()
                } catch (closeException: IOException) {
                    Log.e("ChatFragment", "Erro ao fechar socket", closeException)
                }
            }
        }.start()
    }

    private fun showLoadingOverlay() {
        binding.loadingOverlay.visibility = View.VISIBLE
    }

    private fun hideLoadingOverlay() {
        binding.loadingOverlay.visibility = View.GONE
    }

    private fun startListeningForMessages() {
        Thread {
            try {
                val buffer = ByteArray(1024)
                while (true) {
                    val bytesRead = inputStream?.read(buffer) ?: -1
                    if (bytesRead > 0) {
                        val messageReceived = String(buffer, 0, bytesRead)
                        Handler(Looper.getMainLooper()).post {
                            receiveBotMessage(messageReceived) // Exibe a mensagem recebida como resposta do bot
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e("ChatFragment", "Erro ao ler mensagem do ESP32", e)
                receiveBotMessage("Conexão perdida com o ESP32.")
            }
        }.start()
    }

    private fun sendMessage(userMessage: String) {
        messages.add(Message(userMessage, true))
        adapter.notifyItemInserted(messages.size - 1)
        binding.recyclerViewMessages.scrollToPosition(messages.size - 1)
        binding.etMessage.text.clear()

        sendMessageToESP32(userMessage)
    }

    private fun sendMessageToESP32(message: String) {
        if (outputStream != null) {
            try {
                outputStream?.write(message.toByteArray())
                receiveBotMessage("Mensagem enviada para o ESP32.")
                isFirstMessageSent = true
            } catch (e: IOException) {
                receiveBotMessage("Erro ao enviar mensagem: ${e.message}")
                Log.e("ChatFragment", "Erro ao enviar mensagem para ESP32", e)
            }
        } else {
            if (isFirstMessageSent) {
                receiveBotMessage("Conexão Bluetooth não estabelecida.")
            }
        }
    }

    private fun receiveBotMessage(botMessage: String) {
        messages.add(Message(botMessage, false))
        adapter.notifyItemInserted(messages.size - 1)
        binding.recyclerViewMessages.scrollToPosition(messages.size - 1)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            outputStream?.close()
            inputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e("ChatFragment", "Erro ao fechar conexão Bluetooth", e)
        }
        _binding = null
    }
}
