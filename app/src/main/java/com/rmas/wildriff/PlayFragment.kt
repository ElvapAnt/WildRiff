package com.rmas.wildriff

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.rmas.wildriff.data.Riff
import com.rmas.wildriff.databinding.FragmentPlayBinding
import com.rmas.wildriff.model.CurrentLocationViewModel
import com.rmas.wildriff.model.CustomMapView
import com.rmas.wildriff.model.LocationViewModel
import com.rmas.wildriff.model.OtherRiffsViewModel
import com.rmas.wildriff.model.SelectedRiffViewModel
import com.rmas.wildriff.model.SharedViewModel
import com.rmas.wildriff.model.UserViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.IOException

class PlayFragment: Fragment() {
    private var _binding: FragmentPlayBinding? = null
    private val binding get() = _binding!!
    private val userViewModel: UserViewModel by activityViewModels()
    private val otherRiffsViewModel: OtherRiffsViewModel by activityViewModels()
    private lateinit var map: CustomMapView
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private var userId :String? = null
    private lateinit var sharedViewModel: SharedViewModel
    private val locationViewModel : LocationViewModel by activityViewModels()
    private var mediaPlayer: MediaPlayer? = null

    private val currentLocationViewModel: CurrentLocationViewModel by activityViewModels()

    private val selectedRiffViewModel: SelectedRiffViewModel by activityViewModels()

    private var userLocation: GeoPoint? = null
    private var clickedMarker: Marker? = null
    private val TOUCH_THRESHOLD = 10

