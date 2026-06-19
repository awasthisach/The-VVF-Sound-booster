package com.example.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat

@Composable
fun SettingsScreen(viewModel: EqViewModel) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val bufferSize by viewModel.bufferSize.collectAsState()
    val legacyMode by viewModel.legacyMode.collectAsState()
    val enhancedDetection by viewModel.enhancedDetection.collectAsState()
    val detectedSessions by viewModel.detectedSessions.collectAsState()
    val serviceStats by viewModel.serviceStats.collectAsState()

    var isNotificationAccessGranted by remember { mutableStateOf(false) }

    // Check permission state in lifecycle
    LaunchedEffect(Unit) {
        val packageNames = NotificationManagerCompat.getEnabledListenerPackages(context)
        isNotificationAccessGranted = packageNames.contains(context.packageName)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F))
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Title
        Text(
            text = "ऐप सेटिंग्स (Settings)",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFE6E1E5),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text = "सिस्टम-स्तरीय ऑडियो इंटरसेप्शन तथा लेटेंसी बफ़र को प्रबंधित करें।",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF938F99),
            modifier = Modifier.padding(bottom = 16.dp)
        )



        // 1. Buffer Size Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
            border = BorderStroke(1.dp, Color(0xFF49454F))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.SettingsInputComponent, contentDescription = "Buffer Size", tint = Color(0xFFD0BCFF))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "ऑडियो बफ़र साइज़ (Buffer Size)", color = Color(0xFFE6E1E5), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                val buffersList = listOf("Ultra Low (128)", "Low (256)", "Medium (512)", "Normal (1024)", "High (2048)", "Extreme (4096)")
                var dropdownExpanded by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1C1B1F), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF49454F), RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dropdownExpanded = !dropdownExpanded }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = bufferSize, color = Color(0xFFE6E1E5), fontSize = 13.sp)
                        Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Choose", tint = Color(0xFFD0BCFF))
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.background(Color(0xFF2B2930))
                    ) {
                        buffersList.forEach { valStr ->
                            DropdownMenuItem(
                                text = { Text(text = valStr, color = Color(0xFFE6E1E5)) },
                                onClick = {
                                    viewModel.setBufferSize(valStr)
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Buffer Tips Panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1C1B1F), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Tips",
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "सलाह: यूट्यूब / वीडियो देखने के लिए कम बफ़र (कम लेटेंसी) रखें। हाई-रेस म्यूज़िक सुनने के लिए अधिक बफ़र (बेहतर स्थिरता और सूक्ष्म परिशुद्धता) रखें।",
                            fontSize = 11.sp,
                            color = Color(0xFF938F99),
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // 2. Intercept Mode Toggle Card (Legacy & Dumpsys)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
            border = BorderStroke(1.dp, Color(0xFF49454F))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.BugReport, contentDescription = "Intercept Settings", tint = Color(0xFFD0BCFF))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "इंटरसेप्शन मोड (Detection Modes)", color = Color(0xFFE6E1E5), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Legacy Mode Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(text = "ग्लोबल लिगेसी मोड (Legacy Mode)", color = Color(0xFFE6E1E5), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "ग्लोबल मिक्सर (Session 0) पर सीधे प्रभाव लागू करता है। यह उन पुराने फोन्स के लिए उपयोगी है जो ब्रॉडकास्ट सत्र नहीं भेजते।",
                            color = Color(0xFF938F99),
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }
                    Switch(
                        checked = legacyMode,
                        onCheckedChange = { viewModel.toggleLegacyMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF381E72),
                            checkedTrackColor = Color(0xFFD0BCFF)
                        ),
                        modifier = Modifier.testTag("legacy_mode_switch")
                    )
                }

                Divider(color = Color(0xFF49454F), modifier = Modifier.padding(vertical = 14.dp))

                // Enhanced Dumpsys mode Selector Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(text = "उन्नत सत्र डिटेक्टर (Enhanced Polling)", color = Color(0xFFE6E1E5), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "डीप DUMP अनुमति के ज़रिए ऑडियो फ्लिंगर पोलिंग को सक्रिय करता है, जो छुपे हुए प्लेयर मीडिया सत्र ढूंढ लेता है।",
                            color = Color(0xFF938F99),
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }
                    Switch(
                        checked = enhancedDetection,
                        onCheckedChange = { viewModel.toggleEnhancedDetection(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF381E72),
                            checkedTrackColor = Color(0xFFD0BCFF)
                        ),
                        modifier = Modifier.testTag("enhanced_detection_switch")
                    )
                }
            }
        }

        // 3. Step-by-Step ADB Perm Grant Panel (Shown when Enhanced toggle is focused/shown)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
            border = BorderStroke(1.dp, Color(0xFF49454F))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Terminal, contentDescription = "ADB Console Guide", tint = Color(0xFFD0BCFF))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "ADB डीप DUMP सेटिंग गाइड", color = Color(0xFFE6E1E5), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "अगर उन्नत पोलिंग सक्षम है तो आपको एक बार अपने कंप्यूटर से ADB टूल की मदद से DUMP राइट प्रदान करनी होगी:",
                    fontSize = 11.sp,
                    color = Color(0xFF938F99),
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Code Console Look Block
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF131215), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF49454F), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "adb shell pm grant com.aistudio.vivadequ.gpyzx android.permission.DUMP",
                        color = Color(0xFFA8EFF0),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier.testTag("adb_command_text")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "निर्देश:\n1. फोन की 'Developer Option' में जाकर 'USB Debugging' चालू करें।\n2. फोन को PC से कनेक्ट करें और टर्मिनल में ऊपर दिया गया पूरा कमांड पेस्ट कर एंटर दबाएं।",
                    fontSize = 10.sp,
                    color = Color(0xFF938F99),
                    lineHeight = 14.sp
                )
            }
        }

        // 4. Notification Access Manager Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
            border = BorderStroke(1.dp, Color(0xFF49454F))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = "Notification listener permissions", tint = Color(0xFFD0BCFF))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "म्यूजिक डिटेक्टर अनुमति (Notification Access)", color = Color(0xFFE6E1E5), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "सक्रिय म्यूजिक प्लेयर (Spotify, Youtube आदि) की सूचनाओं को ट्रैक करके यह ऑटोमेटेड सेशन लोड करता है।",
                    fontSize = 11.sp,
                    color = Color(0xFF938F99),
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isNotificationAccessGranted) Color(0xFF34C759) else Color(0xFFFF453A), RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isNotificationAccessGranted) "अनुमति स्वीकृत (Granted)" else "अनुमति अनुपलब्ध (Required)",
                            color = if (isNotificationAccessGranted) Color(0xFF34C759) else Color(0xFFFF3B30),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!isNotificationAccessGranted) {
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                } catch (e: Exception) {
                                    // Fallback
                                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp).testTag("grant_notification_permission_button")
                        ) {
                            Text(text = "परमिशन दें", color = Color(0xFF381E72), fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Service Resource Monitor & Guardian Card
         Card(
             modifier = Modifier
                 .fillMaxWidth()
                 .padding(bottom = 16.dp),
             colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
             border = BorderStroke(1.dp, Color(0xFF49454F))
         ) {
             Column(modifier = Modifier.padding(16.dp)) {
                 Row(
                     verticalAlignment = Alignment.CenterVertically,
                     horizontalArrangement = Arrangement.SpaceBetween,
                     modifier = Modifier.fillMaxWidth()
                 ) {
                     Row(verticalAlignment = Alignment.CenterVertically) {
                         Icon(
                             imageVector = Icons.Default.BugReport,
                             contentDescription = "Resource Guardian",
                             tint = Color(0xFF81C784)
                         )
                         Spacer(modifier = Modifier.width(10.dp))
                         Text(
                             text = "सर्विस गार्डियन और रिसोर्स मॉनिटर",
                             color = Color(0xFFE6E1E5),
                             fontWeight = FontWeight.Bold,
                             fontSize = 14.sp
                         )
                     }
                     
                     // Health Status Badge
                     Box(
                         modifier = Modifier
                             .clip(RoundedCornerShape(4.dp))
                             .background(if (serviceStats.isEngineHealthy) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                             .padding(horizontal = 6.dp, vertical = 2.dp)
                     ) {
                         Text(
                             text = if (serviceStats.isEngineHealthy) "Healthy ✅" else "Degraded ⚠️",
                             color = if (serviceStats.isEngineHealthy) Color(0xFF2E7D32) else Color(0xFFC62828),
                             fontSize = 10.sp,
                             fontWeight = FontWeight.Bold
                         )
                     }
                 }

                 Spacer(modifier = Modifier.height(14.dp))

                 // 1. Memory Usage progress indicator
                 Row(
                     horizontalArrangement = Arrangement.SpaceBetween,
                     modifier = Modifier.fillMaxWidth()
                 ) {
                     Text(
                         text = "रैम उपयोग (Memory Usage)",
                         color = Color(0xFF938F99),
                         fontSize = 12.sp
                     )
                     val percentage = serviceStats.memoryUsagePercent
                     Text(
                         text = "${serviceStats.usedMemoryMb} MB / ${serviceStats.maxMemoryMb} MB (${String.format(java.util.Locale.US, "%.1f", percentage)}%)",
                         color = Color(0xFFE6E1E5),
                         fontSize = 11.sp,
                         fontWeight = FontWeight.SemiBold
                     )
                 }
                 Spacer(modifier = Modifier.height(6.dp))
                 LinearProgressIndicator(
                     progress = { (serviceStats.memoryUsagePercent / 100f).coerceIn(0f, 1f) },
                     modifier = Modifier
                         .fillMaxWidth()
                         .height(6.dp)
                         .clip(RoundedCornerShape(3.dp)),
                     color = if (serviceStats.memoryUsagePercent < 75f) Color(0xFF81C784) else Color(0xFFE57373),
                     trackColor = Color(0xFF49454F)
                 )

                 Spacer(modifier = Modifier.height(14.dp))

                 // 2. Performance Stats: Startup & Interventions
                 Row(
                     horizontalArrangement = Arrangement.spacedBy(16.dp),
                     modifier = Modifier.fillMaxWidth()
                 ) {
                     Column(modifier = Modifier.weight(1f)) {
                         Text(
                             text = "स्टार्टअप गति (Startup Time)",
                             color = Color(0xFF938F99),
                             fontSize = 11.sp
                         )
                         Text(
                             text = "${serviceStats.startupTimeMs} ms",
                             color = Color(0xFFE6E1E5),
                             fontSize = 13.sp,
                             fontWeight = FontWeight.Bold
                         )
                     }
                     Column(modifier = Modifier.weight(1f)) {
                         Text(
                             text = "गार्डियन सुरक्षा कवच (🛡️)",
                             color = Color(0xFF938F99),
                             fontSize = 11.sp
                         )
                         Text(
                             text = if (serviceStats.guardianActionCount == 0) "Active ✅" else "${serviceStats.guardianActionCount} Interventions 🛡️",
                             color = if (serviceStats.guardianActionCount == 0) Color(0xFF81C784) else Color(0xFFFFB74D),
                             fontSize = 13.sp,
                             fontWeight = FontWeight.Bold
                         )
                     }
                 }
             }
         }

        // 5. Active Sessions Information Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930).copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Language, contentDescription = "Session status", tint = Color(0xFF938F99), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "सक्रिय कनेक्टेड सेशन: ${detectedSessions.size}", color = Color(0xFF938F99), fontSize = 12.sp)
                }
                if (detectedSessions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "IDs: ${detectedSessions.joinToString(", ")}",
                        fontSize = 11.sp,
                        color = Color(0xFFD0BCFF),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
