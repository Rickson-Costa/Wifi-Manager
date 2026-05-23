package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "network_events",
    foreignKeys = [
        ForeignKey(
            entity = NetworkDevice::class,
            parentColumns = ["macAddress"],
            childColumns = ["macAddress"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("macAddress")]
)
data class NetworkEvent(
    @PrimaryKey(autoGenerate = true) val eventId: Int = 0,
    val macAddress: String,
    val eventType: String, // "BLOCK", "UNBLOCK", "DISCOVERY", "OFFLINE"
    val origin: String, // "MANUAL", "SYSTEM"
    val timestamp: Long = System.currentTimeMillis()
)
