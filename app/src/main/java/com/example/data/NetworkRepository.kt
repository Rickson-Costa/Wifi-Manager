package com.example.data

import kotlinx.coroutines.flow.Flow

class NetworkRepository(private val dao: DeviceDao) {
    fun getAllDevices(networkId: String): Flow<List<NetworkDevice>> = dao.getAllDevicesByNetwork(networkId)
    fun getBlockedDeviceCount(networkId: String): Flow<Int> = dao.getBlockedDeviceCount(networkId)
    fun getTotalDeviceCount(networkId: String): Flow<Int> = dao.getTotalDeviceCount(networkId)
    val allEvents: Flow<List<NetworkEvent>> = dao.getAllEvents()

    fun getDevice(mac: String): Flow<NetworkDevice?> = dao.getDeviceByMac(mac)
    
    fun getEvents(mac: String): Flow<List<NetworkEvent>> = dao.getEventsForDevice(mac)
    
    suspend fun clearAllEvents() = dao.clearAllEvents()

    suspend fun saveDevice(device: NetworkDevice) = dao.insertDevice(device)
    
    suspend fun updateCustomName(mac: String, name: String) {
        dao.updateCustomName(mac, name)
        dao.insertEvent(
            NetworkEvent(
                macAddress = mac,
                eventType = "NAME_UPDATE",
                origin = "MANUAL"
            )
        )
    }
    
    suspend fun setBlockStatus(mac: String, isBlocked: Boolean) {
        val device = dao.getDeviceByMacSync(mac) ?: return
        dao.updateBlockStatus(mac, isBlocked)
        
        dao.insertEvent(
            NetworkEvent(
                macAddress = mac,
                eventType = if (isBlocked) "BLOCK" else "UNBLOCK",
                origin = "MANUAL"
            )
        )
    }
    
    suspend fun logDiscovery(mac: String) {
        dao.insertEvent(
            NetworkEvent(
                macAddress = mac,
                eventType = "DISCOVERY",
                origin = "SYSTEM"
            )
        )
    }
}
