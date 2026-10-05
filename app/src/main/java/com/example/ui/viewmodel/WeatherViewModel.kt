package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FavoritePlaceEntity
import com.example.data.local.TravelHistoryEntity
import com.example.data.local.WeatherAlertEntity
import com.example.data.model.CachedForecast
import com.example.data.model.GeocodingLocation
import com.example.data.model.WeatherCodeMapper
import com.example.data.model.WeatherResponse
import com.example.data.remote.AiInsights
import com.example.data.remote.GeminiService
import com.example.data.repository.AppSettings
import com.example.data.repository.LocationTracker
import com.example.data.repository.TrackedLocation
import com.example.data.repository.UserPreferencesRepository
import com.example.data.repository.WeatherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

sealed interface WeatherUiState {
    object Idle : WeatherUiState
    object Loading : WeatherUiState

    /**
     * @param isRefreshing a newer forecast is being loaded; the old one stays on screen meanwhile.
     * @param refreshError set when a background refresh failed but [data] is still shown.
     * @param isFromCache [data] was read from disk and has not been confirmed online yet.
     */
    data class Success(
        val data: WeatherResponse,
        val lastUpdated: Long = System.currentTimeMillis(),
        val isRefreshing: Boolean = false,
        val refreshError: String? = null,
        val isFromCache: Boolean = false
    ) : WeatherUiState

    data class Error(val message: String, val isOffline: Boolean = false) : WeatherUiState
}

/** Where the displayed forecast comes from. */
enum class LocationMode { GPS, SELECTED, DEFAULT }

