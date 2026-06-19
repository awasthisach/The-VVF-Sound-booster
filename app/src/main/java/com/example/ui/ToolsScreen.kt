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
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeDown
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
    
    val isDolbyEnabled by viewModel.isDolbyEnabled.collectAsState()
    val dolbyMode by viewModel.dolbyMode.collectAsState()
    val dolbySurroundStrength by viewModel.dolbySurroundStrength.collectAsState()
    val dolbyDialogueEnhancer by viewModel.dolbyDialogueEnhancer.collectAsState()
    val dolbyVolumeLeveler by viewModel.dolbyVolumeLeveler.collectAsState()

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

        // Dolby Atmos Soundstage Control Engine Card (TOP RANKED FEATURE)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDolbyEnabled) Color(0xFF1E1C28) else Color(0xFF23222B)
            ),
            border = BorderStroke(1.dp, if (isDolbyEnabled) Color(0xFF9E86FF) else Color(0xFF49454F))
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
                            imageVector = Icons.Default.SurroundSound,
                            contentDescription = "Dolby Atmos",
                            tint = if (isDolbyEnabled) Color(0xFFB4A0FF) else Color(0xFF938F99),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Dolby Atmos® सराउंड साउंड",
                                color = if (isDolbyEnabled) Color.White else Color(0xFFE6E1E5),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = if (isDolbyEnabled) "3D साउंडस्टेज सक्रिय है" else "सिमुलेशन बंद है",
                                color = if (isDolbyEnabled) Color(0xFFB4A0FF) else Color(0xFF938F99),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = isDolbyEnabled,
                        onCheckedChange = { viewModel.setDolbyEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF381E72),
                            checkedTrackColor = Color(0xFFB4A0FF),
                            uncheckedThumbColor = Color(0xFF938F99),
                            uncheckedTrackColor = Color(0xFF2B2930)
                        ),
                        modifier = Modifier.testTag("dolby_atmos_switch")
                    )
                }

                AnimatedVisibility(visible = isDolbyEnabled) {
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

                        // Dolby Mode selectors (Movie, Music, Game, Voice)
                        Text(
                            text = "एटमॉस साउंड प्रोफाइल (Mode Select)",
                            color = Color(0xFFE6E1E5),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        val dolbyModes = listOf("सिनेमा (Movie)", "संगीत (Music)", "खेल (Game)", "ध्वनि (Voice)")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            dolbyModes.forEachIndexed { index, modeName ->
                                val isSelected = dolbyMode == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFFB4A0FF) else Color(0xFF1C1B1F))
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFFB4A0FF) else Color(0xFF49454F).copy(alpha = 0.5f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.setDolbyMode(index) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = modeName,
                                        color = if (isSelected) Color(0xFF24005A) else Color(0xFFE6E1E5),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Dolby Spatial Width / Surround Strength Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "थ्री-डी साउंडस्टेज विड्थ (Surround Strength)",
                                color = Color(0xFFE6E1E5),
                                fontSize = 12.sp
                            )
                            Text(
                                text = String.format(Locale.US, "%.0f%%", dolbySurroundStrength / 10f),
                                color = Color(0xFFB4A0FF),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = dolbySurroundStrength,
                            onValueChange = { viewModel.setDolbySurroundStrength(it) },
                            valueRange = 0f..1000f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFB4A0FF),
                                activeTrackColor = Color(0xFFB4A0FF),
                                inactiveTrackColor = Color(0xFF49454F)
                            ),
                            modifier = Modifier.testTag("dolby_surround_slider")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Dialog Enhancer (Dialogue Enhancement slider)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "वार्तालाप स्पष्टता (Dialogue Enhancer)",
                                color = Color(0xFFE6E1E5),
                                fontSize = 12.sp
                            )
                            Text(
                                text = String.format(Locale.US, "%.0f%%", dolbyDialogueEnhancer / 10f),
                                color = Color(0xFFB4A0FF),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = dolbyDialogueEnhancer,
                            onValueChange = { viewModel.setDolbyDialogueEnhancer(it) },
                            valueRange = 0f..1000f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFB4A0FF),
                                activeTrackColor = Color(0xFFB4A0FF),
                                inactiveTrackColor = Color(0xFF49454F)
                            ),
                            modifier = Modifier.testTag("dolby_dialogue_slider")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Smart Volume Leveler switch row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1C1B1F), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "स्मार्ट वॉल्यूम लेवलर (Smart Volume)",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "थिएटर जैसी स्वतः आवाज सुसंगत रखेगा",
                                    color = Color(0xFF938F99),
                                    fontSize = 10.sp
                                )
                            }
                            Switch(
                                checked = dolbyVolumeLeveler,
                                onCheckedChange = { viewModel.setDolbyVolumeLeveler(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF381E72),
                                    checkedTrackColor = Color(0xFFB4A0FF)
                                ),
                                modifier = Modifier.testTag("dolby_leveler_switch")
                            )
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

                Spacer(modifier = Modifier.height(8.dp))

                // Virtualizer
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "वर्चुअलाइज़र (Virtualizer - Stereo Wide)", color = Color(0xFFE6E1E5), fontSize = 13.sp)
                    Text(text = String.format(Locale.US, "%.0f%%", currentProfile.virtualizer / 10f), color = Color(0xFFD0BCFF), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
                Slider(
                    value = currentProfile.virtualizer,
                    onValueChange = { viewModel.updateVirtualizer(it) },
                    valueRange = 0f..1000f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFD0BCFF),
                        activeTrackColor = Color(0xFFD0BCFF),
                        inactiveTrackColor = Color(0xFF49454F)
                    ),
                    modifier = Modifier.testTag("virtualizer_slider")
                )
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
                var selectedPresetIdx by remember { mutableStateOf(currentProfile.reverbPreset) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1C1B1F), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF49454F), RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    Text(
                        text = reverbPresets[selectedPresetIdx.coerceIn(0, 6)],
                        color = Color(0xFFE6E1E5),
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded }
                            .padding(14.dp)
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
                                    selectedPresetIdx = index
                                    viewModel.updateReverb(index)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
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

        // 5. Digital Attenuation Card
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
                        Icon(imageVector = Icons.Default.VolumeDown, contentDescription = "Attenuation", tint = Color(0xFFD0BCFF))
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

        // 6. Channel Balance Card
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
