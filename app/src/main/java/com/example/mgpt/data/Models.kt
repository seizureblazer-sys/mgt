package com.example.mgpt.data

import java.util.Date

enum class UserRole {
    SUPER_ADMIN, OPERATOR, PATROL
}

enum class UnitType {
    INFANTRY, VEHICLE, DRONE, K9
}

enum class IncidentType {
    HOSTILE, IED, MEDICAL, LOGISTICS
}

enum class IncidentPriority {
    CRITICAL, HIGH, LOW
}

enum class IncidentStatus {
    ACTIVE, RESOLVED
}

data class User(
    val id: String,
    val username: String,
    val role: UserRole,
    val hqId: String? = null
)

data class PatrolUnit(
    val id: String,
    val lat: Double,
    val lng: Double,
    val role: UserRole
)

data class Incident(
    val id: String,
    val type: IncidentType,
    val priority: IncidentPriority,
    val lat: Double,
    val lng: Double,
    val description: String,
    val reportedBy: String,
    val hqId: String,
    val status: IncidentStatus,
    val createdAt: Long = System.currentTimeMillis()
)
