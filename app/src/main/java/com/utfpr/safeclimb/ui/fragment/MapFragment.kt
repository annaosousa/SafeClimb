package com.utfpr.safeclimb.ui.fragment

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
import androidx.navigation.fragment.findNavController
import com.utfpr.safeclimb.FirstActivity
import com.utfpr.safeclimb.R
import com.utfpr.safeclimb.databinding.FragmentMapBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.modules.SqliteArchiveTileWriter
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val towers = mutableListOf<Marker>()
    private var lastClosestTower: GeoPoint? = null
    private lateinit var mapView: MapView
    private lateinit var locationManager: LocationManager
    private var userMarker: Marker? = null // Para armazenar o marcador do usuário

    // Listener de localização
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val currentLocation = GeoPoint(location.latitude, location.longitude)
            mapView.controller.setCenter(currentLocation)
            mapView.controller.setZoom(15.0)

            // Configura o marcador do usuário
            if (userMarker == null) {
                userMarker = Marker(mapView).apply {
                    title = "You are here"
                    position = currentLocation
                    icon = ContextCompat.getDrawable(requireContext(), R.drawable.arrow_map_icon) // Ícone de "mãozinha"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    mapView.overlays.add(this)
                }
            } else {
                userMarker?.position = currentLocation
            }

            if (towers.size < 2) {
                towers.add(addRandomTower(currentLocation))
                towers.add(addRandomTower(currentLocation))
            }

            val distanceToFirstTower = calculateDistance(currentLocation, towers[0].position)
            val distanceToSecondTower = calculateDistance(currentLocation, towers[1].position)

            val closestTowerLocation = if (distanceToFirstTower < distanceToSecondTower) {
                towers[0].position
            } else {
                towers[1].position
            }

            // Verifica se a torre mais próxima mudou
            if (lastClosestTower != closestTowerLocation) {
                drawRouteAndCalculateDistance(currentLocation, closestTowerLocation)
                lastClosestTower = closestTowerLocation // Atualiza a última torre mais próxima
            }

            mapView.invalidate()
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

        if (!FirstActivity.isLoggedIn()) {
            mapView = binding.mapView
            locationManager =
                requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val dialog = CheckAuthenticationDialogFragment()
            dialog.show(
                childFragmentManager,
                CheckAuthenticationDialogFragment.TAG
            )
            dialog.setNavController(findNavController())
        }
        else {

            // Configurar o mapa
            Configuration.getInstance().osmdroidBasePath =
                File(requireContext().cacheDir, "osmdroid")
            Configuration.getInstance().osmdroidTileCache =
                File(requireContext().cacheDir, "osmdroid/tiles")
            Configuration.getInstance().load(
                requireContext(),
                requireContext().getSharedPreferences("prefs", Context.MODE_PRIVATE)
            )
            mapView = binding.mapView
            mapView.setTileSource(TileSourceFactory.MAPNIK)
            mapView.setMultiTouchControls(true)

            locationManager =
                requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Obter a última localização conhecida
                val lastKnownLocation =
                    locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                lastKnownLocation?.let {
                    val currentLocation = GeoPoint(it.latitude, it.longitude)
                    mapView.controller.setCenter(currentLocation)
                    mapView.controller.setZoom(15.0)
                }
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000L,
                    10f,
                    locationListener
                )
            } else {
                Toast.makeText(
                    requireContext(),
                    "Permissão de localização necessária!",
                    Toast.LENGTH_SHORT
                ).show()
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    1
                )
            }
        }

        return root
    }

    private fun addRandomTower(center: GeoPoint): Marker {
        val randomLat = center.latitude + Random.nextDouble(-0.005, 0.005)
        val randomLon = center.longitude + Random.nextDouble(-0.005, 0.005)
        val towerLocation = GeoPoint(randomLat, randomLon)

        val randomTower = Marker(mapView).apply {
            title = "Tower"
            position = towerLocation
            icon = ContextCompat.getDrawable(requireContext(), R.drawable.tower)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays.add(this)
        }

        mapView.invalidate()
        return randomTower // Retorne o objeto Marker em vez de GeoPoint
    }

    private fun drawRouteAndCalculateDistance(startPoint: GeoPoint, endPoint: GeoPoint) {
        val results = FloatArray(1)
        Location.distanceBetween(
            startPoint.latitude, startPoint.longitude,
            endPoint.latitude, endPoint.longitude,
            results
        )
        val distanceInMeters = results[0]

        Toast.makeText(requireContext(), "Distance until tower: ${distanceInMeters.toInt()} meters", Toast.LENGTH_SHORT).show()

        val polyline = Polyline(mapView).apply {
            addPoint(startPoint)
            addPoint(endPoint)
            color = ContextCompat.getColor(requireContext(), R.color.route_line)
            width = 5f

            setOnClickListener { polyline, mapView, geoPoint ->
                showDistanceInfoWindow(distanceInMeters, (startPoint.latitude + endPoint.latitude) / 2, (startPoint.longitude + endPoint.longitude) / 2)
                true
            }
        }

        mapView.overlays.add(polyline)
        mapView.invalidate()
    }

    private fun showDistanceInfoWindow(distance: Float, lat: Double, lon: Double) {
        Marker(mapView).apply {
            position = GeoPoint(lat, lon)
            title = "Distance: ${distance.toInt()} meters"

            showInfoWindow()
        }
        mapView.invalidate()
    }

    private fun calculateDistance(startPoint: GeoPoint, endPoint: GeoPoint): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            startPoint.latitude, startPoint.longitude,
            endPoint.latitude, endPoint.longitude,
            results
        )
        return results[0]
    }

    private fun downloadTilesAround(center: GeoPoint) {
        val boundingBox = BoundingBox(
            center.latitude + 0.05,
            center.longitude + 0.05,
            center.latitude - 0.05,
            center.longitude - 0.05
        )

        CoroutineScope(Dispatchers.IO).launch {
            val tileFile = File(requireContext().cacheDir, "osmdroid/tiles.sqlite")
            val tileWriter = SqliteArchiveTileWriter(tileFile.absolutePath)

            // Definir a faixa de zoom para o download dos tiles
            for (zoom in 12..15) {
                val tileIndices = getTileIndicesInBoundingBox(boundingBox, zoom)
                for ((x, y) in tileIndices) {
                    val tileUrl = getTileUrl(x, y, zoom)
                    downloadTile(tileUrl, zoom, x, y)
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Tiles baixados para visualização offline", Toast.LENGTH_SHORT).show()
            }
        }
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