    private var isPlaying = false

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        sharedViewModel.userId.observe(this, Observer { id ->
            userId = id
        })

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                setMyLocationOverlay()
            } else {
                Toast.makeText(requireContext(), "Access denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPlayBinding.inflate(inflater, container, false)

        currentLocationViewModel.currLatitude.observe(viewLifecycleOwner){
        }
        currentLocationViewModel.currLatitude.observe(viewLifecycleOwner){
        }
        selectedRiffViewModel.selectedRiff.observe(viewLifecycleOwner){
        }

        userId = sharedViewModel.userId.value

        userViewModel.saveUserData(userId!!) {
            println("Successfully saved the users data in HOME! : ${it?.username}")
        }

        otherRiffsViewModel.fetchOtherRiffs(sharedViewModel.userId.value!!) { success,error->
            if(success) {
                Toast.makeText(requireContext(), "Map updated!", Toast.LENGTH_SHORT).show()
                val pitch = arguments?.getString("pitch") ?: ""
                val tonality = arguments?.getString("tonality") ?: ""
                val key = arguments?.getString("key") ?: ""
                val radius = arguments?.getInt("radius", 0) ?: 0

                updateMap(otherRiffsViewModel.otherRiffsList.value ?: emptyList(), pitch, tonality, key, radius)

            } else{
                Toast.makeText(requireContext(), "Failed with $error", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var ctx: Context? = activity!!.applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx!!))
        map = binding.playMapView
        map.setMultiTouchControls(true)
        map.setTileSource(TileSourceFactory.MAPNIK)

        if((ActivityCompat.checkSelfPermission(requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) &&
            ActivityCompat.checkSelfPermission(requireActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }else{
            setMyLocationOverlay()
            requestLocationUpdate()
        }

        binding.buttonFilter.setOnClickListener {
            findNavController().navigate(R.id.action_PlayFragment_to_PlayFilterFragment)
        }

        var startX = 0f
        var startY = 0f

        map.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                }
                MotionEvent.ACTION_UP -> {
                    val endX = event.x
                    val endY = event.y

                    val distanceX = Math.abs(endX - startX)
                    val distanceY = Math.abs(endY - startY)

                    if (distanceX < TOUCH_THRESHOLD && distanceY < TOUCH_THRESHOLD) {
                        val x = event.x.toInt()
                        val y = event.y.toInt()
                        val projection = map.projection
                        val geoPoint = projection.fromPixels(x, y)

                        val markerList = map.overlays
                            .filterIsInstance<Marker>()
                            .filter { marker -> marker.position.latitude == geoPoint.latitude && marker.position.longitude == geoPoint.longitude }

                        if (markerList.isNotEmpty()) {
                            clickedMarker = markerList.first()
                            v.performClick()
                        }
                    }
                }
            }
            false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    private fun updateMap(riffsList : List<Riff>,
                          pitch : String?, tonality:String?,
                          key:String?,
                          radius:Int?) {

        map.overlays.clear()
        for (riff in riffsList) {
            if (riff.longitude != 0.0 && riff.latitude != 0.0) {

                val propertyFilter = checkPropertyFilter(riff, pitch, tonality, key)
                val radiusFilter = checkRadiusFilter(riff, radius)

                if (propertyFilter && radiusFilter) {
                    val marker = Marker(map)
                    marker.position = GeoPoint(riff.latitude, riff.longitude)
                    marker.icon = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.riff_map_marker
                    )
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    marker.title = riff.name
                    marker.isDraggable = false

                    marker.setOnMarkerClickListener { marker, mapView ->
                        if (isPlaying && marker == clickedMarker) {
                            stopPlayingRiff(marker)
                        } else {
                            playRiff(marker)
                        }
                        true
                    }
                    map.overlays.add(marker)
                }
            }
        }
    }

    private fun setMyLocationOverlay() {
        val myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(activity),map)
        myLocationOverlay.enableMyLocation()
        map.overlays.add(myLocationOverlay)

    }
    @SuppressLint("MissingPermission")
    private fun requestLocationUpdate() {
        val locationManager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val locationProvider = LocationManager.GPS_PROVIDER

        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                userLocation = GeoPoint(location.latitude, location.longitude)

                currentLocationViewModel.setLocation(location.latitude,location.longitude)

                map.controller.setCenter(userLocation)
                map.controller.animateTo(userLocation)
                map.controller.setZoom(15.0)

                locationManager.removeUpdates(this)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                // Handle status changes if needed
            }

            override fun onProviderEnabled(provider: String) {
                // Provider enabled, handle it if needed
            }

            override fun onProviderDisabled(provider: String) {
                // Provider disabled, handle it if needed
            }
        }
        locationManager.requestSingleUpdate(locationProvider, locationListener, null)
    }
    private fun checkPropertyFilter(riff: Riff, pitch: String?, tonality: String?, key: String?): Boolean {
        if (pitch == "" && tonality == "" && key == "") {
            return true
        }
        return (pitch == "" || riff.pitch == pitch) &&
                (tonality == "" || riff.tonality == tonality) &&
                (key == "" || riff.key == key)
    }
    private fun checkRadiusFilter(riff: Riff, radius: Int?): Boolean {
        // Check if radius filter is null
        if (radius == 0) {
            return true // No radius filter, all riffs pass
        }
        val distance = calculateDistance(
            currentLocationViewModel.currLatitude.value?:0.0,
            currentLocationViewModel.currLongitude.value?:0.0,
            riff.latitude,
            riff.longitude)
        return distance <= radius!!
    }
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val radius = 6371
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = (Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2))
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val distance = radius * c
        return distance
    }

    private fun playRiff(marker: Marker) {
        val riff = getRiffFromMarker(marker)
        if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
            mediaPlayer?.pause()
            marker.icon = ContextCompat.getDrawable(requireContext(), R.drawable.reset_button)
            showGradeFragment(riff)
        } else {
            startPlayingRiff(riff)
            marker.icon = ContextCompat.getDrawable(requireContext(), R.drawable.play_button)
        }
    }
    private fun showGradeFragment(riff: Riff) {
        selectedRiffViewModel.setRiff(riff)
        findNavController().navigate(R.id.action_PlayFragment_to_GradeFragment)
    }

    private fun stopPlayingRiff(marker: Marker?) {
        if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
            mediaPlayer?.pause()
            marker?.icon = ContextCompat.getDrawable(requireContext(), R.drawable.reset_button)
        }
    }
    private fun startPlayingRiff(riff: Riff) {
        if (mediaPlayer != null) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
        val riffId = riff.riffId
        val userIdRiffsPath = "${riff.userId}_riffs"
        val storageRef: StorageReference = FirebaseStorage.getInstance("gs://wildriff-84a61.appspot.com")
            .reference.child(userIdRiffsPath).child(riffId!!)

        storageRef.downloadUrl
            .addOnSuccessListener { uri ->
                try {
                    if (mediaPlayer == null) {
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(requireActivity(), uri)
                            setOnCompletionListener {
                                stopPlayingRiff(clickedMarker)
                            }
                            prepareAsync()
                            setOnPreparedListener {
                                start()
                            }
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    requireContext(),
                    "Failed to play file: $exception",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun getRiffFromMarker(marker: Marker): Riff {
        val riffList = otherRiffsViewModel.otherRiffsList.value ?: emptyList()
        return riffList.firstOrNull { riff ->
            riff.latitude == marker.position.latitude && riff.longitude == marker.position.longitude
        } ?: Riff()
    }
}