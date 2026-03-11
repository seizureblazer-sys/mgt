package com.example.mgpt.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mgpt.data.Incident
import com.example.mgpt.data.PatrolUnit
import com.example.mgpt.data.UserRole
import com.example.mgpt.network.SocketManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainViewModel : ViewModel() {
    private val _units = MutableStateFlow<Map<String, PatrolUnit>>(emptyMap())
    val units: StateFlow<Map<String, PatrolUnit>> = _units

    private val _incidents = MutableStateFlow<List<Incident>>(emptyList())
    val incidents: StateFlow<List<Incident>> = _incidents

    private val _tacticalFeed = MutableStateFlow<List<String>>(emptyList())
    val tacticalFeed: StateFlow<List<String>> = _tacticalFeed
    
    val isConnected = SocketManager.isConnected

    init {
        setupSocketListeners()
    }

    private fun setupSocketListeners() {
        SocketManager.on("unit_moved") { args ->
            val data = args[0] as JSONObject
            val unitId = data.getString("unitId")
            val lat = data.getDouble("lat")
            val lng = data.getDouble("lng")
            val role = try { UserRole.valueOf(data.getString("role")) } catch (e: Exception) { UserRole.PATROL }

            _units.value = _units.value + (unitId to PatrolUnit(unitId, lat, lng, role))
            addToFeed("Unit $unitId moved to $lat, $lng")
        }

        SocketManager.on("new_incident") { args ->
            val data = args[0] as JSONObject
            addToFeed("NEW INCIDENT: ${data.getString("type")} at ${data.optString("location", "Unknown")}")
        }
    }

    fun reportIncident(type: String, priority: String, description: String) {
        val data = JSONObject().apply {
            put("type", type)
            put("priority", priority)
            put("description", description)
        }
        SocketManager.emit("report_incident", data)
        addToFeed("REPORTED: $type - $priority")
    }

    private fun addToFeed(message: String) {
        viewModelScope.launch {
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            _tacticalFeed.value = (listOf("[$timestamp] $message") + _tacticalFeed.value).take(50)
        }
    }
}
