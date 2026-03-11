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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AddAlert
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
import androidx.compose.ui.viewinterop.AndroidView
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
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MainActivity : ComponentActivity() {
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("osmdroid_prefs", MODE_PRIVATE)
        org.osmdroid.config.Configuration.getInstance().load(this, prefs)
        org.osmdroid.config.Configuration.getInstance().userAgentValue = "com.example.mgpt"
        sessionManager = SessionManager(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        SocketManager.connect("https://mgt-server.onrender.com")

        setContent {
            MgptTheme {
                val userSession by sessionManager.userSession.collectAsStateWithLifecycle(initialValue = null)
                val isConnected by SocketManager.isConnected.collectAsStateWithLifecycle()

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (userSession == null) {
                        LoginScreen(onLoginSuccess = { id, username, role ->
                            lifecycleScope.launch {
                                val assignedRole = if (username == "admin") UserRole.SUPER_ADMIN else role
                                val user = User(id, username, assignedRole)
                                sessionManager.saveSession(user)
                                if (assignedRole == UserRole.PATROL) startTrackingService()
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
}

@Composable
fun MgptApp(user: User, isConnected: Boolean, onLogout: () -> Unit, viewModel: MainViewModel = viewModel()) {
    var currentDestination by remember { mutableStateOf(AppDestinations.TACTICAL_MAP) }
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach { destination ->
                if (destination == AppDestinations.ADMIN && user.role != UserRole.SUPER_ADMIN) return@forEach
                item(
                    icon = { Icon(destination.icon, null) },
                    label = { Text(destination.label) },
                    selected = destination == currentDestination,
                    onClick = {
                        if (destination == AppDestinations.LOGOUT) onLogout()
                        else currentDestination = destination
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (!isConnected) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (currentDestination) {
                    AppDestinations.TACTICAL_MAP -> TacticalMapScreen(viewModel)
                    AppDestinations.REPORT -> ReportIncidentScreen(user) { type, priority, desc ->
                        viewModel.reportIncident(type.name, priority.name, desc)
                        currentDestination = AppDestinations.TACTICAL_MAP
                    }
                    AppDestinations.ADMIN -> Text("Admin Panel")
                    else -> {}
                }
            }
        }
    }
}

enum class AppDestinations(val label: String, val icon: ImageVector) {
    TACTICAL_MAP("MAP", Icons.Default.Map),
    REPORT("REPORT", Icons.Default.AddAlert),
    ADMIN("ADMIN", Icons.Default.Settings),
    LOGOUT("LOGOUT", Icons.AutoMirrored.Filled.ExitToApp)
}

@Composable
fun TacticalMapScreen(viewModel: MainViewModel) {
    val units by viewModel.units.collectAsStateWithLifecycle()
    AndroidView(
        factory = { context ->
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                controller.setZoom(12.0)
                controller.setCenter(GeoPoint(1.3521, 103.8198))
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { mapView ->
            mapView.overlays.clear()
            units.values.forEach { unit ->
                val marker = Marker(mapView)
                marker.position = GeoPoint(unit.lat, unit.lng)
                marker.title = unit.id
                mapView.overlays.add(marker)
            }
            mapView.invalidate()
        }
    )
}
