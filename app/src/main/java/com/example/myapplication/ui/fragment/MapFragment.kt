package com.example.myapplication.ui.fragment

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
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
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.modules.SqliteArchiveTileWriter
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.net.HttpURLConnection

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var mapView: MapView
    private lateinit var locationManager: LocationManager
    private lateinit var titleWriter: SqliteArchiveTileWriter

    companion object {
        private const val STORAGE_PERMISSION_CODE = 1001
    }

    // Listener de localização como uma instância anônima
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val currentLocation = GeoPoint(location.latitude, location.longitude)
            mapView.controller.setCenter(currentLocation)
            mapView.controller.setZoom(15.0)

            // Adiciona um marcador na localização atual
            val marker = Marker(mapView)
            marker.position = currentLocation
            marker.title = "Você está aqui"
            mapView.overlays.clear()
            mapView.overlays.add(marker)

            // Baixar os tiles ao redor da localização atual para uso offline
            downloadTilesAround(currentLocation)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Configurar o mapa

        Configuration.getInstance().osmdroidBasePath = File(requireContext().cacheDir, "osmdroid")
        Configuration.getInstance().osmdroidTileCache = File(requireContext().cacheDir, "osmdroid/tiles")
        Configuration.getInstance().load(requireContext(), requireContext().getSharedPreferences("prefs", Context.MODE_PRIVATE))
        mapView = binding.mapView
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true) // Permite zoom por gesto

        // Inicializar o gerenciador de localização
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 10f, locationListener)
        } else {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }

        return root
    }

    private fun downloadTilesAround(center: GeoPoint) {
        val boundingBox = BoundingBox(
            center.latitude + 0.05,
            center.longitude + 0.05,
            center.latitude - 0.05,
            center.longitude - 0.05
        )

        // Inicie o download e armazenamento dos tiles ao redor da localização atual
        val tileFile = File(requireContext().cacheDir, "osmdroid/tiles.sqlite")
        val tileWriter = SqliteArchiveTileWriter(tileFile.absolutePath)
        val provider = mapView.tileProvider.tileSource

        // Definir a faixa de zoom para o download dos tiles
        for (zoom in 12..15) {
            val tileIndices = getTileIndicesInBoundingBox(boundingBox, zoom)
            for ((x, y) in tileIndices) {
                val tileUrl = getTileUrl(x, y, zoom)
                downloadTile(tileUrl, zoom, x, y)
            }
        }

        Toast.makeText(requireContext(), "Tiles baixados para visualização offline", Toast.LENGTH_SHORT).show()
    }

    private fun getTileIndicesInBoundingBox(boundingBox: BoundingBox, zoom: Int): List<Pair<Int, Int>> {
        val tileIndices = mutableListOf<Pair<Int, Int>>()

        val northWest = GeoPoint(boundingBox.latNorth, boundingBox.lonWest)
        val southEast = GeoPoint(boundingBox.latSouth, boundingBox.lonEast)

        val xStart = long2tileX(northWest.longitude, zoom)
        val xEnd = long2tileX(southEast.longitude, zoom)
        val yStart = lat2tileY(northWest.latitude, zoom)
        val yEnd = lat2tileY(southEast.latitude, zoom)

        for (x in xStart..xEnd) {
            for (y in yStart..yEnd) {
                tileIndices.add(Pair(x, y))
            }
        }
        return tileIndices
    }

    private fun getTileUrl(x: Int, y: Int, zoom: Int): String {
        return "https://tile.openstreetmap.org/$zoom/$x/$y.png"
    }

    private fun downloadTile(url: String, zoom: Int, x: Int, y: Int) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val tileFile = File(requireContext().cacheDir, "osmdroid/tiles/$zoom/$x/$y.png")
                tileFile.parentFile?.mkdirs()

                connection.inputStream.use { input ->
                    FileOutputStream(tileFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            connection.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun long2tileX(lon: Double, zoom: Int): Int {
        return ((lon + 180) / 360 * (1 shl zoom)).toInt()
    }

    private fun lat2tileY(lat: Double, zoom: Int): Int {
        val radLat = Math.toRadians(lat)
        return ((1.0 - Math.log(Math.tan(radLat) + 1 / Math.cos(radLat)) / Math.PI) / 2 * (1 shl zoom)).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        locationManager.removeUpdates(locationListener)
        _binding = null
    }
}
