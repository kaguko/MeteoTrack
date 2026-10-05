package com.example.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class TrackedLocation(
    val latitude: Double,
    val longitude: Double,
    val displayName: String,
    val timeMillis: Long = System.currentTimeMillis()
)

class LocationTracker(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    // Last location when weather API was fetched
    var lastFetchedLocation: TrackedLocation? = null
        private set

    // Current real-time location
    private val _currentLocation = MutableStateFlow<TrackedLocation?>(null)
    val currentLocation: StateFlow<TrackedLocation?> = _currentLocation.asStateFlow()

    // Distance accumulated since last weather fetch
    private val _distanceSinceLastFetchKm = MutableStateFlow(0.0)
    val distanceSinceLastFetchKm: StateFlow<Double> = _distanceSinceLastFetchKm.asStateFlow()

    private var isTracking = false
    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    suspend fun getInitialLocation(): TrackedLocation? = withContext(Dispatchers.IO) {
        try {
            val lastLoc = suspendCancellableCoroutine<Location?> { cont ->
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { loc ->
                        if (cont.isActive) cont.resume(loc)
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
            }

            if (lastLoc != null) {
                val name = getPlaceNameFromCoordinates(lastLoc.latitude, lastLoc.longitude)
                val tracked = TrackedLocation(lastLoc.latitude, lastLoc.longitude, name)
                _currentLocation.value = tracked
                return@withContext tracked
            }

            // Fallback request current location
            val currentLoc = suspendCancellableCoroutine<Location?> { cont ->
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .addOnSuccessListener { loc ->
                        if (cont.isActive) cont.resume(loc)
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
            }

            if (currentLoc != null) {
                val name = getPlaceNameFromCoordinates(currentLoc.latitude, currentLoc.longitude)
                val tracked = TrackedLocation(currentLoc.latitude, currentLoc.longitude, name)
                _currentLocation.value = tracked
                return@withContext tracked
            }
        } catch (e: Exception) {
            Log.e("LocationTracker", "Error getting initial location", e)
        }

        // Default default location in Hanoi, Vietnam if location is unavailable initially
        val defaultHanoi = TrackedLocation(21.0285, 105.8542, "Hà Nội, Việt Nam")
        _currentLocation.value = defaultHanoi
        defaultHanoi
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(onLocationChanged: (TrackedLocation, Double) -> Unit) {
        if (isTracking) return
        isTracking = true

        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 15_000L)
            .setMinUpdateIntervalMillis(10_000L)
            .setMinUpdateDistanceMeters(50f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val name = getPlaceNameFromCoordinates(loc.latitude, loc.longitude)
                val newTracked = TrackedLocation(loc.latitude, loc.longitude, name)
                _currentLocation.value = newTracked

                val distanceKm = calculateDistanceFromLastFetch(loc.latitude, loc.longitude)
                _distanceSinceLastFetchKm.value = distanceKm

                onLocationChanged(newTracked, distanceKm)
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            Log.e("LocationTracker", "Cannot request location updates", e)
        }
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        isTracking = false
    }

    fun recordWeatherFetched(location: TrackedLocation) {
        lastFetchedLocation = location
        _distanceSinceLastFetchKm.value = 0.0
    }

    fun calculateDistanceFromLastFetch(latitude: Double, longitude: Double): Double {
        val last = lastFetchedLocation ?: return 0.0
        val results = FloatArray(1)
        Location.distanceBetween(
            last.latitude,
            last.longitude,
            latitude,
            longitude,
            results
        )
        return results[0] / 1000.0 // meters to km
    }

    // Helper for manual or simulated movement (very helpful for testing auto-refresh when >5km!)
    fun simulateMovement(deltaKm: Double, onMoved: (TrackedLocation, Double) -> Unit) {
        val current = _currentLocation.value ?: TrackedLocation(21.0285, 105.8542, "Hà Nội")
        // Roughly 1 deg lat is ~111km
        val deltaLat = deltaKm / 111.0
        val newLat = current.latitude + deltaLat
        val newLng = current.longitude + (deltaKm / 111.0)
        val newName = getPlaceNameFromCoordinates(newLat, newLng)
        val newTracked = TrackedLocation(newLat, newLng, newName)
        _currentLocation.value = newTracked

        val dist = calculateDistanceFromLastFetch(newLat, newLng)
        _distanceSinceLastFetchKm.value = dist
        onMoved(newTracked, dist)
    }

    fun getPlaceNameFromCoordinates(lat: Double, lng: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale("vi", "VN"))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                var resultName: String? = null
                // Tiramisu async geocoding fallback
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                formatAddress(addresses?.firstOrNull(), lat, lng)
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                formatAddress(addresses?.firstOrNull(), lat, lng)
            }
        } catch (e: Exception) {
            "Tọa độ: %.3f, %.3f".format(Locale.US, lat, lng)
        }
    }

    private fun formatAddress(address: Address?, lat: Double, lng: Double): String {
        if (address == null) return "Tọa độ: %.3f, %.3f".format(Locale.US, lat, lng)
        val locality = address.subAdminArea ?: address.locality ?: address.subLocality
        val admin = address.adminArea
        return when {
            locality != null && admin != null -> "$locality, $admin"
            locality != null -> locality
            admin != null -> admin
            else -> address.featureName ?: "Tọa độ: %.3f, %.3f".format(Locale.US, lat, lng)
        }
    }
}
