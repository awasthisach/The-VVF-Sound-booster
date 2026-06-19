package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.lifecycleScope
import com.example.audio.AudioService
import com.example.data.AppDatabase
import com.example.data.EqRepository
import com.example.ui.AutoEqScreen
import com.example.ui.EqScreen
import com.example.ui.EqViewModel
import com.example.ui.EqViewModelFactory
import com.example.ui.SettingsScreen
import com.example.ui.ToolsScreen
import androidx.compose.material3.MaterialTheme
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize SQLite storage layer
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = EqRepository(database.eqDao())

        // 2. Build MVVM states using compiled Factory
        val viewModel: EqViewModel by viewModels {
            EqViewModelFactory(repository, applicationContext)
        }

        // 3. Launch foreground equalizer audio engine
        val serviceIntent = Intent(this, AudioService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        setContent {
            MyApplicationTheme(darkTheme = true, dynamicColor = false) {
                val currentTab by viewModel.currentTab.collectAsState()
                val serviceStats by viewModel.serviceStats.collectAsState()

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    topBar = {
                        PersistentDashboard(serviceStats = serviceStats)
                    },
                    bottomBar = {
                        BottomBar(
                            currentTab = currentTab,
                            onTabSelected = { viewModel.selectTab(it) }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        when (currentTab) {
                            0 -> EqScreen(viewModel = viewModel)
                            1 -> AutoEqScreen(viewModel = viewModel)
                            2 -> ToolsScreen(viewModel = viewModel)
                            3 -> SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomBar(
    currentTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline))
            .testTag("app_navigation_bar")
    ) {
        NavigationBarItem(
            selected = currentTab == 0,
            onClick = { onTabSelected(0) },
            label = { Text("इक्वलाइज़र", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (currentTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Equalizer,
                    contentDescription = "Equalizer"
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.background,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                indicatorColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.testTag("nav_tab_equalizer")
        )

        NavigationBarItem(
            selected = currentTab == 1,
            onClick = { onTabSelected(1) },
            label = { Text("ऑटोईक्यु", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (currentTab == 1) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Headset,
                    contentDescription = "AutoEq"
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.background,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                selectedTextColor = MaterialTheme.colorScheme.secondary,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                indicatorColor = MaterialTheme.colorScheme.secondary
            ),
            modifier = Modifier.testTag("nav_tab_autoeq")
        )

        NavigationBarItem(
            selected = currentTab == 2,
            onClick = { onTabSelected(2) },
            label = { Text("टूल्स", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (currentTab == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Tools (DSP)"
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.background,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                indicatorColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.testTag("nav_tab_tools")
        )

        NavigationBarItem(
            selected = currentTab == 3,
            onClick = { onTabSelected(3) },
            label = { Text("सेटिंग्स", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (currentTab == 3) Color(0xFF81C784) else MaterialTheme.colorScheme.onSurfaceVariant) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.background,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                selectedTextColor = MaterialTheme.colorScheme.tertiary,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                indicatorColor = MaterialTheme.colorScheme.tertiary
            ),
            modifier = Modifier.testTag("nav_tab_settings")
        )
    }
}

@Composable
fun PersistentDashboard(
    serviceStats: com.example.data.ServiceResourceStats
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("persistent_service_dashboard"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
        border = BorderStroke(1.dp, Color(0xFF49454F))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Title and engine pulse
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (serviceStats.isEngineHealthy) Color(0xFF81C784) else Color(0xFFE57373),
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "VIVAD ENGINE",
                    color = Color(0xFFE6E1E5),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }

            // RAM stat
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RAM: ",
                    color = Color(0xFF938F99),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${serviceStats.usedMemoryMb}M",
                    color = Color(0xFFD0BCFF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // CPU stat
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CPU: ",
                    color = Color(0xFF938F99),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${String.format(java.util.Locale.US, "%.1f", serviceStats.cpuUsagePercent)}%",
                    color = if (serviceStats.cpuUsagePercent < 15f) Color(0xFF81C784) else Color(0xFFFFB74D),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Guardian count
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🛡️ ${serviceStats.guardianActionCount}",
                    color = if (serviceStats.guardianActionCount == 0) Color(0xFF81C784) else Color(0xFFFFB74D),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
