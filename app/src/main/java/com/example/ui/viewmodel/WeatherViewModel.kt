package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FavoritePlaceEntity
import com.example.data.local.TravelHistoryEntity
import com.example.data.local.WeatherAlertEntity
import com.example.data.model.GeocodingLocation
import com.example.data.model.WeatherCodeMapper
import com.example.data.model.WeatherResponse
import com.example.data.remote.GeminiService
import com.example.data.repository.AppSettings
import com.example.data.repository.LocationTracker
import com.example.data.repository.TrackedLocation
import com.example.data.repository.UserPreferencesRepository
import com.example.data.repository.WeatherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface WeatherUiState {
    object Idle : WeatherUiState
    object Loading : WeatherUiState
    data class Success(val data: WeatherResponse, val lastUpdated: Long = System.currentTimeMillis()) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

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

    // Weather state
    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Idle)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    // Active location being displayed
    private val _activeLocation = MutableStateFlow(
        TrackedLocation(21.0285, 105.8542, "Hà Nội, Việt Nam")
    )
    val activeLocation: StateFlow<TrackedLocation> = _activeLocation.asStateFlow()

    private val _isShowingGpsLocation = MutableStateFlow(true)
    val isShowingGpsLocation: StateFlow<Boolean> = _isShowingGpsLocation.asStateFlow()

    // Distance accumulated since last weather fetch
    val distanceSinceLastFetchKm: StateFlow<Double> = locationTracker.distanceSinceLastFetchKm

    private val _isMovementTrackingActive = MutableStateFlow(true)
    val isMovementTrackingActive: StateFlow<Boolean> = _isMovementTrackingActive.asStateFlow()

    // Day detail selected index (0..6)
    private val _selectedDayIndex = MutableStateFlow<Int?>(null)
    val selectedDayIndex: StateFlow<Int?> = _selectedDayIndex.asStateFlow()

    // Geocoding search results
    private val _searchResults = MutableStateFlow<List<GeocodingLocation>>(emptyList())
    val searchResults: StateFlow<List<GeocodingLocation>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // AI Weather & Google Maps recommendations
    private val _aiInsights = MutableStateFlow<String?>(null)
    val aiInsights: StateFlow<String?> = _aiInsights.asStateFlow()

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

    private var lastFetchTime = 0L
    private var periodicTimeCheckJob: Job? = null

    init {
        initializeLocationAndWeather()
        startPeriodicCheck()
    }

    private fun initializeLocationAndWeather() {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            val initialLoc = locationTracker.getInitialLocation()
            val target = initialLoc ?: TrackedLocation(21.0285, 105.8542, "Hà Nội, Việt Nam")
            _activeLocation.value = target
            _isShowingGpsLocation.value = true
            fetchWeather(target, recordHistory = false)
            startGpsTracking()
        }
    }

    fun startGpsTracking() {
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
        if (_isMovementTrackingActive.value) {
            stopGpsTracking()
        } else {
            startGpsTracking()
        }
    }

    private fun checkMovementThresholdAndRefresh(newLoc: TrackedLocation, distKm: Double) {
        viewModelScope.launch {
            val currentSettings = settings.value
            if (!currentSettings.autoRefreshOnMove || !_isMovementTrackingActive.value) return@launch

            val now = System.currentTimeMillis()
            val minutesSinceLast = (now - lastFetchTime) / (60 * 1000)

            val movedEnough = distKm >= currentSettings.minDistanceKm
            val timePassedEnough = minutesSinceLast >= currentSettings.minTimeMinutes

            if (movedEnough || (distKm > 0.5 && timePassedEnough)) {
                Log.d("WeatherVM", "Auto-refresh triggered! Moved $distKm km, $minutesSinceLast mins passed")
                if (_isShowingGpsLocation.value) {
                    _activeLocation.value = newLoc
                }
                fetchWeather(newLoc, recordHistory = true, distanceMoved = distKm)
            }
        }
    }

    fun simulateMovementTest(deltaKm: Double) {
        viewModelScope.launch {
            locationTracker.simulateMovement(deltaKm) { newLoc, distKm ->
                if (_isShowingGpsLocation.value) {
                    _activeLocation.value = newLoc
                }
                checkMovementThresholdAndRefresh(newLoc, distKm)
            }
        }
    }

    fun fetchWeather(
        location: TrackedLocation,
        recordHistory: Boolean = false,
        distanceMoved: Double = 0.0
    ) {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            val result = weatherRepository.getForecast(location.latitude, location.longitude)
            result.onSuccess { response ->
                _uiState.value = WeatherUiState.Success(response, System.currentTimeMillis())
                lastFetchTime = System.currentTimeMillis()
                locationTracker.recordWeatherFetched(location)

                // Check personal alerts
                weatherRepository.checkAlertsAndNotify(response, location.displayName)

                // Save to travel history if user moved or requested
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
                _uiState.value = WeatherUiState.Error(
                    err.localizedMessage ?: "Không thể kết nối đến máy chủ thời tiết. Vui lòng kiểm tra mạng."
                )
            }
        }
    }

    fun refreshCurrentWeather() {
        val current = _activeLocation.value
        fetchWeather(current, recordHistory = false)
    }

    fun selectDayDetail(index: Int?) {
        _selectedDayIndex.value = index
    }

    fun switchToGpsLocation() {
        viewModelScope.launch {
            val loc = locationTracker.currentLocation.value ?: locationTracker.getInitialLocation()
            if (loc != null) {
                _activeLocation.value = loc
                _isShowingGpsLocation.value = true
                fetchWeather(loc, recordHistory = false)
            }
        }
    }

    fun selectFavoriteLocation(favorite: FavoritePlaceEntity) {
        val loc = TrackedLocation(
            latitude = favorite.latitude,
            longitude = favorite.longitude,
            displayName = favorite.name
        )
        _activeLocation.value = loc
        _isShowingGpsLocation.value = false
        fetchWeather(loc, recordHistory = false)
    }

    fun searchLocations(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _searchResults.value = emptyList()
                return@launch
            }
            _isSearching.value = true
            val results = weatherRepository.searchLocations(query)
            _searchResults.value = results
            _isSearching.value = false
        }
    }

    fun addFavorite(loc: GeocodingLocation, category: String = "CUSTOM") {
        viewModelScope.launch {
            weatherRepository.addFavoritePlace(
                name = loc.name,
                address = loc.fullDisplayName,
                lat = loc.latitude,
                lng = loc.longitude,
                category = category
            )
            _searchResults.value = emptyList()
        }
    }

    fun removeFavorite(favorite: FavoritePlaceEntity) {
        viewModelScope.launch {
            weatherRepository.removeFavoritePlace(favorite)
        }
    }

    fun toggleAlertEnabled(alert: WeatherAlertEntity, enabled: Boolean) {
        viewModelScope.launch {
            weatherRepository.updateWeatherAlert(alert.copy(isEnabled = enabled))
        }
    }

    fun addNewAlert(name: String, type: String, threshold: Double) {
        viewModelScope.launch {
            weatherRepository.addWeatherAlert(
                WeatherAlertEntity(
                    name = name,
                    type = type,
                    threshold = threshold,
                    isEnabled = true
                )
            )
        }
    }

    fun deleteAlert(alert: WeatherAlertEntity) {
        viewModelScope.launch {
            weatherRepository.deleteWeatherAlert(alert)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            weatherRepository.clearTravelHistory()
        }
    }

    fun testAlertNotification(title: String, message: String) {
        weatherRepository.testSendNotification(title, message)
    }

    // AI Weather Insights with Maps Grounding
    fun fetchAiInsights() {
        val state = _uiState.value
        if (state !is WeatherUiState.Success) return
        val current = state.data.current ?: return
        val location = _activeLocation.value
        val weatherInfo = WeatherCodeMapper.getInfo(current.weatherCode, current.isDay == 1)

        viewModelScope.launch {
            _isLoadingAi.value = true
            _aiInsights.value = null
            val result = GeminiService.getPlaceRecommendationsForWeather(
                locationName = location.displayName,
                latitude = location.latitude,
                longitude = location.longitude,
                temperature = current.temperature2m,
                weatherDescription = weatherInfo.title,
                isRaining = weatherInfo.isRaining
            )
            _aiInsights.value = result
            _isLoadingAi.value = false
        }
    }

    fun clearAiInsights() {
        _aiInsights.value = null
    }

    // Settings actions
    fun setTempUnit(unit: String) = viewModelScope.launch { preferencesRepository.setTempUnit(unit) }
    fun setWindUnit(unit: String) = viewModelScope.launch { preferencesRepository.setWindUnit(unit) }
    fun setAutoRefresh(enabled: Boolean) = viewModelScope.launch { preferencesRepository.setAutoRefresh(enabled) }
    fun setMinDistance(km: Double) = viewModelScope.launch { preferencesRepository.setMinDistance(km) }
    fun setMinTime(minutes: Int) = viewModelScope.launch { preferencesRepository.setMinTime(minutes) }
    fun setNotifications(enabled: Boolean) = viewModelScope.launch { preferencesRepository.setNotifications(enabled) }

    private fun startPeriodicCheck() {
        periodicTimeCheckJob = viewModelScope.launch {
            while (true) {
                delay(60_000L) // every 1 min
                val currentSettings = settings.value
                if (currentSettings.autoRefreshOnMove && _isMovementTrackingActive.value && _isShowingGpsLocation.value) {
                    val now = System.currentTimeMillis()
                    val minutesSinceLast = (now - lastFetchTime) / (60 * 1000)
                    if (minutesSinceLast >= currentSettings.minTimeMinutes) {
                        refreshCurrentWeather()
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        periodicTimeCheckJob?.cancel()
        locationTracker.stopLocationUpdates()
    }
}
