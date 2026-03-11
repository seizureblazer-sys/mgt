package com.example.mgpt.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_session")

class SessionManager(private val context: Context) {
    companion object {
        val USER_ID = stringPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")
        val ROLE = stringPreferencesKey("role")
        val HQ_ID = stringPreferencesKey("hq_id")
    }

    val userSession: Flow<User?> = context.dataStore.data.map { preferences ->
        val id = preferences[USER_ID]
        val username = preferences[USERNAME]
        val roleStr = preferences[ROLE]
        val hqId = preferences[HQ_ID]

        if (id != null && username != null && roleStr != null) {
            User(id, username, UserRole.valueOf(roleStr), hqId)
        } else {
            null
        }
    }

    suspend fun saveSession(user: User) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID] = user.id
            preferences[USERNAME] = user.username
            preferences[ROLE] = user.role.name
            user.hqId?.let { preferences[HQ_ID] = it }
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }
}
