package com.example.mgpt.network

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.URISyntaxException

object SocketManager {
    private var socket: Socket? = null
    private const val TAG = "SocketManager"

    fun connect(url: String, onConnect: () -> Unit) {
        try {
            socket = IO.socket(url)
            socket?.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Connected to server")
                onConnect()
            }
            socket?.on(Socket.EVENT_DISCONNECT) {
                Log.d(TAG, "Disconnected from server")
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
    }
}
