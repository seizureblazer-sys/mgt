package com.example.mgpt

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mgpt.data.SessionManager
import com.example.mgpt.data.User
import com.example.mgpt.data.UserRole
import com.example.mgpt.network.SocketManager
import com.example.mgpt.service.LocationService
import com.example.mgpt.ui.MainViewModel
import com.example.mgpt.ui.screens.LoginScreen
import com.example.mgpt.ui.screens.ReportIncidentScreen
import com.example.mgpt.ui.theme.MgptTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)
        
        // Wake Lock: Prevents screen from dimming during active patrols
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        enableEdgeToEdge()
        
        // Connect to Tactical Backend
        SocketManager.connect("http://10.0.2.2:3000") { // Default emulator address
            runOnUiThread {
                // Handle connection success if needed
            }
        }

        setContent {
            MgptTheme {
                val userSession by sessionManager.userSession.collectAsStateWithLifecycle(initialValue = null)

                if (userSession == null) {
                    LoginScreen(onLoginSuccess = { id, username, role ->
                        lifecycleScope.launch {
                            val user = User(id, username, role)
                            sessionManager.saveSession(user)
                            if (role == UserRole.PATROL) {
                                startTrackingService()
                            }
                        }
                    })
                } else {
                    MgptApp(
                        user = userSession!!,
                        onLogout = {
                            lifecycleScope.launch {
                                sessionManager.clearSession()
                                stopTrackingService()
                            }
                        }
                    )
                }
            }
        }
    }

    private fun startTrackingService() {
        val intent = Intent(this, LocationService::class.java)
        startForegroundService(intent)
    }

    private fun stopTrackingService() {
        val intent = Intent(this, LocationService::class.java)
        stopService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        SocketManager.disconnect()
    }
}

@Composable
fun MgptApp(
    user: User, 
    onLogout: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    var currentDestination by remember { mutableStateOf(AppDestinations.TACTICAL_MAP) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach { destination ->
                if (destination == AppDestinations.ADMIN && user.role != UserRole.SUPER_ADMIN) return@forEach
                
                item(
                    icon = { Icon(destination.icon, contentDescription = destination.label) },
                    label = { Text(destination.label) },
                    selected = destination == currentDestination,
                    onClick = {
                        if (destination == AppDestinations.LOGOUT) {
                            onLogout()
                        } else {
                            currentDestination = destination
                        }
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Surface(
                modifier = Modifier.padding(innerPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                when (currentDestination) {
                    AppDestinations.TACTICAL_MAP -> TacticalMapScreen(user, viewModel)
                    AppDestinations.REPORT -> ReportIncidentScreen(user) { type, priority, desc ->
                        viewModel.reportIncident(type.name, priority.name, desc)
                        currentDestination = AppDestinations.TACTICAL_MAP
                    }
                    AppDestinations.ADMIN -> AdminPanelScreen(user)
                    AppDestinations.LOGOUT -> {}
                }
            }
        }
    }
}

enum class AppDestinations(val label: String, val icon: ImageVector) {
    TACTICAL_MAP("MAP", Icons.Default.Map),
    REPORT("REPORT", Icons.Default.AddAlert),
    ADMIN("ADMIN", Icons.Default.Settings),
    LOGOUT("LOGOUT", Icons.Default.ExitToApp)
}

@Composable
fun TacticalMapScreen(user: User, viewModel: MainViewModel) {
    val feed by viewModel.tacticalFeed.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // Map Placeholder (Simulation)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Text(
                "TACTICAL GRID: SECTOR 7",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall
            )
            
            // Simulation of Unit Markers
            Text(
                "• UNIT_01 [INF]",
                modifier = Modifier.padding(start = 100.dp, top = 200.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall
            )
        }

        // Tactical Feed (Log)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp)
        ) {
            Text(
                "REAL-TIME TACTICAL FEED",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))
            
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(feed) { entry ->
                    Text(
                        text = entry,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AdminPanelScreen(user: User) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("COMMAND & CONTROL PANEL", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("USER MANAGEMENT", style = MaterialTheme.typography.titleLarge)
        
        // Placeholder for user creation UI
        OutlinedTextField(
            value = "",
            onValueChange = {},
            label = { Text("NEW UNIT IDENTIFIER") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            Text("PROVISION ACCESS")
        }
    }
}
