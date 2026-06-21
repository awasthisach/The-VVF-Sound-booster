package com.example

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioEffectEngine
import com.example.audio.AudioService
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Launch background volume service
        try {
            val serviceIntent = Intent(this, AudioService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed starting AudioService: ${e.message}")
        }

        setContent {
            MyApplicationTheme(darkTheme = true, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF121118) // Rich cosmic dark background
                ) {
                    BoosterMainScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoosterMainScreen() {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val audioEngine = remember { AudioEffectEngine.getInstance() }
    val prefs = remember { context.getSharedPreferences("booster_prefs", Context.MODE_PRIVATE) }

    // Read stored variables or set modern defaults
    var isEnabled by remember { mutableStateOf(prefs.getBoolean("enabled", true)) }
    var masterBoost by remember { mutableStateOf(prefs.getFloat("master_boost", 25f)) } // 0-100%
    var bassBoost by remember { mutableStateOf(prefs.getFloat("bass_boost", 40f)) } // 0-100%
    var vocalBoost by remember { mutableStateOf(prefs.getFloat("vocal_boost", 50f)) } // 0-100%
    
    // Equalizer-specific bass boost targeting < 250Hz
    var eqBassBoostEnabled by remember { mutableStateOf(prefs.getBoolean("eq_bass_boost_enabled", true)) }
    var eqBassBoostLevelPercent by remember { mutableStateOf(prefs.getFloat("eq_bass_boost_level", 60f)) }

    // Read current system volume state
    val maxSysVolume = remember { val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC); if (max <= 0) 15 else max }
    var currentSysVolume by remember { mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) }

    // Dialogue State for Hearing Alert
    var showHighBoostAlert by remember { mutableStateOf(false) }
    var targetPendingBoostLevel by remember { mutableStateOf(0f) }

    // Pulse animation helper state to represent working dsp engine
    var animatePulse by remember { mutableStateOf(1f) }

    // Keep system volume updated when changed externally
    LaunchedEffect(Unit) {
        audioEngine.boosterEnabled = isEnabled
        audioEngine.masterVolumeBoostPercent = masterBoost
        audioEngine.bassBoostPercent = bassBoost
        audioEngine.vocalBoostPercent = vocalBoost
        audioEngine.eqBassBoostEnabled = eqBassBoostEnabled
        audioEngine.eqBassBoostLevelPercent = eqBassBoostLevelPercent
        audioEngine.applyAllCurrentStyles()

        while(true) {
            currentSysVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            delay(1000)
        }
    }

    // Interactive Pulsing Effect for working engine status
    if (isEnabled && masterBoost > 0f) {
        LaunchedEffect(Unit) {
            while (true) {
                animatePulse = 1.15f
                delay(800)
                animatePulse = 1.0f
                delay(800)
            }
        }
    } else {
        animatePulse = 1.0f
    }

    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = Color(0xFF121118),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    if (isEnabled) Color(0xFF00E676) else Color(0xFFFF5252),
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "VOICE & BASS BOOSTER",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .background(Color(0xFF1F1D2C), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF33304B), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isEnabled) "ACTIVE ⚡" else "STANDBY 💤",
                            color = if (isEnabled) Color(0xFF00E676) else Color(0xFF9EA3B0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121118))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            
            // 1. Core Power Hub (Foreground status)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("core_power_hub"),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(
                    1.dp, 
                    if (isEnabled) Brush.linearGradient(listOf(Color(0xFF8A2BE2), Color(0xFFFF5E36)))
                    else Brush.linearGradient(listOf(Color(0xFF302E3A), Color(0xFF302E3A)))
                ),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1D2A))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "मुख्य स्विच (Power Switch)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isEnabled) "पार्श्व सेवा (Foreground Service) और ऑडियो प्रोसेसिंग सक्रिय है" else "सभी ऑडियो प्रसंस्करण अभी थमा हुआ है",
                            color = if (isEnabled) Color(0xFF9E84FF) else Color(0xFF9EA3B0),
                            fontSize = 12.sp
                        )
                    }
                    
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = {
                            isEnabled = it
                            prefs.edit().putBoolean("enabled", it).apply()
                            audioEngine.boosterEnabled = it
                            audioEngine.applyAllCurrentStyles()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF8A2BE2),
                            uncheckedThumbColor = Color(0xFF9EA3B0),
                            uncheckedTrackColor = Color(0xFF32303E)
                        ),
                        modifier = Modifier.testTag("master_booster_switch")
                    )
                }
            }

            // 2. Dual Side-by-Side Vertical Sliders (Software Volume Boost & Stream Volume Control)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF262435)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16151E))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "वर्टिकल वॉल्यूम कंट्रोल डेस्क (CONTROL DECK)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Slider A: Vertical Software-based Volume/Voice Booster (0% to 100%)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "LOUDEST BOOST",
                                color = if (masterBoost > 40f) Color(0xFFFF5252) else Color(0xFFFF9E00),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${masterBoost.toInt()}%",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            
                            Box(
                                modifier = Modifier
                                    .height(180.dp)
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Slider(
                                    value = masterBoost,
                                    onValueChange = { newVal ->
                                        if (newVal > 40f && masterBoost <= 40f) {
                                            targetPendingBoostLevel = newVal
                                            showHighBoostAlert = true
                                        } else {
                                            masterBoost = newVal
                                            prefs.edit().putFloat("master_boost", newVal).apply()
                                            if (isEnabled) {
                                                audioEngine.masterVolumeBoostPercent = newVal
                                                audioEngine.applyAllCurrentStyles()
                                            }
                                        }
                                    },
                                    valueRange = 0f..100f,
                                    enabled = isEnabled,
                                    colors = SliderDefaults.colors(
                                        thumbColor = if (masterBoost > 40f) Color(0xFFFF5252) else Color(0xFFFF9E00),
                                        activeTrackColor = if (masterBoost > 40f) Color(0xFFFF5252) else Color(0xFFFF9E00),
                                        inactiveTrackColor = Color(0xFF2B2939)
                                    ),
                                    modifier = Modifier
                                        .width(150.dp)
                                        .graphicsLayer {
                                            rotationZ = -90f
                                        }
                                        .testTag("loud_volume_slider")
                                )
                            }
                            
                            Text(
                                text = if (masterBoost > 40f) "⚠️ HIGH RISK" else "✅ SAFE LIMIT",
                                color = if (masterBoost > 40f) Color(0xFFFF5252) else Color(0xFF00E676),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Vertical Separator
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(Color(0xFF262435))
                        )

                        // Slider B: Vertical Stream Volume Control (0% to 100%)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "STREAM MUSIC",
                                color = Color(0xFFFFEE58),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${((currentSysVolume.toFloat() / maxSysVolume.toFloat()) * 100).toInt()}%",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            
                            Box(
                                modifier = Modifier
                                    .height(180.dp)
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Slider(
                                    value = currentSysVolume.toFloat(),
                                    onValueChange = { newVal ->
                                        currentSysVolume = newVal.toInt()
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVal.toInt(), 0)
                                    },
                                    valueRange = 0f..maxSysVolume.toFloat(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFFFEE58),
                                        activeTrackColor = Color(0xFFFFEE58),
                                        inactiveTrackColor = Color(0xFF2B2939)
                                    ),
                                    modifier = Modifier
                                        .width(150.dp)
                                        .graphicsLayer {
                                            rotationZ = -90f
                                        }
                                        .testTag("system_volume_slider")
                                )
                            }

                            Text(
                                text = "SYSTEM STREAM",
                                color = Color(0xFF9EA3B0),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // 3. Custom-configured Equalizer Bass Boost Switch specifically targeting < 250Hz lower ranges
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF262435)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B2A))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Hearing,
                                contentDescription = "Equalizer Bass",
                                tint = if (eqBassBoostEnabled) Color(0xFF00B0FF) else Color(0xFF9EA3B0),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "EQUALIZER BASS BOOST (< 250Hz)",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "विशेष रूप से कम आवृत्ति बैंड को सशक्त करें",
                                    color = Color(0xFF9EA3B0),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Switch(
                            checked = eqBassBoostEnabled,
                            onCheckedChange = {
                                eqBassBoostEnabled = it
                                prefs.edit().putBoolean("eq_bass_boost_enabled", it).apply()
                                if (isEnabled) {
                                    audioEngine.eqBassBoostEnabled = it
                                    audioEngine.applyAllCurrentStyles()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF00B0FF),
                                uncheckedThumbColor = Color(0xFF9EA3B0),
                                uncheckedTrackColor = Color(0xFF32303E)
                            )
                        )
                    }

                    if (eqBassBoostEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "निम्न आवृत्ति स्तर (EQ Low Band Magnitude)",
                                color = Color(0xFF80D8FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${eqBassBoostLevelPercent.toInt()}%",
                                color = Color(0xFF80D8FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Slider(
                            value = eqBassBoostLevelPercent,
                            onValueChange = { newVal ->
                                eqBassBoostLevelPercent = newVal
                                prefs.edit().putFloat("eq_bass_boost_level", newVal).apply()
                                if (isEnabled) {
                                    audioEngine.eqBassBoostLevelPercent = newVal
                                    audioEngine.applyAllCurrentStyles()
                                }
                            },
                            valueRange = 0f..100f,
                            enabled = isEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00B0FF),
                                activeTrackColor = Color(0xFF00B0FF),
                                inactiveTrackColor = Color(0xFF2B2939)
                            )
                        )
                    }
                }
            }

            // 4. Standard DSP Bass Boost (Punchy Bass Booster)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF262435)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16151E))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "DSP Bass",
                                tint = if (isEnabled) Color(0xFF8A2BE2) else Color(0xFF9EA3B0),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "PUNCHY BASS BOOSTER (Standard)",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "सिस्टम एकीकृत ड्राइवर बास इंजन",
                                    color = Color(0xFF9EA3B0),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        
                        Text(
                            text = "${bassBoost.toInt()}%",
                            color = if (isEnabled) Color(0xFFB388FF) else Color(0xFF9EA3B0),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Slider(
                        value = bassBoost,
                        onValueChange = { newVal ->
                            bassBoost = newVal
                            prefs.edit().putFloat("bass_boost", newVal).apply()
                            if (isEnabled) {
                                    audioEngine.bassBoostPercent = newVal
                                    audioEngine.applyAllCurrentStyles()
                            }
                        },
                        valueRange = 0f..100f,
                        enabled = isEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF8A2BE2),
                            activeTrackColor = Color(0xFF8A2BE2),
                            inactiveTrackColor = Color(0xFF2B2939)
                        ),
                        modifier = Modifier.testTag("bass_booster_slider")
                    )
                }
            }

            // 5. Motorola Moto G54 5G Best Audio Tuning Guides
            Text(
                text = "MOTOROLA G54 5G OPTIMAL TUNING",
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF8A2BE2),
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF321E53)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1425))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Hearing,
                            contentDescription = "Speaker Tip",
                            tint = Color(0xFFB388FF),
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "बेहतरीन साउंड क्वालिटी सेटअप गाइड (Motorola Guide)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tip 1: Best Codec LDAC for Motorola G54 5G
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF8A2BE2).copy(alpha = 0.2f), CircleShape)
                                .size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("1", color = Color(0xFFB388FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "बेस्ट ब्लूटूथ कोडेक - LDAC चालू करें",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Motorola G54 5G में MediaTek 7020 है जो LDAC और LHDC को सपोर्ट करता है। डेवलपर ऑप्शंस में जाके ब्लूटूथ कोडेक को 'LDAC' पे सेट करें; इससे आवाज़ बिना कंप्रेस हुए 990 kbps शुद्ध स्पष्टता में मिलेगी।",
                                color = Color(0xFFB8BCC6),
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    // Tip 2: Dolby Atmos + App Booster
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF8A2BE2).copy(alpha = 0.2f), CircleShape)
                                .size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("2", color = Color(0xFFB388FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Dolby Atmos / Moto Sound 'Voice' सेट करें",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "अपने Motorola फ़ोन की Moto Settings में Dolby Atmos ऐप खोलें और इसे 'Voice' या 'Podcast' मोड पर सेट करें। जब आप इसे हमारे EQ BASS BOOSTER के साथ मिलाएंगे, तो ऑडियो और भी ज्यादा स्पष्ट और भारी लगेगी।",
                                color = Color(0xFFB8BCC6),
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    // Tip 3: Troubleshooting
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF8A2BE2).copy(alpha = 0.2f), CircleShape)
                                .size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("3", color = Color(0xFFB388FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "सभी ऐप्स में तुरंत काम करवाने के लिए",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "अगर YouTube या Spotify चालू करने पर आवाज़ अचानक तेज न हो, तो नोटिफिकेशन बार में हमारे 'Voice & Bass Booster' सर्विस स्टेटस को देखें। यह पीछे चालू रहकर हर ऐप के साउंड सेशन से जुड़ता है।",
                                color = Color(0xFFB8BCC6),
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // High Boost Danger Alert Dialog (similar to Goodev volume warn slider threshold)
    if (showHighBoostAlert) {
        AlertDialog(
            onDismissRequest = { showHighBoostAlert = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Caution Alert",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "हाई वॉल्यूम चेतावनी (Hearing Safety)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Text(
                    text = "40% से अधिक बूस्ट स्तर आपके फ़ोन के स्पीकर को स्थायी नुकसान पहुंचा सकता है या कान की सुनने की क्षमता को धीमा कर सकता है। क्या आप सचमुच आगे बढ़ना चाहते हैं?",
                    color = Color(0xFFB8BCC6),
                    fontSize = 13.sp,
                    lineHeight = 17.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        masterBoost = targetPendingBoostLevel
                        prefs.edit().putFloat("master_boost", targetPendingBoostLevel).apply()
                        if (isEnabled) {
                            audioEngine.masterVolumeBoostPercent = targetPendingBoostLevel
                            audioEngine.applyAllCurrentStyles()
                        }
                        showHighBoostAlert = false
                    }
                ) {
                    Text("हाँ, स्वीकार है (Proceed)", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showHighBoostAlert = false
                    }
                ) {
                    Text("रद्द करें (Cancel)", color = Color.White)
                }
            },
            containerColor = Color(0xFF1F1D2C),
            shape = RoundedCornerShape(20.dp)
        )
    }
}
