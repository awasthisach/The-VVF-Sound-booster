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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
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

    // In-App Test Player State
    var isTestPlaying by remember { mutableStateOf(false) }
    var testPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var selectedTrackIndex by remember { mutableStateOf(0) }
    val testTracks = remember {
        listOf(
            "ऑफ़लाइन रिंगटोन (Local Tone Loop)" to "offline_ringtone",
            "क्लाउड हेवी बास बीट्स (Cloud Bass)" to "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            "क्लाउड वोकल पॉडकास्ट (Voice Anchor)" to "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"
        )
    }

    val playTestTrack = remember(selectedTrackIndex, isTestPlaying) {
        {
            testPlayer?.let {
                try {
                    if (it.isPlaying) {
                        it.stop()
                    }
                    it.release()
                } catch (e: Exception) {}
            }
            testPlayer = null

            if (isTestPlaying) {
                try {
                    val player = MediaPlayer()
                    val currentTrack = testTracks[selectedTrackIndex]
                    if (currentTrack.second == "offline_ringtone") {
                        val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE) 
                            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        player.setDataSource(context, alertUri)
                    } else {
                        player.setDataSource(context, Uri.parse(currentTrack.second))
                    }
                    player.isLooping = true
                    player.prepareAsync()
                    player.setOnPreparedListener { mp ->
                        if (isTestPlaying) {
                            audioEngine.registerSession(mp.audioSessionId, "VVF Test Player")
                            mp.start()
                        }
                    }
                    testPlayer = player
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Failed to load/play test audio: ${e.message}")
                }
            }
        }
    }

    val stopTestTrack = remember {
        {
            testPlayer?.let {
                try {
                    if (it.isPlaying) {
                        it.stop()
                    }
                    audioEngine.unregisterSession(it.audioSessionId)
                    it.release()
                } catch (e: Exception) {}
            }
            testPlayer = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            testPlayer?.let {
                try {
                    it.stop()
                    it.release()
                } catch (e: Exception) {}
            }
        }
    }

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
                            modifier = Modifier.weight(1f).fillMaxHeight()
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
                                    .weight(1f)
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CustomVerticalSlider(
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
                                    activeBrush = Brush.verticalGradient(
                                        listOf(Color(0xFF8A2BE2), Color(0xFFFF9E00))
                                    ),
                                    inactiveColor = Color(0xFF2B2939),
                                    modifier = Modifier.testTag("loud_volume_slider")
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
                                .padding(vertical = 16.dp)
                                .width(1.dp)
                                .background(Color(0xFF262435))
                        )

                        // Slider B: Vertical Stream Volume Control (0% to 100%)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f).fillMaxHeight()
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
                                    .weight(1f)
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CustomVerticalSlider(
                                    value = currentSysVolume.toFloat(),
                                    onValueChange = { newVal ->
                                        currentSysVolume = newVal.toInt()
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVal.toInt(), 0)
                                    },
                                    valueRange = 0f..maxSysVolume.toFloat(),
                                    enabled = true,
                                    activeBrush = Brush.verticalGradient(
                                        listOf(Color(0xFFFFEE58), Color(0xFFFF9E00))
                                    ),
                                    inactiveColor = Color(0xFF2B2939),
                                    modifier = Modifier.testTag("system_volume_slider")
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

            // 2.5 In-App Audio Test Player (परीक्षण प्लेयर)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF3B2961)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1D142E))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🔊 बूस्टर परीक्षण ध्वनि (IN-APP TEST ENGINE)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "आवाज़ में भारीपन और बदलाव को तुरंत सुनने के लिए प्ले करें",
                                color = Color(0xFFBCAAA4),
                                fontSize = 11.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                isTestPlaying = !isTestPlaying
                                if (isTestPlaying) {
                                    playTestTrack()
                                } else {
                                    stopTestTrack()
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (isTestPlaying) Color(0xFFFF5252) else Color(0xFF00E676),
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = if (isTestPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                contentDescription = if (isTestPlaying) "Stop Test" else "Play Test",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "परीक्षण ट्रैक चुनें (Select Test Track):",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        testTracks.forEachIndexed { index, track ->
                            val isSelected = selectedTrackIndex == index
                            Button(
                                onClick = {
                                    selectedTrackIndex = index
                                    if (isTestPlaying) {
                                        playTestTrack()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) Color(0xFF8A2BE2) else Color(0xFF261D3A),
                                    contentColor = if (isSelected) Color.White else Color(0xFFBCAAA4)
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Text(
                                    text = track.first.split(" ")[0], // Use first short word
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Music Wave Visualizer simulation when test is active
                    if (isTestPlaying) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                                .background(Color(0xFF130B22), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF281C3D), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "बज रहा है... (PLAYING & ENHANCING)",
                                color = Color(0xFF00E676),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(5) { barIndex ->
                                    val barHeight = remember { mutableStateOf(5f) }
                                    LaunchedEffect(isTestPlaying) {
                                        while (isTestPlaying) {
                                            barHeight.value = kotlin.random.Random.nextFloat() * 15f + 10f
                                            delay(150L + (barIndex * 30))
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(barHeight.value.dp)
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color(0xFF00E676), Color(0xFF9E84FF))
                                                ),
                                                RoundedCornerShape(1.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2.8 Dynamic Sync & Troubleshooting Guide (साउंड सिंक एवं सहायता गाइड)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, Color(0xFF4A148C).copy(alpha = 0.5f)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF160E25))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Sync Info",
                            tint = Color(0xFFA877FF),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🛠️ अन्य ऐप्स से आवाज़ कैसे बढ़ाएं? (SYNC GUIDE)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "• ऑटो-सक्रिय (Auto-Booster):\nहमारी AI बैकग्राउंड सेवा YouTube, Spotify और अन्य मीडिया खिलाड़ियों को अपने आप पकड़ लेती है।",
                        color = Color(0xFFE1BEE7),
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "• यदि आवाज़ न बढ़े (Troubleshoot):\nसंगीत ऐप (उदा. Spotify) की Settings में जाएं और 'Equalizer' विकल्प पर टैप करें। यह तुरंत इस बूस्टर टनल को सक्रिय कर देगा!",
                        color = Color(0xFFE1BEE7),
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Button(
                        onClick = {
                            try {
                                audioEngine.setLegacyMode(false)
                                audioEngine.setLegacyMode(true)
                                audioEngine.applyAllCurrentStyles()
                                android.widget.Toast.makeText(
                                    context, 
                                    "साउंड टनल रीफ्रेश सफल! सभी ऐप्स बूस्टेड हैं। ⚡", 
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "Failed to refresh sound tunnel: ${e.message}")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7B1FA2),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                    ) {
                        Text(
                            text = "🔄 कनेक्शन रीफ्रेश करें (RESET SOUND TUNNEL)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
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

@Composable
fun CustomVerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
    enabled: Boolean = true,
    activeBrush: Brush,
    inactiveColor: Color,
    modifier: Modifier = Modifier
) {
    val rangeSize = valueRange.endInclusive - valueRange.start

    BoxWithConstraints(
        modifier = modifier
            .width(64.dp)
            .fillMaxHeight()
            .pointerInput(enabled, value) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val fraction = ((size.height - offset.y) / size.height).coerceIn(0f, 1f)
                    val newValue = valueRange.start + fraction * rangeSize
                    onValueChange(newValue)
                }
            }
            .pointerInput(enabled, value) {
                if (!enabled) return@pointerInput
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val fractionDelta = -dragAmount.y / size.height
                    val valueDelta = fractionDelta * rangeSize
                    val newValue = (value + valueDelta).coerceIn(valueRange.start, valueRange.endInclusive)
                    onValueChange(newValue)
                }
            }
    ) {
        val totalHeight = constraints.maxHeight.toFloat()
        val totalWidth = constraints.maxWidth.toFloat()

        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackWidth = 14.dp.toPx()
            val thumbRadius = 13.dp.toPx()

            val xCenter = totalWidth / 2f

            // Draw base track (inactive)
            drawRoundRect(
                color = if (enabled) inactiveColor else inactiveColor.copy(alpha = 0.5f),
                topLeft = Offset(xCenter - trackWidth / 2f, thumbRadius),
                size = androidx.compose.ui.geometry.Size(trackWidth, totalHeight - 2 * thumbRadius),
                cornerRadius = CornerRadius(trackWidth / 2f, trackWidth / 2f)
            )

            // Calculate current slider placement fraction
            val currentFraction = ((value - valueRange.start) / rangeSize).coerceIn(0f, 1f)
            val thumbY = totalHeight - thumbRadius - currentFraction * (totalHeight - 2 * thumbRadius)

            // Draw active (filled) portion with smooth gradient brush
            drawRoundRect(
                brush = if (enabled) activeBrush else Brush.linearGradient(listOf(Color(0xFF4C4C4C), Color(0xFF4C4C4C))),
                topLeft = Offset(xCenter - trackWidth / 2f, thumbY),
                size = androidx.compose.ui.geometry.Size(trackWidth, totalHeight - thumbRadius - thumbY),
                cornerRadius = CornerRadius(trackWidth / 2f, trackWidth / 2f)
            )

            // Draw outer thumb highlight aura
            drawCircle(
                color = if (enabled) Color.White.copy(alpha = 0.12f) else Color.Transparent,
                radius = thumbRadius + 5.dp.toPx(),
                center = Offset(xCenter, thumbY)
            )

            // Draw inner solid volume indicator thumb
            drawCircle(
                brush = if (enabled) activeBrush else Brush.linearGradient(listOf(Color(0xFF6E6E6E), Color(0xFF6E6E6E))),
                radius = thumbRadius,
                center = Offset(xCenter, thumbY)
            )

            // Draw core white anchor dot
            drawCircle(
                color = if (enabled) Color.White else Color(0xFF9EA3B0),
                radius = thumbRadius / 2.5f,
                center = Offset(xCenter, thumbY)
            )
        }
    }
}
