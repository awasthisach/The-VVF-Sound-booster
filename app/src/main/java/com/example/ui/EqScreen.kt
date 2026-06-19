package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeviceUnknown
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EqProfile
import kotlinx.coroutines.launch
import java.util.Locale

data class IndianPresetMetadata(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val leadingIconColor: Color,
    val tagBgColor: Color,
    val tagTextColor: Color,
    val tagLabel: String,
    val tagEmoji: String
)

fun getIndianMetadata(profileId: Int, name: String): IndianPresetMetadata? {
    return when (profileId) {
        9 -> IndianPresetMetadata(
            icon = Icons.Default.MusicNote,
            leadingIconColor = Color(0xFFFF9800), // Saffron Orange
            tagBgColor = Color(0xFFFF9800).copy(alpha = 0.15f),
            tagTextColor = Color(0xFFFFB74D),
            tagLabel = "CLASSICAL",
            tagEmoji = "🪕"
        )
        10 -> IndianPresetMetadata(
            icon = Icons.Default.MusicNote,
            leadingIconColor = Color(0xFFE91E63), // Sufi Pink/Crimson
            tagBgColor = Color(0xFFE91E63).copy(alpha = 0.15f),
            tagTextColor = Color(0xFFF48FB1),
            tagLabel = "SUFI",
            tagEmoji = "🎤"
        )
        11 -> IndianPresetMetadata(
            icon = Icons.Default.MusicNote,
            leadingIconColor = Color(0xFF9C27B0), // Bollywood Purple/Magenta
            tagBgColor = Color(0xFF9C27B0).copy(alpha = 0.15f),
            tagTextColor = Color(0xFFCE93D8),
            tagLabel = "BOLLYWOOD",
            tagEmoji = "🥁"
        )
        12 -> IndianPresetMetadata(
            icon = Icons.Default.MusicNote,
            leadingIconColor = Color(0xFF00E676), // Bhakti Green
            tagBgColor = Color(0xFF00E676).copy(alpha = 0.15f),
            tagTextColor = Color(0xFF69F0AE),
            tagLabel = "BHAKTI",
            tagEmoji = "🪔"
        )
        else -> {
            val lowercaseName = name.lowercase(Locale.US)
            if (lowercaseName.contains("classical") || lowercaseName.contains("sitar") || lowercaseName.contains("sur")) {
                IndianPresetMetadata(
                    icon = Icons.Default.MusicNote,
                    leadingIconColor = Color(0xFFFF9800),
                    tagBgColor = Color(0xFFFF9800).copy(alpha = 0.15f),
                    tagTextColor = Color(0xFFFFB74D),
                    tagLabel = "CLASSICAL",
                    tagEmoji = "🪕"
                )
            } else if (lowercaseName.contains("sufi") || lowercaseName.contains("ghazal")) {
                IndianPresetMetadata(
                    icon = Icons.Default.MusicNote,
                    leadingIconColor = Color(0xFFE91E63),
                    tagBgColor = Color(0xFFE91E63).copy(alpha = 0.15f),
                    tagTextColor = Color(0xFFF48FB1),
                    tagLabel = "SUFI",
                    tagEmoji = "🎤"
                )
            } else if (lowercaseName.contains("bollywood") || lowercaseName.contains("dhol") || lowercaseName.contains("dhamaaka")) {
                IndianPresetMetadata(
                    icon = Icons.Default.MusicNote,
                    leadingIconColor = Color(0xFF9C27B0),
                    tagBgColor = Color(0xFF9C27B0).copy(alpha = 0.15f),
                    tagTextColor = Color(0xFFCE93D8),
                    tagLabel = "BOLLYWOOD",
                    tagEmoji = "🥁"
                )
            } else if (lowercaseName.contains("devotional") || lowercaseName.contains("bhajan") || lowercaseName.contains("aarti") || lowercaseName.contains("bhakti")) {
                IndianPresetMetadata(
                    icon = Icons.Default.MusicNote,
                    leadingIconColor = Color(0xFF00E676),
                    tagBgColor = Color(0xFF00E676).copy(alpha = 0.15f),
                    tagTextColor = Color(0xFF69F0AE),
                    tagLabel = "BHAKTI",
                    tagEmoji = "🪔"
                )
            } else null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqScreen(viewModel: EqViewModel) {
    val currentProfile by viewModel.currentProfile.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val connectedDevice by viewModel.connectedDeviceName.collectAsState()
    val waveformPoints by viewModel.waveformPoints.collectAsState()
    val spectrumBars by viewModel.spectrumBars.collectAsState()
    val visualizerStyle by viewModel.visualizerStyle.collectAsState()
    val isDolbyEnabled by viewModel.isDolbyEnabled.collectAsState()
    val dolbyMode by viewModel.dolbyMode.collectAsState()

    var showSaveDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    var isProfilesExpanded by remember { mutableStateOf(false) }
    var selectedFilterCategory by remember { mutableStateOf("All") }
    var selectedMediaTypeToSave by remember { mutableStateOf("Music") }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F))
            .padding(16.dp)
    ) {
        // 1. Header (Active Device Indicator)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "विवाद साउंड इनहांसर",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFFE6E1E5),
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    // Glowing Pulse
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFFD0BCFF), CircleShape)
                            .border(1.dp, Color(0xFFE8DEF8), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$connectedDevice • Active",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF938F99),
                        fontWeight = FontWeight.Medium
                    )
                }

                AnimatedVisibility(visible = isDolbyEnabled) {
                    val modeText = when (dolbyMode) {
                        0 -> "सिनेमा (Movie)"
                        1 -> "संगीत (Music)"
                        2 -> "खेल (Game)"
                        3 -> "ध्वनि (Voice)"
                        else -> "सिनेमा (Movie)"
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF9E86FF).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0xFF9E86FF).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(Color(0xFF9E86FF), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "DOLBY ATMOS • $modeText",
                                    fontSize = 9.sp,
                                    color = Color(0xFFD0BCFF),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Simulated Device Rotator
            IconButton(
                onClick = {
                    val alternateDevices = listOf("Sony WH-1000XM4", "Bose QC45", "Apple AirPods Pro 2", "Wired Headset", "USB-C Audio DAC")
                    val index = (alternateDevices.indexOf(connectedDevice) + 1) % alternateDevices.size
                    viewModel.setSimulatedConnectedDevice(alternateDevices[index])
                },
                modifier = Modifier
                    .background(Color(0xFF2B2930), CircleShape)
                    .size(44.dp)
                    .testTag("device_toggle_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Headset,
                    contentDescription = "Switch Simulated Headphone",
                    tint = Color(0xFFD0BCFF)
                )
            }
        }

        // 2. Real-time Audio Visualizer
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFF49454F)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Interactive dynamic Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .clickable {
                            val styles = listOf("Cosmic Neon", "Cyberpunk Peak", "Minimalist")
                            val nextStyleIdx = (styles.indexOf(visualizerStyle) + 1) % styles.size
                            viewModel.updateVisualizerStyle(styles[nextStyleIdx])
                        }
                ) {
                    val width = size.width
                    val height = size.height

                    // Choose colors based on style
                    val gradient = when (visualizerStyle) {
                        "Cyberpunk Peak" -> Brush.verticalGradient(listOf(Color(0xFFFF3366), Color(0xFF33FFFF)))
                        "Minimalist" -> Brush.verticalGradient(listOf(Color(0xFFE6E1E5), Color(0xFF938F99)))
                        else -> Brush.verticalGradient(listOf(Color(0xFFD0BCFF), Color(0xFF381E72)))
                    }

                    // 1. Draw Waveform background
                    val wavePath = Path()
                    val pointsCount = waveformPoints.size
                    val stepX = width / (pointsCount - 1)
                    wavePath.moveTo(0f, height / 2f)
                    for (i in 0 until pointsCount) {
                        val x = i * stepX
                        val y = (height / 2f) + (waveformPoints[i] * (height / 2.5f))
                        if (i == 0) wavePath.moveTo(x, y) else wavePath.lineTo(x, y)
                    }
                    drawPath(
                        path = wavePath,
                        brush = gradient,
                        style = Stroke(width = 1.5f, cap = StrokeCap.Round, miter = 1f),
                        alpha = 0.3f
                    )

                    // 2. Draw Spectrum Vertical Bars
                    val barCount = spectrumBars.size
                    val paddingX = 4f
                    val totalPadding = paddingX * (barCount + 1)
                    val barWidth = (width - totalPadding) / barCount

                    for (j in 0 until barCount) {
                        val barHeight = spectrumBars[j] * height * 0.85f
                        val x = paddingX + j * (barWidth + paddingX)
                        val y = height - barHeight
                        drawRoundRect(
                            brush = gradient,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                            alpha = 0.85f
                        )
                    }
                }

                // Text Overlay for Style Change indicator
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Text(
                        text = "Visualizer: $visualizerStyle",
                        fontSize = 9.sp,
                        color = Color(0xFF938F99),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .background(Color(0xFF1C1B1F).copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // 3. Category Filter Selector Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("All", "Music", "Movie", "Gaming").forEach { category ->
                val isSelected = selectedFilterCategory == category
                val label = when (category) {
                    "All" -> "All 🌐"
                    "Music" -> "Music 🎵"
                    "Movie" -> "Movie 🎬"
                    "Gaming" -> "Gaming 🎮"
                    else -> category
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) Color(0xFFD0BCFF) else Color(0xFF2B2930))
                        .border(1.dp, if (isSelected) Color(0xFFD0BCFF) else Color(0xFF49454F), RoundedCornerShape(16.dp))
                        .clickable { selectedFilterCategory = category }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("filter_category_$category")
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color(0xFF381E72) else Color(0xFFE6E1E5),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Profile Management Selector Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2B2930))
                    .border(1.dp, Color(0xFF49454F), RoundedCornerShape(12.dp))
                    .clickable { isProfilesExpanded = !isProfilesExpanded }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val currentIndianMeta = getIndianMetadata(currentProfile.id, currentProfile.name)
                    val collapsedIcon = if (currentIndianMeta != null) {
                        currentIndianMeta.icon
                    } else {
                        when (currentProfile.mediaType) {
                            "Music" -> Icons.Default.MusicNote
                            "Movie" -> Icons.Default.Movie
                            "Gaming" -> Icons.Default.SportsEsports
                            else -> Icons.Default.Tune
                        }
                    }
                    val collapsedIconColor = if (currentIndianMeta != null) {
                        currentIndianMeta.leadingIconColor
                    } else {
                        when (currentProfile.mediaType) {
                            "Music" -> Color(0xFF81C784)
                            "Movie" -> Color(0xFF64B5F6)
                            "Gaming" -> Color(0xFFFFB74D)
                            else -> Color(0xFFBA68C8)
                        }
                    }
                    Icon(
                        imageVector = collapsedIcon,
                        contentDescription = currentProfile.mediaType,
                        tint = collapsedIconColor,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(24.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentProfile.name,
                            color = Color(0xFFE6E1E5),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        val catLabel = if (currentIndianMeta != null) {
                            "${currentProfile.mediaType} • ${currentIndianMeta.tagLabel} ${currentIndianMeta.tagEmoji}"
                        } else {
                            when (currentProfile.mediaType) {
                                "Music" -> "Music 🎵"
                                "Movie" -> "Movie 🎬"
                                "Gaming" -> "Gaming 🎮"
                                else -> "General ⚙️"
                            }
                        }
                        Text(
                            text = catLabel,
                            color = if (currentIndianMeta != null) currentIndianMeta.leadingIconColor else Color(0xFF938F99),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Dropdown menu",
                        tint = Color(0xFFD0BCFF)
                    )
                }

                DropdownMenu(
                    expanded = isProfilesExpanded,
                    onDismissRequest = { isProfilesExpanded = false },
                    modifier = Modifier
                        .background(Color(0xFF2B2930))
                        .border(1.dp, Color(0xFF49454F), RoundedCornerShape(4.dp))
                ) {
                    val filteredProfiles = if (selectedFilterCategory == "All") {
                        profiles
                    } else {
                        profiles.filter { it.mediaType.equals(selectedFilterCategory, ignoreCase = true) }
                    }
                    filteredProfiles.forEach { profile ->
                        val indianMeta = getIndianMetadata(profile.id, profile.name)
                        DropdownMenuItem(
                            leadingIcon = {
                                val (icon, iconColor) = if (indianMeta != null) {
                                    indianMeta.icon to indianMeta.leadingIconColor
                                } else {
                                    when (profile.mediaType) {
                                        "Music" -> Icons.Default.MusicNote to Color(0xFF81C784)
                                        "Movie" -> Icons.Default.Movie to Color(0xFF64B5F6)
                                        "Gaming" -> Icons.Default.SportsEsports to Color(0xFFFFB74D)
                                        else -> Icons.Default.Tune to Color(0xFFBA68C8)
                                    }
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = profile.mediaType,
                                    tint = iconColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = profile.name,
                                                color = if (profile.id == currentProfile.id) Color(0xFFD0BCFF) else Color(0xFFE6E1E5),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            
                                            if (indianMeta != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(indianMeta.tagBgColor)
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "${indianMeta.tagLabel} ${indianMeta.tagEmoji}",
                                                        color = indianMeta.tagTextColor,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            } else {
                                                val (tagBg, tagTextCol, tagEmoji) = when (profile.mediaType) {
                                                    "Music" -> Triple(Color(0xFF81C784).copy(alpha = 0.15f), Color(0xFF81C784), "🎵")
                                                    "Movie" -> Triple(Color(0xFF64B5F6).copy(alpha = 0.15f), Color(0xFF64B5F6), "🎬")
                                                    "Gaming" -> Triple(Color(0xFFFFB74D).copy(alpha = 0.15f), Color(0xFFFFB74D), "🎮")
                                                    else -> Triple(Color(0xFFBA68C8).copy(alpha = 0.15f), Color(0xFFBA68C8), "⚙️")
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(tagBg)
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "${profile.mediaType} $tagEmoji",
                                                        color = tagTextCol,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = if (profile.isCustom) "Custom User Preset" else "System Preset",
                                            color = Color(0xFF938F99),
                                            fontSize = 9.sp
                                        )
                                    }
                                    if (profile.isCustom) {
                                        IconButton(
                                            onClick = { viewModel.deleteCustomProfile(profile) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Profile",
                                                tint = Color(0xFFF2B8B5),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            onClick = {
                                viewModel.selectActiveProfile(profile)
                                isProfilesExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Save Trigger Button
            IconButton(
                onClick = { showSaveDialog = true },
                modifier = Modifier
                    .background(Color(0xFFD0BCFF), RoundedCornerShape(12.dp))
                    .size(46.dp)
                    .testTag("save_profile_trigger")
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save Preset Profile",
                    tint = Color(0xFF381E72)
                )
            }
        }

        // 4. 2D Graphic EQ sliders Container
        Text(
            text = "Graphic Equalizer (9 Bands)",
            style = MaterialTheme.typography.titleSmall,
            color = Color(0xFFE6E1E5),
            modifier = Modifier.padding(bottom = 12.dp),
            fontWeight = FontWeight.Medium
        )

        val bands = listOf("60Hz", "120Hz", "250Hz", "500Hz", "1kHz", "2kHz", "4kHz", "8kHz", "16kHz")
        val currentGains = listOf(
            currentProfile.band60Hz,
            currentProfile.band120Hz,
            currentProfile.band250Hz,
            currentProfile.band500Hz,
            currentProfile.band1kHz,
            currentProfile.band2kHz,
            currentProfile.band4kHz,
            currentProfile.band8kHz,
            currentProfile.band16kHz
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF2B2930), RoundedCornerShape(24.dp))
                .border(1.dp, Color(0xFF49454F), RoundedCornerShape(24.dp))
                .padding(vertical = 12.dp)
        ) {
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                bands.forEachIndexed { index, freq ->
                    val gainValue = currentGains[index]
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(62.dp)
                            .padding(vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // dB Gain Indicator Text
                        Text(
                            text = String.format(Locale.US, "%+.1fdB", gainValue),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = Color(0xFF938F99),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )

                        // Elegant Vertical Slider Track
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .width(34.dp)
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Track Background
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFF49454F))
                            )

                            // Slider Component
                            Slider(
                                value = gainValue,
                                onValueChange = { newValue ->
                                    viewModel.updateBand(freq, newValue)
                                },
                                valueRange = -15f..15f,
                                modifier = Modifier
                                    .rotate(-90f)
                                    .size(160.dp, 34.dp)
                                    .testTag("slider_${freq}"),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFD0BCFF),
                                    activeTrackColor = Color.Transparent,
                                    inactiveTrackColor = Color.Transparent,
                                    activeTickColor = Color.Transparent,
                                    inactiveTickColor = Color.Transparent
                                )
                            )
                        }

                        // Target frequency label
                        Text(
                            text = freq,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE6E1E5),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            Button(
                onClick = { viewModel.resetSlidersToDefault() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(38.dp).testTag("reset_sliders_button")
            ) {
                Text(text = "Reset to Default", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { viewModel.selectActiveProfile(EqProfile.FLAT) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF49454F)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(38.dp).testTag("flat_reset_button")
            ) {
                Text(text = "फ़्लैट रीसेट", color = Color(0xFFE6E1E5), fontSize = 12.sp)
            }
        }
    }

    // 5. Save Named Profile Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = {
                showSaveDialog = false
                newProfileName = ""
                selectedMediaTypeToSave = "Music"
            },
            title = {
                Text(
                    text = "नया प्रोफाइल सेव करें",
                    color = Color(0xFFE6E1E5),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        label = { Text("प्रोफाइल का नाम") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD0BCFF),
                            unfocusedBorderColor = Color(0xFF49454F),
                            focusedLabelColor = Color(0xFFD0BCFF),
                            focusedTextColor = Color(0xFFE6E1E5),
                            unfocusedTextColor = Color(0xFFE6E1E5)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("new_profile_name_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "मीडिया का प्रकार (Media Type):",
                        color = Color(0xFF938F99),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Music", "Movie", "Gaming", "General").forEach { type ->
                            val isSelected = selectedMediaTypeToSave == type
                            val icon = when (type) {
                                "Music" -> "🎵"
                                "Movie" -> "🎬"
                                "Gaming" -> "🎮"
                                else -> "⚙️"
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFD0BCFF) else Color(0xFF49454F))
                                    .clickable { selectedMediaTypeToSave = type }
                                    .padding(vertical = 8.dp)
                                    .testTag("media_chip_$type"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = icon, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = type,
                                        color = if (isSelected) Color(0xFF381E72) else Color(0xFFE6E1E5),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newProfileName.isNotBlank()) {
                            viewModel.saveAsNewProfile(newProfileName.trim(), selectedMediaTypeToSave)
                            showSaveDialog = false
                            newProfileName = ""
                            selectedMediaTypeToSave = "Music"
                        }
                    },
                    modifier = Modifier.testTag("save_profile_confirm_button")
                ) {
                    Text(text = "सेव", color = Color(0xFFD0BCFF))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSaveDialog = false
                    newProfileName = ""
                    selectedMediaTypeToSave = "Music"
                }) {
                    Text(text = "रद्द", color = Color(0xFF938F99))
                }
            },
            containerColor = Color(0xFF2B2930)
        )
    }
}
