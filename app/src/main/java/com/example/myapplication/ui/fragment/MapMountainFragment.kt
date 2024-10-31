// MapMountainFragment.kt
package com.example.myapplication.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.myapplication.ui.data.MountainData
import com.example.myapplication.databinding.FragmentMapMountainBinding
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

class MapMountainFragment : Fragment() {

    private var _binding: FragmentMapMountainBinding? = null
    private val binding get() = _binding!!
    private lateinit var mapView: MapView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Configuration.getInstance().osmdroidBasePath = File(requireContext().cacheDir, "osmdroid")
        Configuration.getInstance().osmdroidTileCache = File(requireContext().cacheDir, "osmdroid/tiles")

        _binding = FragmentMapMountainBinding.inflate(inflater, container, false)
        val root: View = binding.root

        Configuration.getInstance().load(requireContext(), requireContext().getSharedPreferences("prefs", Context.MODE_PRIVATE))
        mapView = binding.mapView
        mapView.setMultiTouchControls(true)

        // Obter o nome da montanha passada como argumento
        val selectedMountain = arguments?.getString("selected_mountain")
        val coordinates = MountainData.mountains[selectedMountain]

        // Centralizar o mapa nas coordenadas
        val startPoint = coordinates?.let { GeoPoint(it.first, coordinates.second) }
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(startPoint)

        // Adicionar um marcador
        val marker = Marker(mapView)
        marker.position = startPoint
        marker.title = selectedMountain ?: "Montanha Desconhecida"
        mapView.overlays.add(marker)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
