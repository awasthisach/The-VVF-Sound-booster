package com.example.ui

import android.content.Context
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

    val deviceMappings: StateFlow<List<DeviceMapping>> = repository.allDeviceMappings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Device setup (simulated Bluetooth tracker)
    private val _connectedDeviceName = MutableStateFlow("Sony WH-1000XM4")
    val connectedDeviceName: StateFlow<String> = _connectedDeviceName.asStateFlow()

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

    // Visualizer Styles
    private val _visualizerStyle = MutableStateFlow("Cosmic Neon") // Cosmic Neon, Cyberpunk Peak, Minimalist
    val visualizerStyle: StateFlow<String> = _visualizerStyle.asStateFlow()

    // Settings elements
    private val _bufferSize = MutableStateFlow("Medium (1024)")
    val bufferSize: StateFlow<String> = _bufferSize.asStateFlow()

    private val _legacyMode = MutableStateFlow(true)
    val legacyMode: StateFlow<Boolean> = _legacyMode.asStateFlow()

    private val _enhancedDetection = MutableStateFlow(false)
    val enhancedDetection: StateFlow<Boolean> = _enhancedDetection.asStateFlow()

    private val _detectedSessions = audioEngine.detectedSessions
    val detectedSessions: StateFlow<List<Int>> = _detectedSessions

    val serviceStats: StateFlow<com.example.data.ServiceResourceStats> by lazy {
        _serviceStats.asStateFlow()
    }
    private val _serviceStats = MutableStateFlow(audioEngine.getResourceStats())

    init {
        // Poll service resource stats every 2 seconds
        viewModelScope.launch {
            while (true) {
                try {
                    _serviceStats.value = audioEngine.getResourceStats()
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
                    repository.insertProfile(profile)
                }
            } catch (e: Exception) {
                android.util.Log.e("EqViewModel", "Failed prepopulating default profiles", e)
            }
        }

        // Sync engine profile on initial launch
        audioEngine.setLegacyMode(true)
        audioEngine.updateActiveProfile(_currentProfile.value)

        // Run active physics loop for real-time visualizers
        viewModelScope.launch(Dispatchers.Default) {
            var wavePhase = 0f
            while (true) {
                val profile = _currentProfile.value
                
                // Base amplitudes scaled on EQ slider gains
                val subBassGain = (profile.band60Hz + 15f) / 30f // normalized 0..1
                val vocalGain = (profile.band1kHz + 15f) / 30f
                val trebleGain = (profile.band16kHz + 15f) / 30f
                
                val bassBoostCoeff = (profile.bassBoost / 1000f).coerceAtLeast(0f)

                // 1. Generate Organic Waveform
                wavePhase += 0.15f
                val nextWave = FloatArray(64)
                for (i in 0 until 64) {
                    val t = i.toFloat() / 64f
                    val primaryFreq = sin(t * 12f + wavePhase) * 0.4f * (0.5f + subBassGain * 0.5f + bassBoostCoeff * 0.3f)
                    val vocalHarmonic = sin(t * 35f - wavePhase * 1.5f) * 0.15f * (0.3f + vocalGain * 0.7f)
                    val trebleFlicker = sin(t * 80f + wavePhase * 4f) * 0.05f * (0.2f + trebleGain * 0.8f)
                    
                    nextWave[i] = (primaryFreq + vocalHarmonic + trebleFlicker).coerceIn(-1f, 1f)
                }
                _waveformPoints.value = nextWave

                // 2. Generate Decaying Spectrum Bars with Physics
                val nextSpectrum = FloatArray(24)
                val currentSpectrum = _spectrumBars.value
                for (j in 0 until 24) {
                    val progress = j.toFloat() / 24f
                    
                    // Base target based on user EQ levels and random noise
                    val baseTarget = when {
                        progress < 0.25f -> 0.2f + (subBassGain * 0.6f) + (bassBoostCoeff * 0.2f)
                        progress < 0.65f -> 0.15f + (vocalGain * 0.5f)
                        else -> 0.1f + (trebleGain * 0.6f)
                    }

                    // Add organic flicker
                    val flicker = (sin(wavePhase * 5f + j * 0.8f) * 0.1f + 0.1f)
                    val targetAmplitude = (baseTarget * flicker * 1.2f).coerceIn(0.05f, 1.0f)

                    // Physics damping: slow decay, rapid rise
                    val prev = if (currentSpectrum.size > j) currentSpectrum[j] else 0f
                    if (targetAmplitude > prev) {
                        nextSpectrum[j] = prev + (targetAmplitude - prev) * 0.5f
                    } else {
                        nextSpectrum[j] = prev - 0.08f // decay
                    }
                    nextSpectrum[j] = nextSpectrum[j].coerceIn(0.03f, 1.0f)
                }
                _spectrumBars.value = nextSpectrum

                delay(16) // ~60fps
            }
        }
    }

    fun selectTab(tab: Int) {
        _currentTab.value = tab
    }

    fun updateBand(frequency: String, value: Float) {
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
        audioEngine.updateActiveProfile(updated)
    }

    fun updateReverb(preset: Int) {
        val updated = _currentProfile.value.copy(reverbPreset = preset)
        _currentProfile.value = updated
        audioEngine.updateActiveProfile(updated)
    }

    fun updateBassBoost(strength: Float) {
        val updated = _currentProfile.value.copy(bassBoost = strength)
        _currentProfile.value = updated
        audioEngine.updateActiveProfile(updated)
    }

    fun updateVirtualizer(strength: Float) {
        val updated = _currentProfile.value.copy(virtualizer = strength)
        _currentProfile.value = updated
        audioEngine.updateActiveProfile(updated)
    }

    fun updateEqualLoudness(enabled: Boolean, threshold: Float) {
        val updated = _currentProfile.value.copy(equalLoudnessEnabled = enabled, equalLoudnessThresholdDb = threshold)
        _currentProfile.value = updated
        audioEngine.updateActiveProfile(updated)
    }

    fun updateBassTuner(mode: Int, cutoff: Float, gain: Float) {
        val updated = _currentProfile.value.copy(bassTunerMode = mode, bassTunerCutoff = cutoff, bassTunerPostGain = gain)
        _currentProfile.value = updated
        audioEngine.updateActiveProfile(updated)
    }

    fun updateLimiter(enabled: Boolean, threshold: Float, ratio: Float, attack: Float, release: Float) {
        val updated = _currentProfile.value.copy(
            limiterEnabled = enabled,
            limiterThresholdDb = threshold,
            limiterRatio = ratio,
            limiterAttackMs = attack,
            limiterReleaseMs = release
        )
        _currentProfile.value = updated
        audioEngine.updateActiveProfile(updated)
    }

    fun updateAutomatedGainControl(enabled: Boolean) {
        val updated = _currentProfile.value.copy(automatedGainControlEnabled = enabled)
        _currentProfile.value = updated
        audioEngine.updateActiveProfile(updated)
    }

    fun updateMasterNormalization(enabled: Boolean) {
        val updated = _currentProfile.value.copy(masterNormalizationEnabled = enabled)
        _currentProfile.value = updated
        audioEngine.updateActiveProfile(updated)
    }

    fun updateAttenuation(auto: Boolean, value: Float) {
        val updated = _currentProfile.value.copy(autoAttenuationEnabled = auto, manualAttenuationDb = value)
        _currentProfile.value = updated
        audioEngine.updateActiveProfile(updated)
    }

    fun updateChannelBalance(balance: Float) {
        val updated = _currentProfile.value.copy(channelBalance = balance)
        _currentProfile.value = updated
        audioEngine.updateActiveProfile(updated)
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

    fun toggleEnhancedDetection(enabled: Boolean) {
        _enhancedDetection.value = enabled
        audioEngine.setEnhancedPolling(enabled)
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
