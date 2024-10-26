package com.example.myapplication.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentChatBinding
import com.example.myapplication.ui.adapter.MessageAdapter
import com.example.myapplication.ui.model.Message

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val messages = mutableListOf<Message>()
    private lateinit var adapter: MessageAdapter

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

        // Verifica se há uma primeira mensagem no bundle
        val firstMessage = arguments?.getString("firstMessage")
        if (firstMessage != null) {
            sendMessage(firstMessage)  // Envia a primeira mensagem automaticamente
        }

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
    }


    private fun sendMessage(userMessage: String) {
        // Adiciona a mensagem do usuário à lista
        messages.add(Message(userMessage, true))  // true indica que é mensagem do usuário
        adapter.notifyItemInserted(messages.size - 1)
        binding.recyclerViewMessages.scrollToPosition(messages.size - 1)

        binding.etMessage.text.clear()

        // Simula resposta do bot
        receiveBotMessage("Message sent. Please wait for a reply.")
    }

    private fun receiveBotMessage(botMessage: String) {
        // Adiciona a resposta do bot à lista
        messages.add(Message(botMessage, false))
        adapter.notifyItemInserted(messages.size - 1)
        binding.recyclerViewMessages.scrollToPosition(messages.size - 1)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
