package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class NetworkDevice(
    @PrimaryKey val macAddress: String,
    val customName: String = "",
    val hostname: String = "",
    val vendor: String = "",
    val lastIp: String = "",
    val isBlocked: Boolean = false,
    val isTrusted: Boolean = false,
    val firstSeen: Long = System.currentTimeMillis(),
    val networkId: String = ""
)
