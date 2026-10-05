package com.example.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    // Reverse geocoding is a blocking network call, so it never runs on the main thread.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Location used for the most recent weather fetch. */
    var lastFetchedLocation: TrackedLocation? = null
        private set

    private val _currentLocation = MutableStateFlow<TrackedLocation?>(null)
    val currentLocation: StateFlow<TrackedLocation?> = _currentLocation.asStateFlow()

    private val _distanceSinceLastFetchKm = MutableStateFlow(0.0)
    val distanceSinceLastFetchKm: StateFlow<Double> = _distanceSinceLastFetchKm.asStateFlow()

    private var isTracking = false
    private var locationCallback: LocationCallback? = null
    private var lastNamed: TrackedLocation? = null

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** Returns the device location, or null when permission is missing or no fix is available. */
    @SuppressLint("MissingPermission")
    suspend fun getInitialLocation(): TrackedLocation? {
        if (!hasLocationPermission()) return null
        return try {
            val loc = awaitLocation { fusedLocationClient.lastLocation }
                ?: awaitLocation {
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                }
                ?: return null
            toTracked(loc).also { _currentLocation.value = it }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting initial location", e)
            null
        }
    }

    private suspend fun awaitLocation(request: () -> Task<Location?>): Location? =
        suspendCancellableCoroutine { cont ->
            request()
                .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
                .addOnFailureListener { if (cont.isActive) cont.resume(null) }
        }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(onLocationChanged: (TrackedLocation, Double) -> Unit) {
        if (isTracking || !hasLocationPermission()) return

        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 15_000L)
            .setMinUpdateIntervalMillis(10_000L)
            .setMinUpdateDistanceMeters(50f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                scope.launch {
                    val tracked = toTracked(loc)
                    _currentLocation.value = tracked
                    val distanceKm = calculateDistanceFromLastFetch(loc.latitude, loc.longitude)
                    _distanceSinceLastFetchKm.value = distanceKm
                    onLocationChanged(tracked, distanceKm)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
            locationCallback = callback
            isTracking = true
        } catch (e: Exception) {
            Log.e(TAG, "Cannot request location updates", e)
        }
    }

    fun stopLocationUpdates() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
        isTracking = false
    }

    fun release() {
        stopLocationUpdates()
        scope.cancel()
    }

    fun recordWeatherFetched(location: TrackedLocation) {
        lastFetchedLocation = location
        _distanceSinceLastFetchKm.value = 0.0
    }

    fun calculateDistanceFromLastFetch(latitude: Double, longitude: Double): Double {
        val last = lastFetchedLocation ?: return 0.0
        return distanceKm(last.latitude, last.longitude, latitude, longitude)
    }

    /** Debug helper: pretend the device moved [deltaKm] to the north-east. */
    suspend fun simulateMovement(deltaKm: Double, onMoved: (TrackedLocation, Double) -> Unit) {
        val current = _currentLocation.value ?: TrackedLocation(21.0285, 105.8542, "Hà Nội")
        val delta = deltaKm / 111.0 // ~111 km per degree
        val newLat = current.latitude + delta
        val newLng = current.longitude + delta
        val tracked = TrackedLocation(newLat, newLng, placeName(newLat, newLng))
        _currentLocation.value = tracked
        val dist = calculateDistanceFromLastFetch(newLat, newLng)
        _distanceSinceLastFetchKm.value = dist
        onMoved(tracked, dist)
    }

    private suspend fun toTracked(loc: Location): TrackedLocation =
        TrackedLocation(loc.latitude, loc.longitude, placeName(loc.latitude, loc.longitude))

    /** Reuses the previous name while the device stays within ~300 m to avoid needless geocoder calls. */
    private suspend fun placeName(lat: Double, lng: Double): String {
        lastNamed?.let {
            if (distanceKm(it.latitude, it.longitude, lat, lng) < 0.3) return it.displayName
        }
        val name = withContext(Dispatchers.IO) { reverseGeocode(lat, lng) }
        lastNamed = TrackedLocation(lat, lng, name)
        return name
    }

    private fun reverseGeocode(lat: Double, lng: Double): String {
        val fallback = "Vị trí hiện tại (%.2f, %.2f)".format(Locale.US, lat, lng)
        if (!Geocoder.isPresent()) return fallback
        return try {
            @Suppress("DEPRECATION")
            val addresses = Geocoder(context, Locale.forLanguageTag("vi-VN")).getFromLocation(lat, lng, 1)
            formatAddress(addresses?.firstOrNull()) ?: fallback
        } catch (e: Exception) {
            fallback
        }
    }

    private fun formatAddress(address: Address?): String? {
        address ?: return null
        val locality = address.subAdminArea ?: address.locality ?: address.subLocality
        val admin = address.adminArea
        return when {
            locality != null && admin != null && locality != admin -> "$locality, $admin"
            locality != null -> locality
            admin != null -> admin
            else -> address.featureName
        }
    }

    companion object {
        private const val TAG = "LocationTracker"

        fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
            val results = FloatArray(1)
            Location.distanceBetween(lat1, lng1, lat2, lng2, results)
            return results[0] / 1000.0
        }
    }
}
