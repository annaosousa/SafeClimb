package com.example.myapplication.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentBotBinding
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

class BotFragment : Fragment() {

    private var _binding: FragmentBotBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBotBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        applyPhoneMask(binding.editPhoneNumber)

        binding.buttonStartTheChat.setOnClickListener {
            val message = binding.editTextMessage.text.toString()

            if (message.isNotBlank()) {
                // Cria um bundle para passar a mensagem
                val bundle = Bundle().apply {
                    putString("firstMessage", message)
                }

                // Navega para o ChatFragment passando a mensagem
                findNavController().navigate(R.id.navigation_bot_dialog, bundle)
            }
        }

    }

    fun applyPhoneMask(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            private val mask = "(##)#####-####"

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isUpdating) return

                var text = s.toString().replace(Regex("[^0-9]"), "")
                if (text.length > 11) {
                    text = text.substring(0, 11)
                }

                var formattedText = ""
                var i = 0
                for (char in mask.toCharArray()) {
                    if (char != '#') {
                        formattedText += char
                    } else if (i < text.length) {
                        formattedText += text[i]
                        i++
                    }
                }

                isUpdating = true
                editText.setText(formattedText)
                editText.setSelection(formattedText.length)
                isUpdating = false
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}