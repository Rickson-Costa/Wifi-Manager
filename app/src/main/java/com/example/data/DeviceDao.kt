package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY firstSeen DESC")
    fun getAllDevices(): Flow<List<NetworkDevice>>
    
    @Query("SELECT * FROM devices WHERE networkId = :networkId OR networkId = '' ORDER BY firstSeen DESC")
    fun getAllDevicesByNetwork(networkId: String): Flow<List<NetworkDevice>>

    @Query("SELECT * FROM devices WHERE macAddress = :mac LIMIT 1")
    fun getDeviceByMac(mac: String): Flow<NetworkDevice?>
    
    @Query("SELECT * FROM devices WHERE macAddress = :mac LIMIT 1")
    suspend fun getDeviceByMacSync(mac: String): NetworkDevice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: NetworkDevice)

    @Query("UPDATE devices SET isBlocked = :isBlocked WHERE macAddress = :mac")
    suspend fun updateBlockStatus(mac: String, isBlocked: Boolean)

    @Query("UPDATE devices SET customName = :name WHERE macAddress = :mac")
    suspend fun updateCustomName(mac: String, name: String)

    @Query("SELECT COUNT(*) FROM devices WHERE isBlocked = 1 AND (networkId = :networkId OR networkId = '')")
    fun getBlockedDeviceCount(networkId: String): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM devices WHERE networkId = :networkId OR networkId = ''")
    fun getTotalDeviceCount(networkId: String): Flow<Int>

    // Events
    @Query("SELECT * FROM network_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<NetworkEvent>>

    @Insert
    suspend fun insertEvent(event: NetworkEvent)

    @Query("SELECT * FROM network_events WHERE macAddress = :mac ORDER BY timestamp DESC")
    fun getEventsForDevice(mac: String): Flow<List<NetworkEvent>>

    @Query("DELETE FROM network_events")
    suspend fun clearAllEvents()
}
