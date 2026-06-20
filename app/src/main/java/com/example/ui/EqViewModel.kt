package com.example.ui

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.media.audiofx.Visualizer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioEffectEngine
import com.example.data.AutoEqData
import com.example.data.AutoEqHeadphone
import com.example.data.DeviceMapping
import com.example.data.EqProfile
import com.example.data.EqRepository
import java.util.Locale
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EqViewModel(private val repository: EqRepository, private val context: Context) : ViewModel() {

    private val audioEngine = AudioEffectEngine.getInstance()

    // Dolby Atmos & Dolby Audio Settings Local Storage (Persisted with SharedPreferences)
    private val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private val _isDolbyEnabled = MutableStateFlow(prefs.getBoolean("is_dolby_enabled", false))
    val isDolbyEnabled: StateFlow<Boolean> = _isDolbyEnabled.asStateFlow()

    private val _dolbyMode = MutableStateFlow(prefs.getInt("dolby_mode", 1)) // Default: Music
    val dolbyMode: StateFlow<Int> = _dolbyMode.asStateFlow()

    private val _dolbySurroundStrength = MutableStateFlow(prefs.getFloat("dolby_surround_strength", 600f))
    val dolbySurroundStrength: StateFlow<Float> = _dolbySurroundStrength.asStateFlow()

    private val _dolbyDialogueEnhancer = MutableStateFlow(prefs.getFloat("dolby_dialogue_enhancer", 400f))
    val dolbyDialogueEnhancer: StateFlow<Float> = _dolbyDialogueEnhancer.asStateFlow()

    private val _dolbyVolumeLeveler = MutableStateFlow(prefs.getBoolean("dolby_volume_leveler", true))
    val dolbyVolumeLeveler: StateFlow<Boolean> = _dolbyVolumeLeveler.asStateFlow()

    fun setDolbyEnabled(enabled: Boolean) {
        _isDolbyEnabled.value = enabled
        prefs.edit().putBoolean("is_dolby_enabled", enabled).apply()
        applyDolbySoundStage()
    }

    fun setDolbyMode(mode: Int) {
        _dolbyMode.value = mode
        prefs.edit().putInt("dolby_mode", mode).apply()
        applyDolbySoundStage()
    }

    fun setDolbySurroundStrength(strength: Float) {
        _dolbySurroundStrength.value = strength
        prefs.edit().putFloat("dolby_surround_strength", strength).apply()
        applyDolbySoundStage()
    }

    fun setDolbyDialogueEnhancer(level: Float) {
        _dolbyDialogueEnhancer.value = level
        prefs.edit().putFloat("dolby_dialogue_enhancer", level).apply()
        applyDolbySoundStage()
    }

    fun setDolbyVolumeLeveler(enabled: Boolean) {
        _dolbyVolumeLeveler.value = enabled
        prefs.edit().putBoolean("dolby_volume_leveler", enabled).apply()
        applyDolbySoundStage()
    }

    private fun applyDolbySoundStage() {
        val enabled = _isDolbyEnabled.value
        if (enabled) {
            val mode = _dolbyMode.value
            val surround = _dolbySurroundStrength.value
            val dialogue = _dolbyDialogueEnhancer.value
            val leveler = _dolbyVolumeLeveler.value

            // Setup custom DSP configurations representing the selected Dolby environment mode
            var current = _currentProfile.value

            // Movie, Music, Game, Voice, Custom
            current = when (mode) {
                0 -> { // Movie (सिनेमा) - Wide surround stage, rich deep theater rumble
                    current.copy(
                        virtualizer = surround.coerceIn(0f..1000f),
                        reverbPreset = 4, // Medium Hall (Theater feel)
                        bassBoost = 650f, // Cine-bass boost
                        band1kHz = dialogue / 100f, // Dialog booster frequency
                        band2kHz = (dialogue / 100f) * 1.5f,
                        band4kHz = dialogue / 100f,
                        limiterEnabled = leveler
                    )
                }
                1 -> { // Music (संगीत) - Balanced, pure spatial detailing
                    current.copy(
                        virtualizer = (surround * 0.7f).coerceIn(0f..1000f),
                        reverbPreset = 1, // Small Room (Acoustic feel)
                        bassBoost = 350f,
                        limiterEnabled = leveler
                    )
                }
                2 -> { // Game (खेल) - 3D local positional cues, crisp footstep treble
                    current.copy(
                        virtualizer = (surround * 1.2f).coerceIn(0f..1000f),
                        reverbPreset = 2, // Medium Room (Spatial)
                        band4kHz = 3.0f,
                        band8kHz = 5.0f,
                        band16kHz = 6.0f,
                        bassBoost = 500f,
                        limiterEnabled = leveler
                    )
                }
                3 -> { // Voice (स्पष्ट आवाज़) - Suppressed rumble, boosted mid-frequency voice bands
                    current.copy(
                        virtualizer = 0f,
                        reverbPreset = 0, // No Hall
                        bassBoost = 100f, // Low rumble
                        band250Hz = 1.0f,
                        band500Hz = 3.0f,
                        band1kHz = (dialogue / 100f) * 2f,
                        band2kHz = (dialogue / 100f) * 2.2f,
                        band4kHz = (dialogue / 100f) * 1.8f,
                        limiterEnabled = leveler
                    )
                }
                else -> current // Custom - depends on normal profiles
            }

            // Apply Dolby virtualizer overlay and update active engine parameters
            _currentProfile.value = current
            audioEngine.updateActiveProfile(current)
        } else {
            // Dolby disabled, restore normal profile values
            audioEngine.updateActiveProfile(_currentProfile.value)
        }
    }

    // Screen Tabs
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // EQ profile structures
    private val _currentProfile = MutableStateFlow(EqProfile.FLAT)
    val currentProfile: StateFlow<EqProfile> = _currentProfile.asStateFlow()

    // Fetch profiles and mappings from database
    val profiles: StateFlow<List<EqProfile>> = repository.allProfiles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf(
                EqProfile.FLAT,
                EqProfile.BASS_BOOST,
                EqProfile.VOCAL_BOOST,
                EqProfile.TREBLE_BOOST,
                EqProfile.CLASSIC,
                EqProfile.ROCK_METAL,
                EqProfile.CINEMA_SURROUND,
                EqProfile.FPS_FOOTSTEPS,
                EqProfile.INDIAN_CLASSICAL,
                EqProfile.SUFI_GHAZAL,
                EqProfile.BOLLYWOOD_BEATS,
                EqProfile.BHAKTI_DEVOTIONAL
            )
        )

    val systemProfiles: StateFlow<List<EqProfile>> = repository.systemProfiles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val userProfiles: StateFlow<List<EqProfile>> = repository.userProfiles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val deviceMappings: StateFlow<List<DeviceMapping>> = repository.allDeviceMappings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Device setup (real Bluetooth tracker check)
    private val _connectedDeviceName = MutableStateFlow("No Device Connected")
    val connectedDeviceName: StateFlow<String> = _connectedDeviceName.asStateFlow()

    private var bluetoothA2dp: android.bluetooth.BluetoothA2dp? = null
    
    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.A2DP) {
                bluetoothA2dp = proxy as? android.bluetooth.BluetoothA2dp
                updateConnectedDevice()
            }
        }
        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.A2DP) {
                bluetoothA2dp = null
                _connectedDeviceName.value = "No Device Connected"
            }
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            if (action == BluetoothDevice.ACTION_ACL_CONNECTED ||
                action == BluetoothDevice.ACTION_ACL_DISCONNECTED ||
                "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED" == action) {
                updateConnectedDevice()
            }
        }
    }

    // AutoEq features
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _allHeadphones = MutableStateFlow(AutoEqData.headphoneList)
    val filteredHeadphones: StateFlow<List<AutoEqHeadphone>> = combine(_searchQuery, _allHeadphones) { query, list ->
        if (query.isBlank()) {
            list
        } else {
            list.filter { it.fullName.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AutoEqData.headphoneList)

    // Visualizers (waveform: 64 points, spectrum: 24 points)
    private val _waveformPoints = MutableStateFlow(FloatArray(64))
    val waveformPoints: StateFlow<FloatArray> = _waveformPoints.asStateFlow()

    private val _spectrumBars = MutableStateFlow(FloatArray(24))
    val spectrumBars: StateFlow<FloatArray> = _spectrumBars.asStateFlow()

    private val _isVisualizerActive = MutableStateFlow(false)
    val isVisualizerActive: StateFlow<Boolean> = _isVisualizerActive.asStateFlow()

    private var currentVisualizer: Visualizer? = null

    // Visualizer Styles
    private val _visualizerStyle = MutableStateFlow("Cosmic Neon") // Cosmic Neon, Cyberpunk Peak, Minimalist
    val visualizerStyle: StateFlow<String> = _visualizerStyle.asStateFlow()

    // Settings elements
    private val _bufferSize = MutableStateFlow("Medium (1024)")
    val bufferSize: StateFlow<String> = _bufferSize.asStateFlow()

    private val _legacyMode = MutableStateFlow(true)
    val legacyMode: StateFlow<Boolean> = _legacyMode.asStateFlow()

    private val _detectedSessions = audioEngine.detectedSessions
    val detectedSessions: StateFlow<List<Int>> = _detectedSessions

    val serviceStats: StateFlow<com.example.data.ServiceResourceStats> by lazy {
        _serviceStats.asStateFlow()
    }
    private val _serviceStats = MutableStateFlow(audioEngine.getResourceStats(context))

    init {
        // Poll service resource stats every 2 seconds
        viewModelScope.launch {
            while (true) {
                try {
                    _serviceStats.value = audioEngine.getResourceStats(context)
                } catch (e: Exception) {
                    android.util.Log.e("EqViewModel", "Failed to update service stats: ${e.message}")
                }
                delay(2000)
            }
        }

        // Prepopulate default profiles if database is empty or needs new ones
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val defaults = listOf(
                    EqProfile.FLAT,
                    EqProfile.BASS_BOOST,
                    EqProfile.VOCAL_BOOST,
                    EqProfile.TREBLE_BOOST,
                    EqProfile.CLASSIC,
                    EqProfile.ROCK_METAL,
                    EqProfile.CINEMA_SURROUND,
                    EqProfile.FPS_FOOTSTEPS,
                    EqProfile.INDIAN_CLASSICAL,
                    EqProfile.SUFI_GHAZAL,
                    EqProfile.BOLLYWOOD_BEATS,
                    EqProfile.BHAKTI_DEVOTIONAL
                )
                defaults.forEach { profile ->
                    val existing = repository.getProfileById(profile.id) ?: repository.getProfileByName(profile.name)
                    if (existing == null) {
                        repository.insertProfile(profile)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("EqViewModel", "Failed prepopulating default profiles", e)
            }
        }

        // Sync engine profile on initial launch
        audioEngine.setLegacyMode(true)
        audioEngine.updateActiveProfile(_currentProfile.value)

        // Observe detected sessions to hook the Visualizer
        viewModelScope.launch {
            detectedSessions.collect { sessions ->
                val targetSession = sessions.firstOrNull()
                if (targetSession != null) {
                    setupVisualizer(targetSession)
                } else {
                    releaseVisualizer()
                    _isVisualizerActive.value = false
                    _waveformPoints.value = FloatArray(64)
                    _spectrumBars.value = FloatArray(24)
                }
            }
        }

        // Register Bluetooth monitors
        try {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")
            }
            context.registerReceiver(bluetoothReceiver, filter)
            
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = manager?.adapter
            adapter?.getProfileProxy(context, profileListener, BluetoothProfile.A2DP)
        } catch (e: Exception) {
            android.util.Log.e("EqViewModel", "Bluetooth initialization failed: ${e.message}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            context.unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {
            // Ignored
        }
        try {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = manager?.adapter
            adapter?.closeProfileProxy(BluetoothProfile.A2DP, bluetoothA2dp)
        } catch (e: Exception) {
            // Ignored
        }
        releaseVisualizer()
    }

    @Synchronized
    private fun setupVisualizer(sessionId: Int) {
        try {
            releaseVisualizer()
            
            if (context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                _isVisualizerActive.value = false
                return
            }

            val visualizer = Visualizer(sessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[0]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                        if (waveform != null && waveform.isNotEmpty()) {
                            val points = FloatArray(64)
                            val step = (waveform.size / 64).coerceAtLeast(1)
                            for (i in 0 until 64) {
                                val index = (i * step).coerceAtMost(waveform.size - 1)
                                points[i] = ((waveform[index].toInt() and 0xFF) - 128) / 128f
                            }
                            _waveformPoints.value = points
                            _isVisualizerActive.value = true
                        }
                    }

                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        if (fft != null && fft.isNotEmpty()) {
                            val bars = FloatArray(24)
                            for (i in 0 until 24) {
                                val rIndex = i * 2
                                val iIndex = i * 2 + 1
                                val real = if (rIndex < fft.size) fft[rIndex].toFloat() else 0f
                                val imag = if (iIndex < fft.size) fft[iIndex].toFloat() else 0f
                                val mag = kotlin.math.sqrt(real * real + imag * imag)
                                val db = 20 * kotlin.math.log10(mag.coerceAtLeast(1f))
                                bars[i] = (db / 60f).coerceIn(0.05f, 1.0f)
                            }
                            _spectrumBars.value = bars
                            _isVisualizerActive.value = true
                        }
                    }
                }, Visualizer.getMaxCaptureRate() / 2, true, true)
                enabled = true
            }
            currentVisualizer = visualizer
            _isVisualizerActive.value = true
        } catch (e: Exception) {
            android.util.Log.e("EqViewModel", "Failed to setup Visualizer on session $sessionId: ${e.message}")
            _isVisualizerActive.value = false
        }
    }

    @Synchronized
    private fun releaseVisualizer() {
        try {
            currentVisualizer?.enabled = false
            currentVisualizer?.release()
        } catch (e: Exception) {
            // Ignored
        } finally {
            currentVisualizer = null
        }
    }

    fun updateConnectedDevice() {
        try {
            if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                _connectedDeviceName.value = "Bluetooth Permission Required"
                return
            }
            
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = manager?.adapter
            if (adapter == null || !adapter.isEnabled) {
                _connectedDeviceName.value = "Bluetooth Disabled"
                return
            }

            var devName: String? = null
            
            val devices = bluetoothA2dp?.connectedDevices
            if (!devices.isNullOrEmpty()) {
                devName = devices.firstOrNull()?.name
            }
            
            if (devName == null) {
                val bondedDevices = adapter.bondedDevices
                for (device in bondedDevices) {
                    try {
                        val isConnectedMethod = device.javaClass.getMethod("isConnected")
                        val isConnected = isConnectedMethod.invoke(device) as Boolean
                        if (isConnected) {
                            devName = device.name
                            break
                        }
                    } catch (e: Exception) {
                        // Ignore reflection fail
                    }
                }
            }
            
            val finalName = devName ?: "No Device Connected"
            _connectedDeviceName.value = finalName
            
            if (finalName != "No Device Connected" && finalName != "Bluetooth Disabled" && finalName != "Bluetooth Permission Required") {
                viewModelScope.launch {
                    val mapping = repository.getDeviceMapping(finalName)
                    if (mapping != null) {
                        val mappedProfile = repository.getProfileById(mapping.profileId)
                        if (mappedProfile != null) {
                            _currentProfile.value = mappedProfile
                            audioEngine.updateActiveProfile(mappedProfile)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("EqViewModel", "Error updating connected Bluetooth device Name", e)
            _connectedDeviceName.value = "No Device Connected"
        }
    }

    fun selectTab(tab: Int) {
        _currentTab.value = tab
    }

    private fun saveActiveProfileToDb(profile: EqProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getProfileById(profile.id) ?: repository.getProfileByName(profile.name)
            if (existing != null) {
                val updatedWithCustom = profile.copy(isCustom = existing.isCustom)
                repository.insertProfile(updatedWithCustom)
            } else {
                repository.insertProfile(profile)
            }
        }
    }

    private var debounceApplyJob: kotlinx.coroutines.Job? = null

    private fun debounceApplyAudio(profile: EqProfile) {
        debounceApplyJob?.cancel()
        debounceApplyJob = viewModelScope.launch {
            delay(80)
            audioEngine.updateActiveProfile(profile)
        }
    }

    fun updateBand(frequency: String, value: Float, applyAudioNow: Boolean = false) {
        val current = _currentProfile.value
        val updated = when (frequency) {
            "60Hz" -> current.copy(band60Hz = value)
            "120Hz" -> current.copy(band120Hz = value)
            "250Hz" -> current.copy(band250Hz = value)
            "500Hz" -> current.copy(band500Hz = value)
            "1kHz" -> current.copy(band1kHz = value)
            "2kHz" -> current.copy(band2kHz = value)
            "4kHz" -> current.copy(band4kHz = value)
            "8kHz" -> current.copy(band8kHz = value)
            "16kHz" -> current.copy(band16kHz = value)
            else -> current
        }
        _currentProfile.value = updated
        if (applyAudioNow) {
            debounceApplyJob?.cancel()
            audioEngine.updateActiveProfile(updated)
            saveActiveProfileToDb(updated)
        } else {
            debounceApplyAudio(updated)
        }
    }

    fun updateReverb(preset: Int) {
        val intensity = when (preset) {
            1 -> 200f
            2 -> 300f
            3 -> 500f
            4 -> 600f
            5 -> 1000f
            6 -> 400f
            else -> 0f
        }
        val updated = _currentProfile.value.copy(reverbPreset = preset, reverbIntensity = intensity)
        _currentProfile.value = updated
        debounceApplyJob?.cancel()
        audioEngine.updateActiveProfile(updated)
        saveActiveProfileToDb(updated)
    }

    fun updateReverbIntensity(strength: Float, applyAudioNow: Boolean = false) {
        val updated = _currentProfile.value.copy(reverbIntensity = strength)
        _currentProfile.value = updated
        if (applyAudioNow) {
            debounceApplyJob?.cancel()
            audioEngine.updateActiveProfile(updated)
            saveActiveProfileToDb(updated)
        } else {
            debounceApplyAudio(updated)
        }
    }

    fun updateBassBoost(strength: Float, applyAudioNow: Boolean = false) {
        val updated = _currentProfile.value.copy(bassBoost = strength)
        _currentProfile.value = updated
        if (applyAudioNow) {
            debounceApplyJob?.cancel()
            audioEngine.updateActiveProfile(updated)
            saveActiveProfileToDb(updated)
        } else {
            debounceApplyAudio(updated)
        }
    }

    fun updateVirtualizer(strength: Float, applyAudioNow: Boolean = false) {
        val updated = _currentProfile.value.copy(virtualizer = strength)
        _currentProfile.value = updated
        if (applyAudioNow) {
            debounceApplyJob?.cancel()
            audioEngine.updateActiveProfile(updated)
            saveActiveProfileToDb(updated)
        } else {
            debounceApplyAudio(updated)
        }
    }

    fun updateEqualLoudness(enabled: Boolean, threshold: Float, applyAudioNow: Boolean = false) {
        val updated = _currentProfile.value.copy(equalLoudnessEnabled = enabled, equalLoudnessThresholdDb = threshold)
        _currentProfile.value = updated
        if (applyAudioNow) {
            debounceApplyJob?.cancel()
            audioEngine.updateActiveProfile(updated)
            saveActiveProfileToDb(updated)
        } else {
            debounceApplyAudio(updated)
        }
    }

    fun updateBassTuner(mode: Int, cutoff: Float, gain: Float, applyAudioNow: Boolean = false) {
        val updated = _currentProfile.value.copy(bassTunerMode = mode, bassTunerCutoff = cutoff, bassTunerPostGain = gain)
        _currentProfile.value = updated
        if (applyAudioNow) {
            debounceApplyJob?.cancel()
            audioEngine.updateActiveProfile(updated)
            saveActiveProfileToDb(updated)
        } else {
            debounceApplyAudio(updated)
        }
    }

    fun updateLimiter(enabled: Boolean, threshold: Float, ratio: Float, attack: Float, release: Float, applyAudioNow: Boolean = false) {
        val updated = _currentProfile.value.copy(
            limiterEnabled = enabled,
            limiterThresholdDb = threshold,
            limiterRatio = ratio,
            limiterAttackMs = attack,
            limiterReleaseMs = release
        )
        _currentProfile.value = updated
        if (applyAudioNow) {
            debounceApplyJob?.cancel()
            audioEngine.updateActiveProfile(updated)
            saveActiveProfileToDb(updated)
        } else {
            debounceApplyAudio(updated)
        }
    }

    fun updateAutomatedGainControl(enabled: Boolean) {
        val updated = _currentProfile.value.copy(automatedGainControlEnabled = enabled)
        _currentProfile.value = updated
        debounceApplyJob?.cancel()
        audioEngine.updateActiveProfile(updated)
        saveActiveProfileToDb(updated)
    }

    fun updateMasterNormalization(enabled: Boolean) {
        val updated = _currentProfile.value.copy(masterNormalizationEnabled = enabled)
        _currentProfile.value = updated
        debounceApplyJob?.cancel()
        audioEngine.updateActiveProfile(updated)
        saveActiveProfileToDb(updated)
    }

    fun updateAttenuation(auto: Boolean, value: Float, applyAudioNow: Boolean = false) {
        val updated = _currentProfile.value.copy(autoAttenuationEnabled = auto, manualAttenuationDb = value)
        _currentProfile.value = updated
        if (applyAudioNow) {
            debounceApplyJob?.cancel()
            audioEngine.updateActiveProfile(updated)
            saveActiveProfileToDb(updated)
        } else {
            debounceApplyAudio(updated)
        }
    }

    fun updateChannelBalance(balance: Float, applyAudioNow: Boolean = false) {
        val updated = _currentProfile.value.copy(channelBalance = balance)
        _currentProfile.value = updated
        if (applyAudioNow) {
            debounceApplyJob?.cancel()
            audioEngine.updateActiveProfile(updated)
            saveActiveProfileToDb(updated)
        } else {
            debounceApplyAudio(updated)
        }
    }

    // Toggle presets or custom EQ profiles
    fun selectActiveProfile(profile: EqProfile) {
        _currentProfile.value = profile
        audioEngine.updateActiveProfile(profile)
        
        // Auto-refresh device mapping matching the current connected bluetooth headset
        viewModelScope.launch {
            val dev = _connectedDeviceName.value
            if (dev.isNotBlank() && profile.id > 0) {
                repository.insertDeviceMapping(DeviceMapping(dev, profile.id))
            }
        }
    }

    // Creating, saving, and deleting profiles in database
    fun saveAsNewProfile(profileName: String, mediaType: String = "Music") {
        if (profileName.isBlank()) return
        viewModelScope.launch {
            val current = _currentProfile.value
            val newProfile = current.copy(
                id = 0, // autoGenerate
                name = profileName,
                isCustom = true,
                mediaType = mediaType
            )
            val insertId = repository.insertProfile(newProfile)
            val savedProfile = newProfile.copy(id = insertId.toInt())
            _currentProfile.value = savedProfile
            audioEngine.updateActiveProfile(savedProfile)

            // Automap device to newly saved profile
            val dev = _connectedDeviceName.value
            if (dev.isNotBlank()) {
                repository.insertDeviceMapping(DeviceMapping(dev, insertId.toInt()))
            }
        }
    }

    fun resetSlidersToDefault() {
        val current = _currentProfile.value
        val updated = current.copy(
            band60Hz = 0f,
            band120Hz = 0f,
            band250Hz = 0f,
            band500Hz = 0f,
            band1kHz = 0f,
            band2kHz = 0f,
            band4kHz = 0f,
            band8kHz = 0f,
            band16kHz = 0f
        )
        _currentProfile.value = updated
        audioEngine.updateActiveProfile(updated)
    }

    fun resetGainAndNormalizationSettings() {
        val current = _currentProfile.value
        val updated = current.copy(
            automatedGainControlEnabled = false,
            masterNormalizationEnabled = false,
            autoAttenuationEnabled = true,
            manualAttenuationDb = 0f,
            channelBalance = 0f,
            limiterEnabled = false,
            limiterThresholdDb = -3.0f,
            limiterRatio = 2.0f,
            limiterAttackMs = 5.0f,
            limiterReleaseMs = 50.0f
        )
        _currentProfile.value = updated
        audioEngine.updateActiveProfile(updated)
    }

    fun deleteCustomProfile(profile: EqProfile) {
        viewModelScope.launch {
            repository.deleteProfile(profile)
            _currentProfile.value = EqProfile.FLAT
            audioEngine.updateActiveProfile(EqProfile.FLAT)
        }
    }

    fun mappingDeviceToProfile(deviceName: String, profileId: Int) {
        viewModelScope.launch {
            repository.insertDeviceMapping(DeviceMapping(deviceName, profileId))
        }
    }

    fun removeDeviceMapping(deviceName: String) {
        viewModelScope.launch {
            repository.deleteDeviceMapping(deviceName)
        }
    }

    fun setSimulatedConnectedDevice(name: String) {
        _connectedDeviceName.value = name
        // Check database if mapping exists, load profile automatically
        viewModelScope.launch {
            val mapping = repository.getDeviceMapping(name)
            if (mapping != null) {
                val mappedProfile = repository.getProfileById(mapping.profileId)
                if (mappedProfile != null) {
                    _currentProfile.value = mappedProfile
                    audioEngine.updateActiveProfile(mappedProfile)
                }
            }
        }
    }

    // Setting configurations
    fun setBufferSize(valText: String) {
        _bufferSize.value = valText
    }

    fun toggleLegacyMode(enabled: Boolean) {
        _legacyMode.value = enabled
        audioEngine.setLegacyMode(enabled)
    }

    fun updateVisualizerStyle(style: String) {
        _visualizerStyle.value = style
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // AutoEq load curve
    fun loadAutoEqHeadphone(eq: AutoEqHeadphone) {
        val current = _currentProfile.value
        val updated = current.copy(
            band60Hz = eq.gains[0],
            band120Hz = eq.gains[1],
            band250Hz = eq.gains[2],
            band500Hz = eq.gains[3],
            band1kHz = eq.gains[4],
            band2kHz = eq.gains[5],
            band4kHz = eq.gains[6],
            band8kHz = eq.gains[7],
            band16kHz = eq.gains[8]
        )
        // Set profile name indicating autoeq
        val profileWithAutoEqName = updated.copy(
            id = 0,
            name = "AutoEq: ${eq.brand} ${eq.model}",
            isCustom = true
        )
        viewModelScope.launch {
            val insertId = repository.insertProfile(profileWithAutoEqName)
            val finalProfile = profileWithAutoEqName.copy(id = insertId.toInt())
            _currentProfile.value = finalProfile
            audioEngine.updateActiveProfile(finalProfile)
            
            // Map connected device
            val dev = _connectedDeviceName.value
            if (dev.isNotBlank()) {
                repository.insertDeviceMapping(DeviceMapping(dev, insertId.toInt()))
            }
        }
    }

    // Import from csv/squig text editor pasted data
    fun importCustomAutoEqCurve(name: String, textData: String): Boolean {
        if (name.isBlank() || textData.isBlank()) return false
        
        try {
            val userFreqs = floatArrayOf(60f, 120f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)
            val parsedGains = FloatArray(9) { 0f }
            val dataPoints = mutableListOf<Pair<Float, Float>>()

            val lines = textData.lines()
            for (line in lines) {
                val cleaned = line.trim()
                if (cleaned.startsWith("#") || cleaned.isBlank()) continue
                
                // match numbers (comma, tab, space, semi-colon separators)
                val parts = cleaned.split(Regex("\\s+|,|;"))
                if (parts.size >= 2) {
                    val f = parts[0].toFloatOrNull()
                    val g = parts[1].toFloatOrNull()
                    if (f != null && g != null) {
                        dataPoints.add(Pair(f, g))
                    }
                }
            }

            if (dataPoints.isEmpty()) return false

            // Map standard frequencies by finding the closest parsed frequency
            for (i in 0 until 9) {
                val targetFreq = userFreqs[i]
                val closestPoint = dataPoints.minByOrNull { Math.abs(it.first - targetFreq) }
                parsedGains[i] = closestPoint?.second?.coerceIn(-15f, 15f) ?: 0f
            }

            val newProfile = EqProfile(
                id = 0,
                name = "Pasted: $name",
                isCustom = true,
                band60Hz = parsedGains[0],
                band120Hz = parsedGains[1],
                band250Hz = parsedGains[2],
                band500Hz = parsedGains[3],
                band1kHz = parsedGains[4],
                band2kHz = parsedGains[5],
                band4kHz = parsedGains[6],
                band8kHz = parsedGains[7],
                band16kHz = parsedGains[8]
            )

            viewModelScope.launch {
                val insertId = repository.insertProfile(newProfile)
                val finalProfile = newProfile.copy(id = insertId.toInt())
                _currentProfile.value = finalProfile
                audioEngine.updateActiveProfile(finalProfile)

                val dev = _connectedDeviceName.value
                if (dev.isNotBlank()) {
                    repository.insertDeviceMapping(DeviceMapping(dev, insertId.toInt()))
                }
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }
}

class EqViewModelFactory(private val repository: EqRepository, private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EqViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EqViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
