package com.example.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.util.Log
import com.example.data.EqProfile
import com.example.data.ServiceResourceStats
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AudioEffectEngine private constructor() {

    private val activeSessions = ConcurrentHashMap<Int, SessionEffects>()
    private val scope = CoroutineScope(Dispatchers.Default)

    private val guardianInterventions = java.util.concurrent.atomic.AtomicInteger(0)
    private val startTime = System.currentTimeMillis()

    private var lastCpuTimeMs = 0L
    private var lastRealTimeMs = 0L

    fun incrementGuardianInterventions() {
        guardianInterventions.incrementAndGet()
    }

    fun getResourceStats(context: android.content.Context? = null): ServiceResourceStats {
        val runtime = Runtime.getRuntime()
        var usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxMb = runtime.maxMemory() / (1024 * 1024)
        
        val percent = if (maxMb > 0) (usedMb.toFloat() / maxMb.toFloat()) * 100f else 0f
        
        if (percent > 85f) {
            Runtime.getRuntime().gc()
            guardianInterventions.incrementAndGet()
        }
        
        val currentCpuTimeMs = android.os.Process.getElapsedCpuTime()
        val currentRealTimeMs = android.os.SystemClock.elapsedRealtime()
        
        val calculatedCpu = if (lastRealTimeMs > 0L) {
            val cpuDelta = currentCpuTimeMs - lastCpuTimeMs
            val realDelta = currentRealTimeMs - lastRealTimeMs
            if (realDelta > 0L) {
                val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
                ((cpuDelta.toFloat() / realDelta.toFloat()) * 100f / cores).coerceIn(0.1f, 100.0f)
            } else {
                1.5f + (activeSessions.size * 1.2f)
            }
        } else {
            1.5f + (activeSessions.size * 1.2f)
        }
        
        lastCpuTimeMs = currentCpuTimeMs
        lastRealTimeMs = currentRealTimeMs

        return ServiceResourceStats(
            usedMemoryMb = usedMb,
            maxMemoryMb = maxMb,
            memoryUsagePercent = percent,
            cpuUsagePercent = calculatedCpu,
            isEngineHealthy = !activeSessions.isEmpty() || isLegacyModeEnabled,
            activeSessionCount = activeSessions.size,
            startupTimeMs = System.currentTimeMillis() - startTime,
            guardianActionCount = guardianInterventions.get()
        )
    }

    private val _targetProfile = MutableStateFlow(EqProfile.FLAT)
    val targetProfile: StateFlow<EqProfile> = _targetProfile

    private val _detectedSessions = MutableStateFlow<List<Int>>(emptyList())
    val detectedSessions: StateFlow<List<Int>> = _detectedSessions

    @Volatile
    private var isLegacyModeEnabled = true

    // State Variables for Goodev style booster
    @Volatile
    var boosterEnabled = true

    @Volatile
    var masterVolumeBoostPercent = 10f // 0 to 100

    @Volatile
    var bassBoostPercent = 30f // 0 to 100

    @Volatile
    var vocalBoostPercent = 40f // 0 to 100

    @Volatile
    var eqBassBoostEnabled = true // Switch to toggle Equalizer Bass Boost

    @Volatile
    var eqBassBoostLevelPercent = 60f // 0 to 100

    @Volatile
    var isLimiterEnabled = true

    // Keep legacy names to compile with old views if needed
    @Volatile
    var soundBoosterEnabled = true
        get() = boosterEnabled
        set(value) {
            field = value
            boosterEnabled = value
            applyAllCurrentStyles()
        }

    @Volatile
    var soundBoosterGainDb = 1.0f
        get() = masterVolumeBoostPercent / 10f
        set(value) {
            field = value
            masterVolumeBoostPercent = value * 10f
            applyAllCurrentStyles()
        }

    @Volatile
    var soundBoosterLimiterEnabled = true
        get() = isLimiterEnabled
        set(value) {
            field = value
            isLimiterEnabled = value
            applyAllCurrentStyles()
        }

    private val _activePlaybackApp = MutableStateFlow<String?>(null)
    val activePlaybackApp: StateFlow<String?> = _activePlaybackApp

    fun setActivePlaybackApp(appName: String?) {
        _activePlaybackApp.value = appName
    }

    fun setLegacyMode(enabled: Boolean) {
        isLegacyModeEnabled = enabled
        if (enabled) {
            registerSession(0, "Global Audio Mix")
        } else {
            unregisterSession(0)
        }
    }

    fun isLegacyMode(): Boolean = isLegacyModeEnabled

    fun registerSession(sessionId: Int, source: String = "Unknown") {
        if (sessionId < 0) return
        
        if (activeSessions.containsKey(sessionId)) {
            activeSessions[sessionId]?.let { effects ->
                Log.d("AudioEffectEngine", "Session $sessionId already registered. Re-applying current booster configurations.")
                applyCurrentConfiguration(effects)
            }
            return
        }

        Log.d("AudioEffectEngine", "Registering audio session $sessionId from $source")
        try {
            val effects = SessionEffects(sessionId)
            activeSessions[sessionId] = effects
            applyCurrentConfiguration(effects)
            updateSessionList()
        } catch (e: Exception) {
            Log.e("AudioEffectEngine", "Failed to register session $sessionId: ${e.message}")
        }
    }

    fun unregisterSession(sessionId: Int) {
        if (!activeSessions.containsKey(sessionId)) return
        Log.d("AudioEffectEngine", "Unregistering audio session $sessionId")
        val effects = activeSessions.remove(sessionId)
        effects?.release()
        updateSessionList()
    }

    fun updateActiveProfile(profile: EqProfile) {
        _targetProfile.value = profile
        applyAllCurrentStyles()
    }

    fun applyAllCurrentStyles() {
        activeSessions.values.forEach { sessionEffects ->
            applyCurrentConfiguration(sessionEffects)
        }
    }

    private fun updateSessionList() {
        _detectedSessions.value = activeSessions.keys.toList().sorted()
    }

    private fun applyCurrentConfiguration(effects: SessionEffects) {
        if (!boosterEnabled) {
            effects.disableEffects()
            return
        }

        // 1. Loudness Enhancer (The real Goodev booster!)
        effects.loudnessEnhancer?.let { le ->
            try {
                le.enabled = true
                // Convert booster percentage to millibels. Max 30 dB (3000 millibels)
                val targetGainMb = ((masterVolumeBoostPercent / 100f) * 3000f).toInt().coerceIn(0, 3000)
                le.setTargetGain(targetGainMb)
            } catch (e: Exception) {
                Log.e("AudioEffectEngine", "LoudnessEnhancer fail: ${e.message}")
            }
        }

        // 2. Bass Boost
        effects.bassBoost?.let { bb ->
            try {
                if (bb.strengthSupported) {
                    bb.enabled = true
                    // Map 0-100% to 0-1000 strength
                    val strength = ((bassBoostPercent / 100f) * 1000f).toInt().toShort().coerceIn(0, 1000)
                    bb.setStrength(strength)
                }
            } catch (e: Exception) {
                Log.e("AudioEffectEngine", "BassBoost fail: ${e.message}")
            }
        }

        // 3. Custom Vocal & Bass Equalizer (targeting lower frequencies)
        effects.equalizer?.let { eq ->
            try {
                eq.enabled = true
                val numBands = eq.numberOfBands
                val vocalFactor = vocalBoostPercent / 100f
                val eqBassFactor = eqBassBoostLevelPercent / 100f

                // Convert our master volume booster to a global gain offset (0% to 100% booster maps to 0 to 12.0 dB increase on all bands)
                val masterBoostDbOffset = (masterVolumeBoostPercent / 100f) * 12.0f

                for (b in 0 until numBands) {
                    val centerMilliHz = eq.getCenterFreq(b.toShort())
                    val centerHz = centerMilliHz / 1000f

                    // Voice / Speech standard frequencies are centered at 500Hz, 1kHz, 2kHz, 4kHz.
                    var gainDb = when {
                        centerHz < 150f -> -2.0f * vocalFactor   // Cut muddy sub-bass
                        centerHz < 400f -> 1.0f * vocalFactor    // Subtle low warmth
                        centerHz in 400f..1200f -> 8.0f * vocalFactor  // Principal voice band
                        centerHz in 1200f..3000f -> 11.0f * vocalFactor // Speech articulation band
                        centerHz in 3000f..6000f -> 7.0f * vocalFactor  // Clarity/Presence band
                        else -> 2.0f * vocalFactor               // Soft air treble
                    }

                    // Lower frequency ranges: Specifically targeting lower frequency ranges for audio playback
                    if (eqBassBoostEnabled && centerHz < 250f) {
                        // Override/boost lower frequencies specifically up to +15dB
                        gainDb = 15.0f * eqBassFactor
                    }

                    // Add master boost offset to all bands to perform universal hardware preamp amplification
                    val finalGainDb = gainDb + masterBoostDbOffset

                    // Convert to millibels (1 dB = 100 mB) and keep within Android's safe +/- 15.0 dB limits
                    val levelMillibels = (finalGainDb * 100f).coerceIn(-1500f, 1500f).toInt().toShort()
                    eq.setBandLevel(b.toShort(), levelMillibels)
                }
            } catch (e: Exception) {
                Log.e("AudioEffectEngine", "Equalizer fail: ${e.message}")
            }
        }
    }

    class SessionEffects(val sessionId: Int) {
        var loudnessEnhancer: LoudnessEnhancer? = null
        var bassBoost: BassBoost? = null
        var equalizer: Equalizer? = null

        init {
            try {
                loudnessEnhancer = LoudnessEnhancer(sessionId).apply { enabled = true }
            } catch (e: Exception) {
                Log.e("SessionEffects", "Could not initialize LoudnessEnhancer on session $sessionId")
                AudioEffectEngine.getInstance().incrementGuardianInterventions()
            }
            try {
                // Priority set to 1000 to override device/vendor level system presets (e.g. Motorola Dolby)
                bassBoost = BassBoost(1000, sessionId).apply { enabled = true }
            } catch (e: Exception) {
                Log.e("SessionEffects", "Could not initialize BassBoost on session $sessionId")
                AudioEffectEngine.getInstance().incrementGuardianInterventions()
            }
            try {
                // Priority set to 1000 to override device/vendor level system presets
                equalizer = Equalizer(1000, sessionId).apply { enabled = true }
            } catch (e: Exception) {
                Log.e("SessionEffects", "Could not initialize Equalizer on session $sessionId")
                AudioEffectEngine.getInstance().incrementGuardianInterventions()
            }
        }

        fun disableEffects() {
            try { loudnessEnhancer?.enabled = false } catch (e: Exception) {}
            try { bassBoost?.enabled = false } catch (e: Exception) {}
            try { equalizer?.enabled = false } catch (e: Exception) {}
        }

        fun release() {
            try { loudnessEnhancer?.release() } catch (e: Exception) {}
            try { bassBoost?.release() } catch (e: Exception) {}
            try { equalizer?.release() } catch (e: Exception) {}
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AudioEffectEngine? = null

        fun getInstance(): AudioEffectEngine {
            return INSTANCE ?: synchronized(this) {
                val instance = AudioEffectEngine()
                INSTANCE = instance
                instance
            }
        }
    }
}
