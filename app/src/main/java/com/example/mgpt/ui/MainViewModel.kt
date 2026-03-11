package com.example.mgpt.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mgpt.data.Incident
import com.example.mgpt.data.PatrolUnit
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

    init {
        setupSocketListeners()
    }

    private fun setupSocketListeners() {
        SocketManager.on("unit_moved") { args ->
            val data = args[0] as JSONObject
            // Update unit position logic
            addToFeed("Unit ${data.getString("unitId")} moved")
        }

        SocketManager.on("new_incident") { args ->
            val data = args[0] as JSONObject
            // Add incident logic
            addToFeed("NEW INCIDENT: ${data.getString("type")}")
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
            _tacticalFeed.value = (listOf("[${System.currentTimeMillis()}] $message") + _tacticalFeed.value).take(50)
        }
    }
}
