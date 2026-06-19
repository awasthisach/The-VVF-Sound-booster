package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "eq_profiles")
data class EqProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isCustom: Boolean = true,
    val mediaType: String = "Music", // "Music", "Movie", "Gaming", "General"
    
    // EQ Bands (dB gains: -15.0 to +15.0)
    val band60Hz: Float = 0f,
    val band120Hz: Float = 0f,
    val band250Hz: Float = 0f,
    val band500Hz: Float = 0f,
    val band1kHz: Float = 0f,
    val band2kHz: Float = 0f,
    val band4kHz: Float = 0f,
    val band8kHz: Float = 0f,
    val band16kHz: Float = 0f,
    
    // Core Effects
    val bassBoost: Float = 0f, // 0 to 1000 % strength (milli-percent)
    val virtualizer: Float = 0f, // 0 to 1000 % strength (milli-percent)
    val reverbPreset: Int = 0, // 0 = None, 1 = Small Room, 2 = Medium Room, 3 = Large Room, 4 = Medium Hall, 5 = Large Hall, 6 = Plate
    val reverbIntensity: Float = 0f, // 0 to 1000 % custom dynamic intensity (milli-percent)
    
    // Equal Loudness
    val equalLoudnessEnabled: Boolean = false,
    val equalLoudnessThresholdDb: Float = -20f,
    
    // Bass Tuner (Custom DSP style)
    val bassTunerMode: Int = 0, // 0: Natural, 1: Transient Compressor, 2: Sustain Compressor
    val bassTunerCutoff: Float = 80f, // Hz
    val bassTunerPostGain: Float = 0f, // dB
    
    // Limiter (DynamicsProcessing)
    val limiterEnabled: Boolean = false,
    val limiterThresholdDb: Float = -3.0f,
    val limiterRatio: Float = 2.0f,
    val limiterAttackMs: Float = 5.0f,
    val limiterReleaseMs: Float = 50.0f,
    
    // Automated Gain Control
    val automatedGainControlEnabled: Boolean = false,
    
    // Master Normalization
    val masterNormalizationEnabled: Boolean = false,
    
    // Attenuation & Channel Balance
    val autoAttenuationEnabled: Boolean = true,
    val manualAttenuationDb: Float = 0f,
    val channelBalance: Float = 0f // -1.0 (Left) to 1.0 (Right)
) {
    fun toBandArray(): FloatArray {
        return floatArrayOf(
            band60Hz, band120Hz, band250Hz, band500Hz,
            band1kHz, band2kHz, band4kHz, band8kHz, band16kHz
        )
    }

    companion object {
        val FLAT = EqProfile(
            id = 1,
            name = "Flat / Stock",
            isCustom = false,
            mediaType = "General"
        )
        val BASS_BOOST = EqProfile(
            id = 2,
            name = "Dhar Dhamaka (Bass Boost)",
            isCustom = false,
            mediaType = "Music",
            band60Hz = 8f,
            band120Hz = 6f,
            band250Hz = 3f,
            bassBoost = 650f
        )
        val VOCAL_BOOST = EqProfile(
            id = 3,
            name = "Vocal Clear (Sur-Sangeet)",
            isCustom = false,
            mediaType = "General",
            band500Hz = 2f,
            band1kHz = 5f,
            band2kHz = 4f,
            band4kHz = 2f
        )
        val TREBLE_BOOST = EqProfile(
            id = 4,
            name = "Treble Boost (Jhnkar)",
            isCustom = false,
            mediaType = "Music",
            band4kHz = 3f,
            band8kHz = 6f,
            band16kHz = 8f
        )
        val CLASSIC = EqProfile(
            id = 5,
            name = "Classic Oldies",
            isCustom = false,
            mediaType = "Music",
            band60Hz = 4f,
            band120Hz = 2f,
            band250Hz = 1f,
            band1kHz = -1f,
            band2kHz = -1f,
            band4kHz = 1f,
            band8kHz = 2f,
            band16kHz = 3f
        )
        val ROCK_METAL = EqProfile(
            id = 6,
            name = "Rock & Metal (Josh)",
            isCustom = false,
            mediaType = "Music",
            band60Hz = 6f,
            band120Hz = 4f,
            band250Hz = -2f,
            band500Hz = -1f,
            band1kHz = 1f,
            band2kHz = 3f,
            band4kHz = 4f,
            band8kHz = 5f,
            band16kHz = 5f
        )
        val CINEMA_SURROUND = EqProfile(
            id = 7,
            name = "Cinema Atmos Surround",
            isCustom = false,
            mediaType = "Movie",
            band60Hz = 7f,
            band120Hz = 5f,
            band1kHz = 2f,
            band2kHz = 3f,
            band4kHz = 1f,
            bassBoost = 700f,
            virtualizer = 800f,
            reverbPreset = 4 // Medium Hall
        )
        val FPS_FOOTSTEPS = EqProfile(
            id = 8,
            name = "FPS Footstep Booster",
            isCustom = false,
            mediaType = "Gaming",
            band120Hz = -4f, // suppress rumble
            band250Hz = -2f,
            band1kHz = 1f,
            band2kHz = 3f,
            band4kHz = 6f, // crisp footstep treble
            band8kHz = 5f,
            band16kHz = 4f,
            virtualizer = 900f // max 3D spacing
        )
        val INDIAN_CLASSICAL = EqProfile(
            id = 9,
            name = "Indian Classical (Sitar & Sur)",
            isCustom = false,
            mediaType = "Music",
            band60Hz = 2f,
            band120Hz = 4f,
            band250Hz = 1f,
            band500Hz = 0f,
            band1kHz = 3f,
            band2kHz = 5f,
            band4kHz = 4f,
            band8kHz = 3f,
            band16kHz = 2f
        )
        val SUFI_GHAZAL = EqProfile(
            id = 10,
            name = "Sufi & Ghazal Warmth",
            isCustom = false,
            mediaType = "Music",
            band60Hz = 3f,
            band120Hz = 5f,
            band250Hz = 3f,
            band500Hz = 2f,
            band1kHz = 4f,
            band2kHz = 3f,
            band4kHz = 2f,
            band8kHz = 1f,
            band16kHz = 2f
        )
        val BOLLYWOOD_BEATS = EqProfile(
            id = 11,
            name = "Bollywood Dhol Dhamaaka",
            isCustom = false,
            mediaType = "Music",
            band60Hz = 9f,
            band120Hz = 7f,
            band250Hz = 4f,
            band1kHz = 1f,
            band2kHz = 2f,
            band4kHz = 3f,
            band8kHz = 5f,
            band16kHz = 4f,
            bassBoost = 700f
        )
        val BHAKTI_DEVOTIONAL = EqProfile(
            id = 12,
            name = "Devotional (Bhajan & Aarti)",
            isCustom = false,
            mediaType = "Music",
            band60Hz = 1f,
            band120Hz = 2f,
            band500Hz = 1f,
            band1kHz = 3f,
            band2kHz = 4f,
            band4kHz = 6f,
            band8kHz = 5f,
            band16kHz = 4f,
            virtualizer = 300f,
            reverbPreset = 2 // Medium Room
        )
    }
}

@Entity(tableName = "device_mappings")
data class DeviceMapping(
    @PrimaryKey val deviceName: String,
    val profileId: Int
)

data class AutoEqHeadphone(
    val brand: String,
    val model: String,
    val gains: List<Float> // Corresponding to 9 bands: 60Hz, 120Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz
) {
    val fullName: String get() = "$brand $model"
}

data class ServiceResourceStats(
    val usedMemoryMb: Long,
    val maxMemoryMb: Long,
    val memoryUsagePercent: Float,
    val cpuUsagePercent: Float,
    val isEngineHealthy: Boolean,
    val activeSessionCount: Int,
    val startupTimeMs: Long,
    val guardianActionCount: Int
)

