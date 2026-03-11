package com.example.mgpt.network

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.net.URISyntaxException

object SocketManager {
    private var socket: Socket? = null
    private const val TAG = "SocketManager"

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    fun connect(url: String, onConnect: () -> Unit = {}) {
        if (socket?.connected() == true) return
        
        try {
            socket = IO.socket(url)
            socket?.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Connected to server")
                _isConnected.value = true
                onConnect()
            }
            socket?.on(Socket.EVENT_DISCONNECT) {
                Log.d(TAG, "Disconnected from server")
                _isConnected.value = false
            }
            socket?.on(Socket.EVENT_CONNECT_ERROR) {
                Log.e(TAG, "Connection Error: ${it[0]}")
            }
            socket?.connect()
        } catch (e: URISyntaxException) {
            Log.e(TAG, "URL Syntax Error", e)
        }
    }

    fun emit(event: String, data: JSONObject) {
        socket?.emit(event, data)
    }

    fun on(event: String, listener: (args: Array<out Any>) -> Unit) {
        socket?.on(event) { args ->
            listener(args)
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket = null
        _isConnected.value = false
    }
}
