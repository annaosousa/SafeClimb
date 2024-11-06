package com.example.myapplication.ui.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.DeviceListDialogBinding
import com.example.myapplication.databinding.FragmentChatBinding
import com.example.myapplication.ui.adapter.MessageAdapter
import com.example.myapplication.ui.data.Message
import java.util.*

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private var isFirstMessageSent = false

    private val messages = mutableListOf<Message>()
    private lateinit var adapter: MessageAdapter

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothGatt: BluetoothGatt? = null
    private var gattCharacteristic: BluetoothGattCharacteristic? = null
    private val uuidService: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val uuidCharacteristic: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    companion object {
        private const val BLUETOOTH_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        val root: View = binding.root

        adapter = MessageAdapter(messages)
        binding.recyclerViewMessages.adapter = adapter
        binding.recyclerViewMessages.layoutManager = LinearLayoutManager(context)

        binding.btnSend.setOnClickListener {
            val userMessage = binding.etMessage.text.toString()
            if (userMessage.isNotBlank()) {
                sendMessage(userMessage)
            }
        }

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
        showDeviceListDialog()

        if (phoneNumber != null) {
            sendPhoneNumberToESP32(phoneNumber)
        }

        if (firstMessage != null) {
            sendMessage(firstMessage)
        }
    }

    private fun showDeviceListDialog() {
        val dialogBinding = DeviceListDialogBinding.inflate(LayoutInflater.from(context))
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Dispositivos Bluetooth")
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnScan.setOnClickListener {
            scanDevices(dialogBinding)
        }

        dialog.show()
    }

    private fun scanDevices(dialogBinding: DeviceListDialogBinding) {
        if (checkBluetoothPermissions()) {
            try {
                val deviceNames = mutableListOf<String>()
                val deviceMap = mutableMapOf<String, BluetoothDevice>()

                // BroadcastReceiver para capturar dispositivos encontrados
                val discoveryReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        val action = intent.action
                        if (BluetoothDevice.ACTION_FOUND == action) {
                            val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            device?.let {
                                val deviceName = it.name ?: "Desconhecido"

                                if (!deviceMap.containsKey(device.address)) {
                                    deviceNames.add(deviceName)
                                    deviceMap[device.address] = device

                                    (dialogBinding.deviceList.adapter as ArrayAdapter<*>).notifyDataSetChanged()
                                }
                            }
                        }
                    }
                }

                // Configura o adaptador para exibir os dispositivos encontrados
                dialogBinding.deviceList.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    deviceNames
                )

                // Configura o clique nos itens da lista para conectar ao dispositivo
                dialogBinding.deviceList.setOnItemClickListener { _, _, position, _ ->
                    val selectedDeviceAddress = deviceMap.keys.elementAt(position)
                    val device = deviceMap[selectedDeviceAddress]
                    device?.let {
                        connectToDevice(it)
                    }
                }

                // Registra o BroadcastReceiver para descobrir dispositivos
                val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
                requireContext().registerReceiver(discoveryReceiver, filter)

                // Inicia a varredura para dispositivos Bluetooth de forma contínua
                bluetoothAdapter?.startDiscovery()

                // Cancela a busca quando o fragmento ou atividade for destruído
                viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
                    override fun onDestroy(owner: LifecycleOwner) {
                        requireContext().unregisterReceiver(discoveryReceiver)
                        bluetoothAdapter?.cancelDiscovery()
                    }
                })

            } catch (e: SecurityException) {
                Log.e("ChatFragment", "Permission denied: Unable to scan devices", e)
                receiveBotMessage("Unable to scan: Bluetooth permissions are required.")
            }
        } else {
            requestBluetoothPermissions()
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (checkBluetoothPermissions()) {
            try {
                bluetoothGatt = device.connectGatt(requireContext(), false, gattCallback)
            } catch (e: SecurityException) {
                Log.e("ChatFragment", "Permission denied: Unable to connect to device", e)
                receiveBotMessage("Unable to connect: Bluetooth permissions are required.")
            }
        } else {
            receiveBotMessage("Bluetooth permissions not granted.")
            requestBluetoothPermissions()
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (!checkBluetoothPermissions()) {
                requestBluetoothPermissions()
                return
            }

            if (newState == BluetoothGatt.STATE_CONNECTED) {
                try {
                    gatt.discoverServices()
                } catch (e: SecurityException) {
                    Log.e("ChatFragment", "Permission denied: Unable to discover services", e)
                    receiveBotMessage("Unable to discover services: Bluetooth permissions are required.")
                }
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                receiveBotMessage("Disconnected from ESP32.")
                try {
                    bluetoothGatt?.close()
                } catch (e: SecurityException) {
                    Log.e("ChatFragment", "Permission denied: Unable to close connection", e)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val service = gatt.getService(uuidService)
            gattCharacteristic = service?.getCharacteristic(uuidCharacteristic)
            receiveBotMessage("Connected and ready for communication.")

            startListeningForMessages()
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic
        ) {
            val messageReceived = characteristic.getStringValue(0)
            Handler(Looper.getMainLooper()).post {
                receiveBotMessage(messageReceived)
            }
        }
    }

    private fun startListeningForMessages() {
        gattCharacteristic?.let { characteristic ->
            if (checkBluetoothPermissions()) {
                try {
                    bluetoothGatt?.setCharacteristicNotification(characteristic, true)
                    val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                    descriptor?.let {
                        it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        bluetoothGatt?.writeDescriptor(it)
                    }
                } catch (e: SecurityException) {
                    Log.e("ChatFragment", "Permission denied: Unable to start listening for messages", e)
                    receiveBotMessage("Unable to listen for messages: Bluetooth permissions are required.")
                }
            } else {
                requestBluetoothPermissions()
            }
        } ?: receiveBotMessage("Characteristic not found. Unable to listen for messages.")
    }

    private fun sendPhoneNumberToESP32(phoneNumber: String) {
        gattCharacteristic?.let {
            if (checkBluetoothPermissions()) {
                try {
                    it.setValue(phoneNumber.toByteArray())
                    bluetoothGatt?.writeCharacteristic(it)
                    receiveBotMessage("Phone number sent to ESP32.")
                } catch (e: SecurityException) {
                    Log.e("ChatFragment", "Permission denied: Unable to send phone number", e)
                    receiveBotMessage("Unable to send phone number: Bluetooth permissions are required.")
                }
            } else {
                requestBluetoothPermissions()
            }
        } ?: receiveBotMessage("Bluetooth connection not established.")
    }

    private fun sendMessage(userMessage: String) {
        messages.add(Message(userMessage, true))
        adapter.notifyItemInserted(messages.size - 1)
        binding.recyclerViewMessages.scrollToPosition(messages.size - 1)
        binding.etMessage.text.clear()

        sendMessageToESP32(userMessage)
    }

    private fun sendMessageToESP32(message: String) {
        gattCharacteristic?.let {
            if (checkBluetoothPermissions()) {
                try {
                    it.setValue(message.toByteArray())
                    bluetoothGatt?.writeCharacteristic(it)
                    receiveBotMessage("Message sent to ESP32.")
                    isFirstMessageSent = true
                } catch (e: SecurityException) {
                    Log.e("ChatFragment", "Permission denied: Unable to send message", e)
                    receiveBotMessage("Unable to send message: Bluetooth permissions are required.")
                }
            } else {
                requestBluetoothPermissions()
            }
        } ?: receiveBotMessage("Bluetooth connection not established.")
    }

    private fun receiveBotMessage(botMessage: String) {
        messages.add(Message(botMessage, false))
        adapter.notifyItemInserted(messages.size - 1)
        binding.recyclerViewMessages.scrollToPosition(messages.size - 1)
    }

    private fun checkBluetoothPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestBluetoothPermissions() {
        if (!checkBluetoothPermissions()) {
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

    private fun showLoadingOverlay() {
        binding.loadingOverlay.visibility = View.VISIBLE
    }

    private fun hideLoadingOverlay() {
        binding.loadingOverlay.visibility = View.GONE
    }

    @SuppressLint("MissingPermission")
    override fun onDestroyView() {
        super.onDestroyView()
        bluetoothGatt?.close()
        _binding = null
    }
}
