package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AutoEqScreen(viewModel: EqViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val headphones by viewModel.filteredHeadphones.collectAsState()
    val connectedDevice by viewModel.connectedDeviceName.collectAsState()

    var showImportDialog by remember { mutableStateOf(false) }
    var importName by remember { mutableStateOf("") }
    var importData by remember { mutableStateOf("") }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F))
            .padding(16.dp)
    ) {
        // Title
        Text(
            text = "ऑटोईक्यु (AutoEq) डेटाबेस",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFE6E1E5),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text = "ओपन सोर्स AutoEq सुधार वक्र सीधे अपने हेडफ़ोन मॉडल के अनुसार लोड करें या Squig, Squiglink या autoeq.app का डेटा पेस्ट करें।",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF938F99),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Action Row Search + Custom Import Trigger
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("हेडफ़ोन खोजें (जैसे Sony, AirPods...)", color = Color(0xFF938F99), fontSize = 13.sp) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color(0xFFD0BCFF)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF938F99))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFD0BCFF),
                    unfocusedBorderColor = Color(0xFF49454F),
                    focusedContainerColor = Color(0xFF2B2930),
                    unfocusedContainerColor = Color(0xFF2B2930),
                    focusedTextColor = Color(0xFFE6E1E5),
                    unfocusedTextColor = Color(0xFFE6E1E5)
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("headphone_search_input")
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { showImportDialog = true },
                modifier = Modifier
                    .background(Color(0xFFD0BCFF), RoundedCornerShape(12.dp))
                    .size(50.dp)
                    .testTag("open_import_dialog_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ImportExport,
                    contentDescription = "Import custom curves data",
                    tint = Color(0xFF381E72)
                )
            }
        }

        // Selected headphone hint
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930).copy(alpha = 0.6f)),
            border = BorderStroke(1.dp, Color(0xFF49454F))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Active Target Headphone",
                    tint = Color(0xFFD0BCFF),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "लोड किया गया ऑटोईक्यु आपके सक्रिय डिवाइस ($connectedDevice) से अपने आप मैप हो जाएगा।",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF938F99),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        // Search Results List
        Text(
            text = "आधिकारिक सूची (${headphones.size} हेडफ़ोन)",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF938F99),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (headphones.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "खेद है, कोई हेडफ़ोन नहीं मिला। कस्टमाइज़ वक्र जोड़ने के लिए आयात बटन दबाएं।",
                            color = Color(0xFF938F99),
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(headphones) { eq ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.loadAutoEqHeadphone(eq)
                                Toast
                                    .makeText(
                                        context,
                                        "${eq.brand} ${eq.model} का करेक्शन कर्व लोड हुआ!",
                                        Toast.LENGTH_SHORT
                                    )
                                    .show()
                            }
                            .testTag("headphone_item_${eq.brand}_${eq.model}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                        border = BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = eq.model,
                                    color = Color(0xFFE6E1E5),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = eq.brand,
                                    color = Color(0xFF938F99),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            // Visual small curves previews
                            Row(
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF1C1B1F)),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                eq.gains.forEachIndexed { i, g ->
                                    if (i % 2 == 0) { // draw 5 lines as curve summary
                                        val barHeightAnimation = ((g + 15f) / 30f).coerceIn(0.1f, 0.9f)
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .fillMaxHeight(barHeightAnimation)
                                                .clip(RoundedCornerShape(1.dp))
                                                .background(Color(0xFFD0BCFF))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Import pasted curve Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = {
                showImportDialog = false
                importName = ""
                importData = ""
            },
            title = {
                Text(
                    text = "Squig / AutoEq डेटा आयात करें",
                    color = Color(0xFFE6E1E5),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "निर्देश: autoeq.app या squig.link से 'Frequency Response' को .txt के रूप में एक्सपोर्ट करें, फिर उसकी फ़्रीक्वेंसी-गैन मान सूची को यहाँ पेस्ट कें (जैसे: 60 -1.2)।",
                        fontSize = 11.sp,
                        color = Color(0xFF938F99),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = importName,
                        onValueChange = { importName = it },
                        placeholder = { Text("हेडफ़ोन मॉडल का नाम (जैसे Tin T2...)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFFE6E1E5),
                            unfocusedTextColor = Color(0xFFE6E1E5),
                            focusedBorderColor = Color(0xFFD0BCFF),
                            unfocusedBorderColor = Color(0xFF49454F)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag("import_model_name_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = importData,
                        onValueChange = { importData = it },
                        placeholder = { Text("60 -1.5\n120 -3.2\n250 -4.5\n500 0.8\n1000 2.1\n...\nपेस्ट करें") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFFE6E1E5),
                            unfocusedTextColor = Color(0xFFE6E1E5),
                            focusedBorderColor = Color(0xFFD0BCFF),
                            unfocusedBorderColor = Color(0xFF49454F)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("import_data_input"),
                        singleLine = false,
                        maxLines = 15
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (importName.isNotBlank() && importData.isNotBlank()) {
                            val success = viewModel.importCustomAutoEqCurve(importName.trim(), importData.trim())
                            if (success) {
                                Toast.makeText(context, "कस्टम वक्र '$importName' सफलतापूर्वक लोड किया गया!", Toast.LENGTH_LONG).show()
                                showImportDialog = false
                                importName = ""
                                importData = ""
                            } else {
                                Toast.makeText(context, "त्रुटि! डेटा प्रारूप की जाँच करें। पंक्तियों में 'फ़्रीक्वेंसी मान' होना चाहिए।", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.testTag("import_confirm_button")
                ) {
                    Text(text = "आयात (Import)", color = Color(0xFFD0BCFF))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportDialog = false
                    importName = ""
                    importData = ""
                }) {
                    Text(text = "रद्द", color = Color(0xFF938F99))
                }
            },
            containerColor = Color(0xFF2B2930)
        )
    }
}