/** One-off message for a snackbar, optionally with an action (e.g. "Hoàn tác"). */
data class UiEvent(
    val message: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

class WeatherViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesRepository = UserPreferencesRepository(application)
    private val weatherRepository = WeatherRepository(application, preferencesRepository)
    val locationTracker = LocationTracker(application)

    // Settings
    val settings: StateFlow<AppSettings> = preferencesRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    /** null while DataStore is still loading, so the welcome screen never flashes for returning users. */
    val onboardingDone: StateFlow<Boolean?> = preferencesRepository.onboardingDoneFlow
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Weather state
    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Idle)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _activeLocation = MutableStateFlow(DEFAULT_LOCATION)
    val activeLocation: StateFlow<TrackedLocation> = _activeLocation.asStateFlow()

    private val _locationMode = MutableStateFlow(LocationMode.DEFAULT)
    val locationMode: StateFlow<LocationMode> = _locationMode.asStateFlow()

    private val _hasLocationPermission = MutableStateFlow(locationTracker.hasLocationPermission())
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()

    val distanceSinceLastFetchKm: StateFlow<Double> = locationTracker.distanceSinceLastFetchKm

    private val _isMovementTrackingActive = MutableStateFlow(true)
    val isMovementTrackingActive: StateFlow<Boolean> = _isMovementTrackingActive.asStateFlow()

    // Day detail selected index (0..6)
    private val _selectedDayIndex = MutableStateFlow<Int?>(null)
    val selectedDayIndex: StateFlow<Int?> = _selectedDayIndex.asStateFlow()

    // Geocoding search
    private val _searchResults = MutableStateFlow<List<GeocodingLocation>>(emptyList())
    val searchResults: StateFlow<List<GeocodingLocation>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    /** The query that produced [searchResults]; lets the UI say "no match for X". */
    private val _searchedQuery = MutableStateFlow("")
    val searchedQuery: StateFlow<String> = _searchedQuery.asStateFlow()

    // AI / smart suggestions
    private val _aiInsights = MutableStateFlow<AiInsights?>(null)
    val aiInsights: StateFlow<AiInsights?> = _aiInsights.asStateFlow()

    private val _isLoadingAi = MutableStateFlow(false)
    val isLoadingAi: StateFlow<Boolean> = _isLoadingAi.asStateFlow()

    // Room DB streams
    val favoritePlaces: StateFlow<List<FavoritePlaceEntity>> = weatherRepository.favoritePlaces.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val travelHistory: StateFlow<List<TravelHistoryEntity>> = weatherRepository.travelHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val weatherAlerts: StateFlow<List<WeatherAlertEntity>> = weatherRepository.weatherAlerts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private var lastFetchTime = 0L
    private var fetchJob: Job? = null
    private var searchJob: Job? = null
    private var periodicTimeCheckJob: Job? = null

    init {
        start()
        startPeriodicCheck()
    }

    // ---------------------------------------------------------------- start-up

    private fun start() {
        viewModelScope.launch {
            // 1. Show the last known forecast immediately (also works offline).
            val cached = weatherRepository.loadCache()
            if (cached != null) {
                _activeLocation.value = TrackedLocation(cached.latitude, cached.longitude, cached.locationName)
                _locationMode.value = LocationMode.SELECTED
                _uiState.value = WeatherUiState.Success(
                    data = cached.data,
                    lastUpdated = cached.savedAt,
                    isRefreshing = true,
                    isFromCache = true
                )
            } else {
                _uiState.value = WeatherUiState.Loading
            }

            // 2. Resolve the real position; fall back to the cached / default place without a fix.
            val gps = locationTracker.getInitialLocation()
            if (gps != null) {
                useGps(gps)
                startGpsTracking()
            } else {
                if (cached == null) _locationMode.value = LocationMode.DEFAULT
                fetchWeather(_activeLocation.value, keepStale = cached != null)
            }
        }
    }

    private fun useGps(loc: TrackedLocation) {
        _activeLocation.value = loc
        _locationMode.value = LocationMode.GPS
        fetchWeather(loc, keepStale = _uiState.value is WeatherUiState.Success)
    }

    /** Call from the Activity whenever the permission state may have changed (result callback, onResume). */
    fun refreshPermissionState() {
        val granted = locationTracker.hasLocationPermission()
        val wasGranted = _hasLocationPermission.value
        _hasLocationPermission.value = granted
        if (granted && !wasGranted) onLocationPermissionGranted()
    }

    private fun onLocationPermissionGranted() {
        startGpsTracking()
        switchToGpsLocation()
    }

    /** Refresh stale data when the app returns to the foreground. */
    fun onAppResumed() {
        val state = _uiState.value
        if (state is WeatherUiState.Success && !state.isRefreshing &&
            System.currentTimeMillis() - state.lastUpdated > STALE_AFTER_MS
        ) {
            refreshCurrentWeather()
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch { preferencesRepository.setOnboardingDone() }
    }

    // ---------------------------------------------------------------- movement tracking

    fun startGpsTracking() {
        if (!locationTracker.hasLocationPermission()) return
        _isMovementTrackingActive.value = true
        locationTracker.startLocationUpdates { newLoc, distKm ->
            checkMovementThresholdAndRefresh(newLoc, distKm)
        }
    }

    fun stopGpsTracking() {
        _isMovementTrackingActive.value = false
        locationTracker.stopLocationUpdates()
    }

    fun toggleMovementTracking() {
        if (_isMovementTrackingActive.value) stopGpsTracking() else startGpsTracking()
    }

    private fun checkMovementThresholdAndRefresh(newLoc: TrackedLocation, distKm: Double) {
        viewModelScope.launch {
            val currentSettings = settings.value
            if (!currentSettings.autoRefreshOnMove || !_isMovementTrackingActive.value) return@launch
            // Only the GPS view follows the device; a chosen city must not be overwritten.
            if (_locationMode.value != LocationMode.GPS) return@launch

            val minutesSinceLast = (System.currentTimeMillis() - lastFetchTime) / (60 * 1000)
            val movedEnough = distKm >= currentSettings.minDistanceKm
            val timePassedEnough = minutesSinceLast >= currentSettings.minTimeMinutes

            if (movedEnough || (distKm > 0.5 && timePassedEnough)) {
                Log.d(TAG, "Auto-refresh: moved $distKm km, $minutesSinceLast min since last fetch")
                _activeLocation.value = newLoc
                fetchWeather(newLoc, recordHistory = true, distanceMoved = distKm, keepStale = true)
            }
        }
    }

    /** Debug helper used by the "simulate +5.5 km" button (hidden in release builds). */
    fun simulateMovementTest(deltaKm: Double) {
        viewModelScope.launch {
            locationTracker.simulateMovement(deltaKm) { newLoc, distKm ->
                if (_locationMode.value == LocationMode.GPS) _activeLocation.value = newLoc
                checkMovementThresholdAndRefresh(newLoc, distKm)
            }
        }
    }

    // ---------------------------------------------------------------- weather

    /**
     * @param keepStale keep showing the previous forecast (with a refresh indicator) instead of a
     * full-screen spinner. Only valid when the location did not change.
     */
    fun fetchWeather(
        location: TrackedLocation,
        recordHistory: Boolean = false,
        distanceMoved: Double = 0.0,
        keepStale: Boolean = false
    ) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            val previous = _uiState.value
            _uiState.value = if (keepStale && previous is WeatherUiState.Success) {
                previous.copy(isRefreshing = true, refreshError = null)
            } else {
                WeatherUiState.Loading
            }

            val result = weatherRepository.getForecast(location.latitude, location.longitude)
            ensureActive() // a newer request superseded this one

            result.onSuccess { response ->
                val now = System.currentTimeMillis()
                _uiState.value = WeatherUiState.Success(response, now)
                lastFetchTime = now
                locationTracker.recordWeatherFetched(location)
                weatherRepository.saveCache(
                    CachedForecast(location.displayName, location.latitude, location.longitude, now, response)
                )

                weatherRepository.checkAlertsAndNotify(response, location.displayName)

                if (recordHistory && response.current != null) {
                    weatherRepository.saveTravelHistory(
                        locationName = location.displayName,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        temperature = response.current.temperature2m,
                        weatherCode = response.current.weatherCode,
                        distanceMovedKm = distanceMoved
                    )
                }
            }.onFailure { err ->
                val message = friendlyMessage(err)
                _uiState.value = if (keepStale && previous is WeatherUiState.Success) {
                    previous.copy(isRefreshing = false, refreshError = message)
                } else {
                    WeatherUiState.Error(message, isOffline = err is IOException)
                }
            }
        }
    }

    fun refreshCurrentWeather() {
        fetchWeather(_activeLocation.value, keepStale = _uiState.value is WeatherUiState.Success)
    }

    fun dismissRefreshError() {
        val state = _uiState.value
        if (state is WeatherUiState.Success) _uiState.value = state.copy(refreshError = null)
    }

    fun selectDayDetail(index: Int?) {
        _selectedDayIndex.value = index
    }

    fun switchToGpsLocation() {
        viewModelScope.launch {
            val loc = locationTracker.getInitialLocation()
            if (loc != null) {
                useGpsFromUser(loc)
            } else if (!locationTracker.hasLocationPermission()) {
                _events.tryEmit(UiEvent("Hãy cấp quyền vị trí để xem thời tiết nơi bạn đang đứng"))
            } else {
                _events.tryEmit(UiEvent("Chưa xác định được vị trí. Hãy bật GPS và thử lại"))
            }
        }
    }

    private fun useGpsFromUser(loc: TrackedLocation) {
        _activeLocation.value = loc
        _locationMode.value = LocationMode.GPS
        fetchWeather(loc)
    }

    fun selectFavoriteLocation(favorite: FavoritePlaceEntity) {
        selectLocation(TrackedLocation(favorite.latitude, favorite.longitude, favorite.name))
    }

    fun selectSearchResult(location: GeocodingLocation) {
        selectLocation(TrackedLocation(location.latitude, location.longitude, location.fullDisplayName))
    }

    private fun selectLocation(loc: TrackedLocation) {
        _activeLocation.value = loc
        _locationMode.value = LocationMode.SELECTED
        fetchWeather(loc)
    }

    // ---------------------------------------------------------------- search & favorites

    fun searchLocations(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _searchError.value = null
            _searchedQuery.value = ""
            _isSearching.value = false
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            _isSearching.value = true
            weatherRepository.searchLocations(query)
                .onSuccess {
                    _searchResults.value = it
                    _searchError.value = null
                    _searchedQuery.value = query.trim()
                }
                .onFailure {
                    _searchResults.value = emptyList()
                    _searchError.value = friendlyMessage(it)
                }
            _isSearching.value = false
        }
    }

    fun clearSearch() = searchLocations("")

    fun addFavorite(loc: GeocodingLocation, category: String = "CUSTOM") {
        viewModelScope.launch {
            val added = weatherRepository.addFavoritePlace(
                name = loc.name,
                address = loc.fullDisplayName,
                lat = loc.latitude,
                lng = loc.longitude,
                category = category
            )
            _events.tryEmit(
                UiEvent(if (added) "Đã lưu “${loc.name}”" else "“${loc.name}” đã có trong danh sách")
            )
            clearSearch()
        }
    }

    fun removeFavorite(favorite: FavoritePlaceEntity) {
        viewModelScope.launch {
            weatherRepository.removeFavoritePlace(favorite)
            _events.tryEmit(
                UiEvent("Đã xóa “${favorite.name}”", "Hoàn tác") {
                    viewModelScope.launch { weatherRepository.restoreFavoritePlace(favorite) }
                }
            )
        }
    }

    // ---------------------------------------------------------------- alerts & history

    fun toggleAlertEnabled(alert: WeatherAlertEntity, enabled: Boolean) {
        viewModelScope.launch { weatherRepository.updateWeatherAlert(alert.copy(isEnabled = enabled)) }
    }

    fun addNewAlert(name: String, type: String, threshold: Double) {
        viewModelScope.launch {
            weatherRepository.addWeatherAlert(
                WeatherAlertEntity(name = name, type = type, threshold = threshold, isEnabled = true)
            )
        }
    }

    fun deleteAlert(alert: WeatherAlertEntity) {
        viewModelScope.launch {
            weatherRepository.deleteWeatherAlert(alert)
            _events.tryEmit(
                UiEvent("Đã xóa cảnh báo “${alert.name}”", "Hoàn tác") {
                    viewModelScope.launch { weatherRepository.addWeatherAlert(alert) }
                }
            )
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            weatherRepository.clearTravelHistory()
            _events.tryEmit(UiEvent("Đã xóa nhật ký di chuyển"))
        }
    }

    fun testAlertNotification(title: String, message: String) {
        weatherRepository.testSendNotification(title, message)
    }

    // ---------------------------------------------------------------- suggestions

    fun fetchAiInsights() {
        val state = _uiState.value
        if (state !is WeatherUiState.Success) return
        val current = state.data.current ?: return
        val location = _activeLocation.value
        val weatherInfo = WeatherCodeMapper.getInfo(current.weatherCode, current.isDay == 1)

        viewModelScope.launch {
            _isLoadingAi.value = true
            _aiInsights.value = null
            _aiInsights.value = GeminiService.getPlaceRecommendationsForWeather(
                locationName = location.displayName,
                latitude = location.latitude,
                longitude = location.longitude,
                temperature = current.temperature2m,
                weatherDescription = weatherInfo.title,
                isRaining = weatherInfo.isRaining
            )
            _isLoadingAi.value = false
        }
    }

    fun clearAiInsights() {
        _aiInsights.value = null
    }

    // ---------------------------------------------------------------- settings

    fun setTempUnit(unit: String) = viewModelScope.launch { preferencesRepository.setTempUnit(unit) }
    fun setWindUnit(unit: String) = viewModelScope.launch { preferencesRepository.setWindUnit(unit) }
    fun setAutoRefresh(enabled: Boolean) = viewModelScope.launch { preferencesRepository.setAutoRefresh(enabled) }
    fun setMinDistance(km: Double) = viewModelScope.launch { preferencesRepository.setMinDistance(km) }
    fun setMinTime(minutes: Int) = viewModelScope.launch { preferencesRepository.setMinTime(minutes) }
    fun setNotifications(enabled: Boolean) = viewModelScope.launch { preferencesRepository.setNotifications(enabled) }

    /** Lets the Activity (permission results etc.) surface a snackbar through the same channel. */
    fun postEvent(event: UiEvent) {
        _events.tryEmit(event)
    }

    // ---------------------------------------------------------------- internals

    private fun startPeriodicCheck() {
        periodicTimeCheckJob = viewModelScope.launch {
            while (true) {
                delay(60_000L)
                val currentSettings = settings.value
                if (currentSettings.autoRefreshOnMove &&
                    _isMovementTrackingActive.value &&
                    _locationMode.value == LocationMode.GPS
                ) {
                    val minutesSinceLast = (System.currentTimeMillis() - lastFetchTime) / (60 * 1000)
                    if (minutesSinceLast >= currentSettings.minTimeMinutes) refreshCurrentWeather()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        periodicTimeCheckJob?.cancel()
        locationTracker.release()
    }

    companion object {
        private const val TAG = "WeatherVM"
        private const val SEARCH_DEBOUNCE_MS = 350L
        private const val STALE_AFTER_MS = 15 * 60 * 1000L
        val DEFAULT_LOCATION = TrackedLocation(21.0285, 105.8542, "Hà Nội, Việt Nam")

        fun friendlyMessage(error: Throwable): String = when (error) {
            is IOException -> "Không có kết nối mạng. Hãy kiểm tra Wi‑Fi hoặc dữ liệu di động rồi thử lại."
            is HttpException -> "Máy chủ thời tiết đang bận (mã ${error.code()}). Vui lòng thử lại sau ít phút."
            else -> "Đã có lỗi xảy ra khi tải dữ liệu. Vui lòng thử lại."
        }
    }
}
