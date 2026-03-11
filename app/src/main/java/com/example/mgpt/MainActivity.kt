package com.example.mgpt

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
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
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)
        
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        enableEdgeToEdge()
        
        // Connect to Render Backend
        SocketManager.connect("https://mgt-server.onrender.com")

        setContent {
            MgptTheme {
                val userSession by sessionManager.userSession.collectAsStateWithLifecycle(initialValue = null)
                val isConnected by SocketManager.isConnected.collectAsStateWithLifecycle()

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
                            isConnected = isConnected,
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
    isConnected: Boolean,
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
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (!isConnected) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp).statusBarsPadding(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Waking up Tactical Server...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
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
    val units by viewModel.units.collectAsStateWithLifecycle()
    
    val defaultPos = LatLng(1.3521, 103.8198)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPos, 12f)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = true),
                uiSettings = MapUiSettings(myLocationButtonEnabled = true)
            ) {
                units.values.forEach { unit ->
                    Marker(
                        state = MarkerState(position = LatLng(unit.lat, unit.lng)),
                        title = "Unit: ${unit.id}",
                        snippet = "Role: ${unit.role}"
                    )
                }
            }
            
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
            ) {
                Text(
                    "TACTICAL GRID ACTIVE",
                    modifier = Modifier.padding(4.dp),
                    color = Color.Green,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp)
        ) {
            Text(
                "LIVE TACTICAL FEED",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(feed) { entry ->
                    Text(
                        text = entry,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
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
        Text("COMMAND & CONTROL", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            Text("PROVISION NEW UNIT")
        }
    }
}
