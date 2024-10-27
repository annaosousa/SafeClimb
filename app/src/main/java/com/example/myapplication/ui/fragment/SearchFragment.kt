package com.example.myapplication.ui.fragment

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentSearchBinding
import com.example.myapplication.ui.adapter.Item
import com.example.myapplication.ui.adapter.SearchItemAdapter
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter

class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    // BroadcastReceiver para monitorar o estado do Bluetooth
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_ON) {
                    showModal() // Exibe o modal quando o Bluetooth é ativado
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Configuração do SearchView
        setupSearchView()

        // Verificação inicial do Bluetooth
        val bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
            showModal()
        }

        // Registro do BroadcastReceiver para ouvir mudanças no estado do Bluetooth
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        requireContext().registerReceiver(bluetoothReceiver, filter)

        // Configuração do RecyclerView
        setUpPopularMountainsRecyclerView()
        setUpBestViewRecyclerView()

        return root
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    val navController = findNavController()
                    val bundle = Bundle().apply {
                        putString("search_query", it) // Passa a consulta para o ResultFragment
                    }
                    navController.navigate(R.id.navigation_result, bundle)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

    private fun setUpPopularMountainsRecyclerView() {
        val popularMountains = listOf(
            Item("Pico do Paraná", "Montanha mais popular"),
            Item("Pico Marumbi", "Outra montanha popular"),
            Item("Morro do Anhangava", "Mais uma popular")
        )

        binding.popularMountainsRecycler.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        val adapter = SearchItemAdapter(popularMountains)
        binding.popularMountainsRecycler.adapter = adapter
    }

    private fun setUpBestViewRecyclerView() {
        val items = listOf(
            Item("Pico do Caratuva", "Descrição da montanha 1"),
            Item("Morro do Araçatuba", "Descrição da montanha 2"),
            Item("Morro do Canal", "Descrição da montanha 3")
        )

        binding.bestViewRecycler.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        val adapter = SearchItemAdapter(items)
        binding.bestViewRecycler.adapter = adapter
    }

    private fun showModal() {
        val dialog = CustomDialogFragment()
        dialog.show(parentFragmentManager, "CustomDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        requireContext().unregisterReceiver(bluetoothReceiver) // Remove o registro do BroadcastReceiver
    }
}