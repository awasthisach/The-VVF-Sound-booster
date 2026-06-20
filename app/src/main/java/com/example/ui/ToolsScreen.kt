package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import com.example.data.EqProfile
import java.util.Locale

@Composable
fun ToolsScreen(viewModel: EqViewModel) {
    val currentProfile by viewModel.currentProfile.collectAsState()
    
    val isSoundBoosterEnabled by viewModel.isSoundBoosterEnabled.collectAsState()
    val soundBoosterGainDb by viewModel.soundBoosterGainDb.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F))
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Title
        Text(
            text = "ऑडियो टूल्स और प्रभाव (DSP)",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFE6E1E5),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "बास ट्यूनर, लिमिटर, और एटेन्यूएटर जैसी पेशेवर सेटिंग्स को नियंत्रित करें जो सीधे सिस्टम स्ट्रीम पर लागू होती हैं।",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF938F99),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Sound Booster Card (NEW HIGH POWER REPLACEMENT FOR DOLBY)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSoundBoosterEnabled) Color(0xFF2E1F1A) else Color(0xFF23222B)
            ),
            border = BorderStroke(1.dp, if (isSoundBoosterEnabled) Color(0xFFFF8F00) else Color(0xFF49454F))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Main Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Sound Booster",
                            tint = if (isSoundBoosterEnabled) Color(0xFFFFB300) else Color(0xFF938F99),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "साउंड बूस्टर (Volume Booster)",
                                color = if (isSoundBoosterEnabled) Color.White else Color(0xFFE6E1E5),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = if (isSoundBoosterEnabled) "अतिरिक्त लाउडनेस बूस्ट सक्षम है" else "बूस्टर निष्क्रिय है (0 dB)",
                                color = if (isSoundBoosterEnabled) Color(0xFFFFB300) else Color(0xFF938F99),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = isSoundBoosterEnabled,
                        onCheckedChange = { viewModel.setSoundBoosterEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF4E2600),
                            checkedTrackColor = Color(0xFFFFB300),
                            uncheckedThumbColor = Color(0xFF938F99),
                            uncheckedTrackColor = Color(0xFF2B2930)
                        ),
                        modifier = Modifier.testTag("sound_booster_switch")
                    )
                }

                AnimatedVisibility(visible = isSoundBoosterEnabled) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        // Horizontal divider line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFF49454F).copy(alpha = 0.5f))
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Protection/Warning indicator
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFF8F00).copy(alpha = 0.1f)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFFF8F00).copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Hearing,
                                    contentDescription = "Hearing Protect",
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "सुरक्षा चेतावनी: कान और स्पीकर हित में कृपया अधिक समय तक निरंतर अधिकतम बूस्ट स्तर पर न सुनें।",
                                    color = Color(0xFFFFE082),
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp
                                )
                            }
                        }

                        // Booster Gain Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "बूस्टर पावर लेवल (Booster Intensity)",
                                color = Color(0xFFE6E1E5),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val multiplier = 100 + (soundBoosterGainDb * 10).toInt()
                            Text(
                                text = String.format(Locale.US, "+%.1f dB (%d%%)", soundBoosterGainDb, multiplier),
                                color = Color(0xFFFFB300),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Slider(
                            value = soundBoosterGainDb,
                            onValueChange = { viewModel.setSoundBoosterGainDb(it) },
                            valueRange = 0f..15f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFFB300),
                                activeTrackColor = Color(0xFFFFB300),
                                inactiveTrackColor = Color(0xFF49454F)
                            ),
                            modifier = Modifier.testTag("sound_booster_slider")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("0 dB (सामान्य)", color = Color(0xFF938F99), fontSize = 10.sp)
                            Text("+7.5 dB (मीडियम)", color = Color(0xFF938F99), fontSize = 10.sp)
                            Text("+15 dB (सुपर बूस्ट!)", color = Color(0xFFFFB300), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 1. Classic Bass & Spatializer Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
            border = BorderStroke(1.dp, Color(0xFF49454F))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.SurroundSound, contentDescription = "Surround", tint = Color(0xFFD0BCFF))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "मूल बूस्टर (Main Boosters)", color = Color(0xFFE6E1E5), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bass Boost
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "बास बूस्ट (Bass Boost)", color = Color(0xFFE6E1E5), fontSize = 13.sp)
                    Text(text = String.format(Locale.US, "%.0f%%", currentProfile.bassBoost / 10f), color = Color(0xFFD0BCFF), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
                Slider(
                    value = currentProfile.bassBoost,
                    onValueChange = { viewModel.updateBassBoost(it) },
                    valueRange = 0f..1000f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFD0BCFF),
                        activeTrackColor = Color(0xFFD0BCFF),
                        inactiveTrackColor = Color(0xFF49454F)
                    ),
                    modifier = Modifier.testTag("bass_boost_slider")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Divider(color = Color(0xFF49454F).copy(alpha = 0.5f), thickness = 1.dp)

                Spacer(modifier = Modifier.height(12.dp))

                // Virtual Surround Sound Toggle & Strength
                val isVirtualSurroundEnabled = currentProfile.virtualizer > 0f
                var lastNonZeroVirtualizer by remember { mutableStateOf(600f) }
                if (currentProfile.virtualizer > 0f) {
                    lastNonZeroVirtualizer = currentProfile.virtualizer
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "वर्चुअल सराउंड साउंड (Virtual Surround)",
                            color = Color(0xFFE6E1E5),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "हेडफ़ोन के लिए 3D स्थानिक ऑडियो अनुभव सक्रिय करें",
                            color = Color(0xFF938F99),
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = isVirtualSurroundEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                viewModel.updateVirtualizer(lastNonZeroVirtualizer)
                            } else {
                                viewModel.updateVirtualizer(0f)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF4F378B),
                            checkedTrackColor = Color(0xFFD0BCFF)
                        ),
                        modifier = Modifier.testTag("virtual_surround_switch")
                    )
                }

                AnimatedVisibility(visible = isVirtualSurroundEnabled) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "स्थानिक तीव्रता (Spatial Intensity)", color = Color(0xFFE6E1E5), fontSize = 12.sp)
                            Text(
                                text = String.format(Locale.US, "%.0f%%", currentProfile.virtualizer / 10f),
                                color = Color(0xFFD0BCFF),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                        Slider(
                            value = currentProfile.virtualizer,
                            onValueChange = { viewModel.updateVirtualizer(it) },
                            valueRange = 100f..1000f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFD0BCFF),
                                activeTrackColor = Color(0xFFD0BCFF),
                                inactiveTrackColor = Color(0xFF49454F)
                            ),
                            modifier = Modifier.testTag("virtualizer_slider")
                        )
                    }
                }
            }
        }

        // 2. Headphone Reverb Preset Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
            border = BorderStroke(1.dp, Color(0xFF49454F))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.MusicNote, contentDescription = "Rooms Reverb", tint = Color(0xFFD0BCFF))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "गूँज / रीवरब (Environmental Reverb)", color = Color(0xFFE6E1E5), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                val reverbPresets = listOf("None (बंद)", "Small Room (कमरा)", "Medium Room (हॉल)", "Large Room (बड़ा कमरा)", "Medium Hall (बड़ा हॉल)", "Large Hall (थिएटर)", "Plate Reverb")
                val selectedPresetIdx = currentProfile.reverbPreset.coerceIn(0, 6)

                Text(text = "रीवरब प्रीसेट (Reverb Preset)", color = Color(0xFFE6E1E5), fontSize = 11.sp, modifier = Modifier.padding(bottom = 6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1C1B1F), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF49454F), RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    Text(
                        text = reverbPresets[selectedPresetIdx],
                        color = Color(0xFFE6E1E5),
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded }
                            .padding(14.dp)
                            .testTag("reverb_preset_text")
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color(0xFF2B2930))
                    ) {
                        reverbPresets.forEachIndexed { index, name ->
                            DropdownMenuItem(
                                text = { Text(text = name, color = Color(0xFFE6E1E5)) },
                                onClick = {
                                    viewModel.updateReverb(index)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reverb Intensity Slider
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "रीवरब तीव्रता (Reverb Intensity)", color = Color(0xFFE6E1E5), fontSize = 13.sp)
                    Text(text = String.format(Locale.US, "%.0f%%", currentProfile.reverbIntensity / 10f), color = Color(0xFFD0BCFF), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
                Slider(
                    value = currentProfile.reverbIntensity,
                    onValueChange = { viewModel.updateReverbIntensity(it) },
                    valueRange = 0f..1000f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFD0BCFF),
                        activeTrackColor = Color(0xFFD0BCFF),
                        inactiveTrackColor = Color(0xFF49454F)
                    ),
                    modifier = Modifier.testTag("reverb_intensity_slider")
                )
            }
        }

        // 3. Bass Tuner Card (Subwoofer simulation)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
            border = BorderStroke(1.dp, Color(0xFF49454F))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Speaker, contentDescription = "Bass Tuner", tint = Color(0xFFD0BCFF))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "बास ट्यूनर (Subwoofer Rumble Engine)", color = Color(0xFFE6E1E5), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "यह सब-वूफर जैसा रंबल/कंपन प्रदान करता है।",
                    fontSize = 11.sp,
                    color = Color(0xFF938F99),
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                // Bass Tuner Mode Selector
                val modes = listOf("Natural", "Transient Comp", "Sustain Comp")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    modes.forEachIndexed { idx, name ->
                        val isSelected = currentProfile.bassTunerMode == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFFD0BCFF) else Color(0xFF1C1B1F))
                                .border(1.dp, if (isSelected) Color(0xFFD0BCFF) else Color(0xFF49454F), RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.updateBassTuner(idx, currentProfile.bassTunerCutoff, currentProfile.bassTunerPostGain)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                color = if (isSelected) Color(0xFF381E72) else Color(0xFF938F99),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Cutoff Frequency
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "फ़्रीक्वेंसी कटऑफ़ (Cutoff Frequency)", color = Color(0xFFE6E1E5), fontSize = 13.sp)
                    Text(text = String.format(Locale.US, "%.0f Hz", currentProfile.bassTunerCutoff), color = Color(0xFFD0BCFF), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
                Slider(
                    value = currentProfile.bassTunerCutoff,
                    onValueChange = { viewModel.updateBassTuner(currentProfile.bassTunerMode, it, currentProfile.bassTunerPostGain) },
                    valueRange = 40f..180f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFD0BCFF),
                        activeTrackColor = Color(0xFFD0BCFF)
                    ),
                    modifier = Modifier.testTag("bass_tuner_cutoff_slider")
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Post Gain
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "ट्यूनर पोस्ट-गैन (Sub-Gain Booster)", color = Color(0xFFE6E1E5), fontSize = 13.sp)
                    Text(text = String.format(Locale.US, "%+.1f dB", currentProfile.bassTunerPostGain), color = Color(0xFFD0BCFF), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
                Slider(
                    value = currentProfile.bassTunerPostGain,
                    onValueChange = { viewModel.updateBassTuner(currentProfile.bassTunerMode, currentProfile.bassTunerCutoff, it) },
                    valueRange = -5f..12f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFD0BCFF),
                        activeTrackColor = Color(0xFFD0BCFF)
                    ),
                    modifier = Modifier.testTag("bass_tuner_gain_slider")
                )
            }
        }

        // 4. Limiter (Dynamics Processing API) Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
            border = BorderStroke(1.dp, Color(0xFF49454F))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Hearing, contentDescription = "Limiter", tint = Color(0xFFD0BCFF))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "डायनामिक लिमिटर (Limiter)", color = Color(0xFFE6E1E5), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Switch(
                        checked = currentProfile.limiterEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.updateLimiter(
                                enabled,
                                currentProfile.limiterThresholdDb,
                                currentProfile.limiterRatio,
                                currentProfile.limiterAttackMs,
                                currentProfile.limiterReleaseMs
                            )
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF381E72),
                            checkedTrackColor = Color(0xFFD0BCFF)
                        ),
                        modifier = Modifier.testTag("limiter_enabled_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                AnimatedVisibility(visible = currentProfile.limiterEnabled) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Threshold
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "थ्रेशोल्ड (Threshold Boundary)", color = Color(0xFFE6E1E5), fontSize = 12.sp)
                            Text(text = String.format(Locale.US, "%.1f dB", currentProfile.limiterThresholdDb), color = Color(0xFFD0BCFF), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                        Slider(
                            value = currentProfile.limiterThresholdDb,
                            onValueChange = {
                                viewModel.updateLimiter(
                                    currentProfile.limiterEnabled,
                                    it,
                                    currentProfile.limiterRatio,
                                    currentProfile.limiterAttackMs,
                                    currentProfile.limiterReleaseMs
                                )
                            },
                            valueRange = -18f..0f,
                            modifier = Modifier.testTag("limiter_threshold_slider")
                        )

                        // Ratio
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "अनुपात (Ratio Compression)", color = Color(0xFFE6E1E5), fontSize = 12.sp)
                            Text(text = String.format(Locale.US, "%.1f:1", currentProfile.limiterRatio), color = Color(0xFFD0BCFF), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                        Slider(
                            value = currentProfile.limiterRatio,
                            onValueChange = {
                                viewModel.updateLimiter(
                                    currentProfile.limiterEnabled,
                                    currentProfile.limiterThresholdDb,
                                    it,
                                    currentProfile.limiterAttackMs,
                                    currentProfile.limiterReleaseMs
                                )
                            },
                            valueRange = 1.0f..10.0f,
                            modifier = Modifier.testTag("limiter_ratio_slider")
                        )

                        // Attack Time
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "हमला समय (Attack Time)", color = Color(0xFFE6E1E5), fontSize = 12.sp)
                            Text(text = String.format(Locale.US, "%.1f ms", currentProfile.limiterAttackMs), color = Color(0xFFD0BCFF), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                        Slider(
                            value = currentProfile.limiterAttackMs,
                            onValueChange = {
                                viewModel.updateLimiter(
                                    currentProfile.limiterEnabled,
                                    currentProfile.limiterThresholdDb,
                                    currentProfile.limiterRatio,
                                    it,
                                    currentProfile.limiterReleaseMs
                                )
                            },
                            valueRange = 0.5f..15.0f,
                            modifier = Modifier.testTag("limiter_attack_slider")
                        )

                        // Release Time
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "रिलीज समय (Release Time)", color = Color(0xFFE6E1E5), fontSize = 12.sp)
                            Text(text = String.format(Locale.US, "%.0f ms", currentProfile.limiterReleaseMs), color = Color(0xFFD0BCFF), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                        Slider(
                            value = currentProfile.limiterReleaseMs,
                            onValueChange = {
                                viewModel.updateLimiter(
                                    currentProfile.limiterEnabled,
                                    currentProfile.limiterThresholdDb,
                                    currentProfile.limiterRatio,
                                    currentProfile.limiterAttackMs,
                                    it
                                )
                            },
                            valueRange = 10.0f..300.0f,
                            modifier = Modifier.testTag("limiter_release_slider")
                        )
                    }
                }
            }
        }

        // 5. Automated Gain Control (AGC) Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (currentProfile.automatedGainControlEnabled) Color(0xFF1E1C28) else Color(0xFF2B2930)
            ),
            border = BorderStroke(1.dp, if (currentProfile.automatedGainControlEnabled) Color(0xFF81C784) else Color(0xFF49454F))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Automated Gain Control",
                            tint = if (currentProfile.automatedGainControlEnabled) Color(0xFF81C784) else Color(0xFFD0BCFF)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ऑटोमैटिक गेन... (Automated Gain Control)",
                                color = Color(0xFFE6E1E5),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (currentProfile.automatedGainControlEnabled) "सक्रिय (Clipping से सुरक्षा)" else "निष्क्रिय",
                                color = if (currentProfile.automatedGainControlEnabled) Color(0xFF81C784) else Color(0xFF938F99),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = currentProfile.automatedGainControlEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.updateAutomatedGainControl(enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF1B5E20),
                            checkedTrackColor = Color(0xFF81C784)
                        ),
                        modifier = Modifier.testTag("agc_enabled_switch")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "यह फ़ीचर विभिन्न प्रीसेट्स या गानों के बीच स्विच करते समय अचानक तेज़ आवाज़ को दबाकर एक समान वॉल्यूम स्तर बनाए रखता है और हाई-इंटेंसिटी सेटिंग्स में ऑडियो क्लिपिंग (फ़टने) को होने से रोकता है।",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCABEFF),
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )
                
                if (currentProfile.automatedGainControlEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1B5E20).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF81C784).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "🛡️ गतिशील सुरक्षा मोड चालू है",
                                color = Color(0xFF81C784),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Text(
                                text = "• सुरक्षा थ्रेशोल्ड: -4.5 dB (सुरक्षित स्तर)\n• तीव्र प्रतिक्रिया समय: 2.0 ms (एंटी-क्लिपिंग)\n• यह आपके संगीत के अनुसार बुद्धिमानी से इनपुट हेडरूम को लाइव ट्यून करता है।",
                                color = Color(0xFFE2F3E3),
                                fontSize = 10.5.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // 6. Master Normalization Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (currentProfile.masterNormalizationEnabled) Color(0xFF1B2A2B) else Color(0xFF2B2930)
            ),
            border = BorderStroke(1.dp, if (currentProfile.masterNormalizationEnabled) Color(0xFF4DB6AC) else Color(0xFF49454F))
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
                            contentDescription = "Master Normalization",
                            tint = if (currentProfile.masterNormalizationEnabled) Color(0xFF4DB6AC) else Color(0xFFD0BCFF)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ट्रैक वॉल्यूम नॉर्मलाइजेशन (Volume Normalization)",
                                color = Color(0xFFE6E1E5),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (currentProfile.masterNormalizationEnabled) "सक्रिय (स्वचालित संगीत स्तर सुरक्षा चालू)" else "निष्क्रिय (गानों के बीच आवाज असंतुलित हो सकती है)",
                                color = if (currentProfile.masterNormalizationEnabled) Color(0xFF4DB6AC) else Color(0xFF938F99),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = currentProfile.masterNormalizationEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.updateMasterNormalization(enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF004D40),
                            checkedTrackColor = Color(0xFF4DB6AC)
                        ),
                        modifier = Modifier.testTag("master_normalization_switch")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "यह इंटेलिजेंट नॉर्मलाइजर विभिन्न गानों या म्यूजिक ट्रैक्स के बीच अचानक तेज आवाज के उतार-चढ़ाव (sudden volume spikes/jumps) को पूर्ण रूप से रोकता है। यह सभी EQ बैंड्स और बूस्टर्स का स्वचालित रूप से विश्लेषण कर पूरे सत्र के लिए सुसंगत और संतुलित आवाज स्तर प्रदान करता है।",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCABEFF),
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )
                
                if (currentProfile.masterNormalizationEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF004D40).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF4DB6AC).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            val maxBandBoost = currentProfile.toBandArray().maxOrNull()?.coerceAtLeast(0f) ?: 0f
                            val bassBoostDb = (currentProfile.bassBoost / 1000f) * 12.0f
                            val bassTunerDb = if (currentProfile.bassTunerPostGain > 0) currentProfile.bassTunerPostGain else 0f
                            val virtualizerDb = (currentProfile.virtualizer / 1000f) * 4.0f
                            val estimatedPeak = maxBandBoost + bassBoostDb + bassTunerDb + virtualizerDb
                            val normOffset = if (estimatedPeak > 0f) -(estimatedPeak * 0.70f).coerceIn(1.5f, 18.0f) else -0.5f

                            Text(
                                text = "🛡️ प्रीसेट-आधारित गेन सुरक्षा",
                                color = Color(0xFF4DB6AC),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "• कुल अनुमानित प्रीसेट ओवरहेड: +%.1f dB\n• स्वचालित गेन नॉर्मलाइजेशन: %.1f dB\n• तीव्र लिमिटर थ्रेशोल्ड: -5.0 dB (क्लिपिंग अवरोधक)\n• संगीत प्रतिक्रिया गति: 1.5 ms (स्मूथ ट्रांजिशन)",
                                    estimatedPeak,
                                    normOffset
                                ),
                                color = Color(0xFFE0F2F1),
                                fontSize = 10.5.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // 7. Digital Attenuation Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
            border = BorderStroke(1.dp, Color(0xFF49454F))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.VolumeDown, contentDescription = "Attenuation", tint = Color(0xFFD0BCFF))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "डिजिटल एटेन्यूएटर (Gain Headroom)", color = Color(0xFFE6E1E5), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "स्वचालित (Auto)", color = Color(0xFF938F99), fontSize = 11.sp, modifier = Modifier.padding(end = 6.dp))
                        Switch(
                            checked = currentProfile.autoAttenuationEnabled,
                            onCheckedChange = { auto ->
                                viewModel.updateAttenuation(auto, currentProfile.manualAttenuationDb)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF381E72),
                                checkedTrackColor = Color(0xFFD0BCFF)
                            ),
                            modifier = Modifier.testTag("attenuator_auto_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "सिस्टम में डिजिटल क्लिपिंग (आवाज फटने) को रोकने के लिए एटेन्यूएटर समग्र आउटपुट हेडरूम को कम रखता है।",
                    fontSize = 11.sp,
                    color = Color(0xFF938F99),
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                if (!currentProfile.autoAttenuationEnabled) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "मैन्युअल एटेन्यूएशन (Attenuation Db)", color = Color(0xFFE6E1E5), fontSize = 12.sp)
                        Text(text = String.format(Locale.US, "%.1f dB", currentProfile.manualAttenuationDb), color = Color(0xFFD0BCFF), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                    Slider(
                        value = currentProfile.manualAttenuationDb,
                        onValueChange = { viewModel.updateAttenuation(false, it) },
                        valueRange = -15f..0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFD0BCFF),
                            activeTrackColor = Color(0xFFD0BCFF)
                        ),
                        modifier = Modifier.testTag("manual_attenuation_slider")
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1C1B1F), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "स्वचालित नियंत्रण सक्रिय: EQ गेन बूस्ट स्तरों के अनुसार हेडरूम को अनुकूलित किया जा रहा है।",
                            color = Color(0xFFD0BCFF),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // 7. Channel Balance Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
            border = BorderStroke(1.dp, Color(0xFF49454F))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Tune, contentDescription = "Balance", tint = Color(0xFFD0BCFF))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "चैनल बैलेंस (Left / Right Balance)", color = Color(0xFFE6E1E5), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "L / R संतुलन", color = Color(0xFFE6E1E5), fontSize = 13.sp)
                    val locStr = when {
                        currentProfile.channelBalance < -0.05f -> "काफी बाएँ (" + String.format(Locale.US, "%.0f%%", -currentProfile.channelBalance * 100f) + ")"
                        currentProfile.channelBalance > 0.05f -> "काफी दाएँ (" + String.format(Locale.US, "%.0f%%", currentProfile.channelBalance * 100f) + ")"
                        else -> "पूर्ण केन्द्र (Center)"
                    }
                    Text(text = locStr, color = Color(0xFFD0BCFF), fontSize = 12.sp)
                }
                Slider(
                    value = currentProfile.channelBalance,
                    onValueChange = { viewModel.updateChannelBalance(it) },
                    valueRange = -1.0f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFD0BCFF),
                        activeTrackColor = Color(0xFFD0BCFF)
                    ),
                    modifier = Modifier.testTag("channel_balance_slider")
                )
            }
        }
    }
}
