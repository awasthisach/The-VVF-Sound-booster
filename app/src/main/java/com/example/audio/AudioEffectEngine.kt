package com.example.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.EnvironmentalReverb
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
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
        var maxMb = runtime.maxMemory() / (1024 * 1024)
        
        if (context != null) {
            try {
                val activityManager = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
                if (activityManager != null) {
                    val memoryInfo = android.app.ActivityManager.MemoryInfo()
                    activityManager.getMemoryInfo(memoryInfo)
                    
                    val debugMemInfo = android.os.Debug.MemoryInfo()
                    android.os.Debug.getMemoryInfo(debugMemInfo)
                    val pssMb = debugMemInfo.totalPss / 1024L
                    if (pssMb > 0) {
                        usedMb = pssMb
                    }
                }
            } catch (e: Exception) {
                Log.e("AudioEffectEngine", "Failed to retrieve exact MemoryInfo: ${e.message}")
            }
        }
        
        val percent = if (maxMb > 0) (usedMb.toFloat() / maxMb.toFloat()) * 100f else 0f
        
        // Auto-GC safety threshold to prevent OOM
        if (percent > 85f) {
            Runtime.getRuntime().gc()
            guardianInterventions.incrementAndGet()
            Log.w("AudioEffectEngine", "Dynamic resource guardian triggered GC due to high memory ($usedMb MB / $maxMb MB)")
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

    @Volatile
    var soundBoosterEnabled = false

    @Volatile
    var soundBoosterGainDb = 0f

    private val _activePlaybackApp = MutableStateFlow<String?>(null)
    val activePlaybackApp: StateFlow<String?> = _activePlaybackApp

    fun setActivePlaybackApp(appName: String?) {
        _activePlaybackApp.value = appName
    }

    fun setLegacyMode(enabled: Boolean) {
        isLegacyModeEnabled = enabled
        if (enabled) {
            registerSession(0, "Legacy System Mix")
        } else {
            unregisterSession(0)
        }
    }

    fun isLegacyMode(): Boolean = isLegacyModeEnabled

    fun registerSession(sessionId: Int, source: String = "Unknown") {
        if (sessionId < 0) return
        if (activeSessions.containsKey(sessionId)) return

        Log.d("AudioEffectEngine", "Registering audio session $sessionId from $source")
        try {
            val effects = SessionEffects(sessionId)
            activeSessions[sessionId] = effects
            applyProfileToEffects(effects, _targetProfile.value)
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
        activeSessions.values.forEach { sessionEffects ->
            applyProfileToEffects(sessionEffects, profile)
        }
    }

    private fun updateSessionList() {
        _detectedSessions.value = activeSessions.keys.toList().sorted()
    }

    private fun applyProfileToEffects(effects: SessionEffects, profile: EqProfile) {
        // 1. Graphic EQ
        effects.equalizer?.let { eq ->
            try {
                val numBands = eq.numberOfBands
                val userFrequencies = floatArrayOf(60f, 120f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)
                val userGains = profile.toBandArray()

                for (b in 0 until numBands) {
                    val centerMilliHz = eq.getCenterFreq(b.toShort())
                    val centerHz = centerMilliHz / 1000f
                    val gain = interpolateGain(centerHz, userFrequencies, userGains)

                    // Target Bass Tuner adjustments on sub-bass frequencies
                    var finalGain = gain
                    if (centerHz < profile.bassTunerCutoff) {
                        if (profile.bassTunerMode == 0) { // Natural Deep rumble booster
                            finalGain += profile.bassTunerPostGain
                        } else if (profile.bassTunerMode == 2) { // Sustain rumble booster
                            finalGain += (profile.bassTunerPostGain * 1.5f)
                        }
                    }

                    val levelMillibels = (finalGain * 100f).coerceIn(-1500f, 1500f).toInt().toShort()
                    eq.setBandLevel(b.toShort(), levelMillibels)
                }
            } catch (e: Exception) {
                Log.e("AudioEffectEngine", "EQ fail: ${e.message}")
            }
        }

        // 2. Bass Boost
        effects.bassBoost?.let { bb ->
            try {
                if (bb.strengthSupported) {
                    // Combine standard bassBoost slider with Bass Tuner state
                    var strengthFactor = profile.bassBoost
                    if (profile.bassTunerMode == 1) { // Transient compress - snap dynamic booster
                        strengthFactor = (strengthFactor * 0.8f).coerceAtLeast(300f)
                    } else if (profile.bassTunerMode == 2) { // Sustain rumble booster
                        strengthFactor = (strengthFactor * 1.3f).coerceAtMost(1000f)
                    }
                    bb.setStrength(strengthFactor.toInt().toShort())
                }
            } catch (e: Exception) {
                Log.e("AudioEffectEngine", "BassBoost fail: ${e.message}")
            }
        }

        // 3. Virtualizer
        effects.virtualizer?.let { v ->
            try {
                if (v.strengthSupported) {
                    v.setStrength(profile.virtualizer.toInt().toShort())
                }
            } catch (e: Exception) {
                Log.e("AudioEffectEngine", "Virtualizer fail: ${e.message}")
            }
        }

        // 4. Reverb Presets
        effects.reverb?.let { r ->
            try {
                if (profile.reverbIntensity > 0f) {
                    r.enabled = true
                    val calcDecay = (100 + (profile.reverbIntensity / 1000f) * 4900f).toInt()
                    r.decayTime = calcDecay
                    try {
                        val calcLevel = (-9000 + (profile.reverbIntensity / 1000f) * 9000f).toInt().toShort()
                        r.reverbLevel = calcLevel
                    } catch (e: Exception) {
                        // Some older versions or devices do not fully support changing reverbLevel
                    }
                } else if (profile.reverbPreset in 1..6) {
                    r.enabled = true
                    when (profile.reverbPreset) {
                        1 -> r.decayTime = 1000 // Small Room
                        2 -> r.decayTime = 1500 // Medium Room
                        3 -> r.decayTime = 2500 // Large Room
                        4 -> r.decayTime = 3000 // Medium Hall
                        5 -> r.decayTime = 5000 // Large Hall
                        6 -> r.decayTime = 2000 // Plate Reverb
                    }
                } else {
                    r.enabled = false
                }
            } catch (e: Exception) {
                Log.e("AudioEffectEngine", "Reverb fail: ${e.message}")
            }
        }

        // 5. DynamicsProcessing & Limiter (Android 9+)
        effects.recreateDynamics(profile)
    }

    private fun interpolateGain(freq: Float, userFreqs: FloatArray, userGains: FloatArray): Float {
        if (freq <= userFreqs.first()) return userGains.first()
        if (freq >= userFreqs.last()) return userGains.last()

        for (i in 0 until userFreqs.size - 1) {
            if (freq >= userFreqs[i] && freq <= userFreqs[i + 1]) {
                val f1 = userFreqs[i]
                val f2 = userFreqs[i + 1]
                val g1 = userGains[i]
                val g2 = userGains[i + 1]
                val t = (freq - f1) / (f2 - f1)
                return g1 + t * (g2 - g1)
            }
        }
        return 0f
    }



    private fun buildDynamicsConfig(profile: EqProfile): DynamicsProcessing.Config? {
        try {
            val attenuationDb = if (profile.masterNormalizationEnabled) {
                // Calculate total potential gain overhead from the active preset components
                val maxBandBoost = profile.toBandArray().maxOrNull()?.coerceAtLeast(0f) ?: 0f
                val bassBoostDb = (profile.bassBoost / 1000f) * 12.0f // up to 12dB boost
                val bassTunerDb = if (profile.bassTunerPostGain > 0) profile.bassTunerPostGain else 0f
                val virtualizerDb = (profile.virtualizer / 1000f) * 4.0f
                
                // Estimate peak preset gain
                val presetPeakGain = maxBandBoost + bassBoostDb + bassTunerDb + virtualizerDb
                
                // Master Normalization: automatically scales input gain downward by the exact preset overhead
                // to match reference headroom, providing an incredibly smooth and balanced transition.
                val normalizationOffset = if (presetPeakGain > 0f) {
                    -(presetPeakGain * 0.70f).coerceIn(1.5f, 18.0f)
                } else {
                    -0.5f
                }
                
                // If AGC is also enabled, add slight safety pad
                if (profile.automatedGainControlEnabled) {
                    normalizationOffset - 1.5f
                } else {
                    normalizationOffset
                }
            } else if (profile.automatedGainControlEnabled) {
                // Automated Gain Control dynamic leveling
                val totalGainOffset = profile.toBandArray().map { it.coerceAtLeast(0f) }.sum() +
                        (profile.bassBoost / 1000f) * 10f +
                        (if (profile.bassTunerPostGain > 0) profile.bassTunerPostGain else 0f)
                
                // Target a comfortable ceiling representing consistent loudness
                // High intense settings get more attenuation, lower settings get less
                -(totalGainOffset / 1.8f).coerceIn(1.5f, 18f)
            } else if (profile.autoAttenuationEnabled) {
                val maxBoost = profile.toBandArray().maxOrNull()?.coerceAtLeast(0f) ?: 0f
                val bassAtten = (profile.bassBoost / 1000f) * 6.0f
                val btBoost = if (profile.bassTunerPostGain > 0) profile.bassTunerPostGain else 0f
                -((maxBoost + bassAtten + btBoost) / 1.5f).coerceAtMost(15f)
            } else {
                profile.manualAttenuationDb
            }

            var leftBalanceWeight = 0f
            var rightBalanceWeight = 0f
            if (profile.channelBalance < 0f) {
                rightBalanceWeight = profile.channelBalance * 10f
            } else if (profile.channelBalance > 0f) {
                leftBalanceWeight = -profile.channelBalance * 10f
            }

            val boosterBoost = if (soundBoosterEnabled) soundBoosterGainDb else 0f
            val finalLeftGain = attenuationDb + leftBalanceWeight + boosterBoost
            val finalRightGain = attenuationDb + rightBalanceWeight + boosterBoost

            val finalLimiterEnabled = profile.limiterEnabled || profile.automatedGainControlEnabled || profile.masterNormalizationEnabled || (soundBoosterEnabled && soundBoosterGainDb > 0f)

            val builder = DynamicsProcessing.Config.Builder(
                0, // preferredFrameDuration
                2, // Channel count
                false, 0, // pre-eq
                false, 0, // mbc
                false, 0, // post-eq
                finalLimiterEnabled // limiter
            )

            val attackTime = if (profile.masterNormalizationEnabled) {
                1.5f // Extremely fast response to prevent switching spikes
            } else if (profile.automatedGainControlEnabled) {
                2.0f
            } else {
                profile.limiterAttackMs
            }

            val releaseTime = if (profile.masterNormalizationEnabled) {
                80.0f // Smooth recovery for natural audio quality
            } else if (profile.automatedGainControlEnabled) {
                60.0f
            } else {
                profile.limiterReleaseMs
            }

            val ratio = if (profile.masterNormalizationEnabled) {
                5.0f // Assertive but musical compression ratio
            } else if (profile.automatedGainControlEnabled) {
                4.0f
            } else {
                profile.limiterRatio
            }

            val threshold = if (profile.masterNormalizationEnabled) {
                -5.0f // Protective threshold ceiling for presets
            } else if (profile.automatedGainControlEnabled) {
                -4.5f
            } else {
                profile.limiterThresholdDb
            }

            val config = builder.build()

            for (c in 0 until 2) {
                val gain = if (c == 0) finalLeftGain else finalRightGain
                val limiter = DynamicsProcessing.Limiter(
                    finalLimiterEnabled,
                    finalLimiterEnabled,
                    0, // linkGroup
                    attackTime,
                    releaseTime,
                    ratio,
                    threshold,
                    gain
                )
                config.setLimiterByChannelIndex(c, limiter)
            }
            return config
        } catch (e: Exception) {
            return null
        }
    }

    class SessionEffects(val sessionId: Int) {
        var equalizer: Equalizer? = null
        var bassBoost: BassBoost? = null
        var virtualizer: Virtualizer? = null
        var reverb: EnvironmentalReverb? = null
        var dynamicsProcessing: DynamicsProcessing? = null

        init {
            try {
                equalizer = Equalizer(0, sessionId).apply { enabled = true }
            } catch (e: Exception) {
                Log.e("SessionEffects", "Could not initialize Equalizer on session $sessionId")
                AudioEffectEngine.getInstance().incrementGuardianInterventions()
            }
            try {
                bassBoost = BassBoost(0, sessionId).apply { enabled = true }
            } catch (e: Exception) {
                Log.e("SessionEffects", "Could not initialize BassBoost on session $sessionId")
                AudioEffectEngine.getInstance().incrementGuardianInterventions()
            }
            try {
                virtualizer = Virtualizer(0, sessionId).apply { enabled = true }
            } catch (e: Exception) {
                Log.e("SessionEffects", "Could not initialize Virtualizer on session $sessionId")
                AudioEffectEngine.getInstance().incrementGuardianInterventions()
            }
            try {
                reverb = EnvironmentalReverb(0, sessionId)
            } catch (e: Exception) {
                Log.e("SessionEffects", "Could not initialize EnvironmentalReverb on session $sessionId")
                AudioEffectEngine.getInstance().incrementGuardianInterventions()
            }
            recreateDynamics(EqProfile.FLAT)
        }

        fun recreateDynamics(profile: EqProfile) {
            try {
                dynamicsProcessing?.release()
                dynamicsProcessing = null
 
                val config = AudioEffectEngine.getInstance().buildDynamicsConfig(profile) ?: return
                dynamicsProcessing = DynamicsProcessing(0, sessionId, config).apply { enabled = true }
            } catch (e: Exception) {
                Log.e("SessionEffects", "Could not initialize DynamicsProcessing on session $sessionId: ${e.message}")
                AudioEffectEngine.getInstance().incrementGuardianInterventions()
            }
        }

        fun release() {
            try { equalizer?.release() } catch (e: Exception) {}
            try { bassBoost?.release() } catch (e: Exception) {}
            try { virtualizer?.release() } catch (e: Exception) {}
            try { reverb?.release() } catch (e: Exception) {}
            try { dynamicsProcessing?.release() } catch (e: Exception) {}
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
