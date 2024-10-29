package com.example.myapplication.ui.fragment

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.FragmentMapBinding
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.concurrent.Executors
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var mapView: MapView

    companion object {
        private const val STORAGE_PERMISSION_CODE = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        Configuration.getInstance().osmdroidBasePath = File(requireContext().cacheDir, "osmdroid")
        Configuration.getInstance().osmdroidTileCache = File(requireContext().cacheDir, "osmdroid/tiles")

        _binding = FragmentMapBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Configurar o mapa
        Configuration.getInstance().load(requireContext(), requireContext().getSharedPreferences("prefs", Context.MODE_PRIVATE))
        mapView = binding.mapView
        mapView.setMultiTouchControls(true) // Permite zoom por gesto

        // Centralizar o mapa em um ponto específico (exemplo: Pico do Paraná)
        val startPoint = GeoPoint(-25.2427, -48.8395) // Coordenadas do Pico do Paraná
        mapView.controller.setZoom(15.0) // Zoom inicial
        mapView.controller.setCenter(startPoint)

        // Adicionar um marcador no centro
        val marker = Marker(mapView)
        marker.position = startPoint
        marker.title = "Pico do Paraná"
        mapView.overlays.add(marker)

        // Configurar ação do botão de download
        binding.buttonFindHistory.setOnClickListener {
            if (checkPermission()) {
                downloadMap()
            } else {
                requestStoragePermission()
            }
        }

        return root
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            STORAGE_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                downloadMap()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Permission denied. Can't download the map.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun downloadMap() {
        // URL do arquivo que será baixado
        val mapUrl = "https://example.com/map.pdf" // Substitua pela URL do arquivo real

        // Diretório de destino
        val downloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        val fileName = "map.pdf"
        val file = File(downloadDirectory, fileName)

        // Iniciar download em thread separada
        Executors.newSingleThreadExecutor().execute {
            try {
                val url = URL(mapUrl)
                url.openStream().use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }

                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Map downloaded successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Failed to download map.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
