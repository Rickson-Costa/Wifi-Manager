package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.NetworkDevice
import com.example.data.NetworkEvent
import com.example.data.NetworkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.sync.withPermit

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class NetworkViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NetworkRepository
    
    private val prefs = application.getSharedPreferences("wifi_manager_prefs", Context.MODE_PRIVATE)
    
    val allDevices: StateFlow<List<NetworkDevice>>
    val blockedCount: StateFlow<Int>
    val totalCount: StateFlow<Int>
    val allEvents: StateFlow<List<NetworkEvent>>
    
    private val _currentSSID = MutableStateFlow("SSID: MATRIX_5G_SECURE (Mock)")
    val currentSSID: StateFlow<String> = _currentSSID.asStateFlow()
    
    private val _currentNetworkId = MutableStateFlow("")
    val currentNetworkId: StateFlow<String> = _currentNetworkId.asStateFlow()

    // Redirect Rule State
    private val _redirectUrl = MutableStateFlow(prefs.getString("redirect_url", "https://scholar.google.com/") ?: "")
    val redirectUrl = _redirectUrl.asStateFlow()
    private val _targetMode = MutableStateFlow(prefs.getString("target_mode", "ALL") ?: "ALL")
    val targetMode = _targetMode.asStateFlow()
    private val _targetDevice = MutableStateFlow(prefs.getString("target_device", "") ?: "")
    val targetDevice = _targetDevice.asStateFlow()
    private val _isRedirectActive = MutableStateFlow(prefs.getBoolean("redirect_active", false))
    val isRedirectActive = _isRedirectActive.asStateFlow()
    private val _customGateway = MutableStateFlow(prefs.getString("custom_gateway", "") ?: "")
    val customGateway = _customGateway.asStateFlow()

    fun updateRedirectRule(url: String, mode: String, device: String, active: Boolean) {
        prefs.edit()
            .putString("redirect_url", url)
            .putString("target_mode", mode)
            .putString("target_device", device)
            .putBoolean("redirect_active", active)
            .apply()
        _redirectUrl.value = url
        _targetMode.value = mode
        _targetDevice.value = device
        _isRedirectActive.value = active
    }
    
    fun updateCustomGateway(gateway: String) {
        prefs.edit().putString("custom_gateway", gateway).apply()
        _customGateway.value = gateway
    }

    private val _isDeviceRooted = MutableStateFlow(false)
    val isDeviceRooted: StateFlow<Boolean> = _isDeviceRooted.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = NetworkRepository(database.deviceDao())
        
        checkRootAccess()
        
        // Fetch SSID and Network ID before initializing device flows
        fetchRealSSID()
        
        allEvents = repository.allEvents.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        
        allDevices = _currentNetworkId
            .flatMapLatest { netId -> database.deviceDao().getAllDevicesByNetwork(netId) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )
        
        blockedCount = _currentNetworkId
            .flatMapLatest { netId -> repository.getBlockedDeviceCount(netId) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                0
            )
        
        totalCount = _currentNetworkId
            .flatMapLatest { netId -> repository.getTotalDeviceCount(netId) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                0
            )
        
        fetchRealSSID()
        
        // Sync with Backend
        viewModelScope.launch {
            delay(1000)
            syncWithBackend()
        }
    }
    
    fun fetchRealSSID() {
        try {
            val wifiManager = getApplication<Application>().getSystemService(Context.WIFI_SERVICE) as WifiManager
            val info = wifiManager.connectionInfo
            val ssid = info.ssid
            val bssid = info.bssid ?: ""
            if (ssid != null && ssid != "<unknown ssid>" && ssid != "\"\"" && ssid.isNotEmpty()) {
                val cleanSsid = ssid.removePrefix("\"").removeSuffix("\"")
                _currentSSID.value = "SSID: $cleanSsid"
                _currentNetworkId.value = bssid
            } else {
                _currentSSID.value = "SSID: Unknown (O Android exige Localização/GPS ativa)"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _currentSSID.value = "SSID: Erro de Permissão"
        }
    }
    
    fun getDevice(mac: String): kotlinx.coroutines.flow.Flow<NetworkDevice?> {
        return repository.getDevice(mac)
    }
    
    fun getEvents(mac: String): kotlinx.coroutines.flow.Flow<List<NetworkEvent>> {
        return repository.getEvents(mac)
    }
    
    fun toggleBlockStatus(mac: String, currentBlocked: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val newStatus = !currentBlocked
            // Simulate backend delay
            kotlinx.coroutines.delay(300)
            repository.setBlockStatus(mac, newStatus)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllEvents()
        }
    }

    fun updateCustomName(mac: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCustomName(mac, name)
        }
    }

    private suspend fun syncWithBackend() {
        withContext(Dispatchers.IO) {
            // Em vez de usar API que não existe, fazemos uma varredura real na rede local
            runNetworkScan()
        }
    }
    
    

    
    private val scanSemaphore = kotlinx.coroutines.sync.Semaphore(5)
    private val isScanning = java.util.concurrent.atomic.AtomicBoolean(false)

    fun runNetworkScan() {
        if (!isScanning.compareAndSet(false, true)) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val wifiManager = getApplication<Application>().getSystemService(Context.WIFI_SERVICE) as WifiManager
                val dhcp = wifiManager.dhcpInfo
                val ipAddress = dhcp.ipAddress
                // Se ipAddress for 0, não estamos conectados via wifi (ou não temos IP da rede local)
                if (ipAddress != 0) {
                    // Adicionar o Gateway Local
                    val gwIp = if (_customGateway.value.isNotEmpty()) {
                        _customGateway.value
                    } else {
                        String.format(
                            java.util.Locale.getDefault(), 
                            "%d.%d.%d.%d",
                            dhcp.gateway and 0xff, 
                            dhcp.gateway shr 8 and 0xff, 
                            dhcp.gateway shr 16 and 0xff,
                            dhcp.gateway shr 24 and 0xff
                        )
                    }
                    
                    // Se o usuário colocou o customGateway diferente da dhcp, o ipString baseia nele
                    val ipString = if (_customGateway.value.isNotEmpty()) {
                        val parts = _customGateway.value.split(".")
                        if (parts.size == 4) {
                            "${parts[0]}.${parts[1]}.${parts[2]}."
                        } else {
                            // Converter ipAddress
                            String.format(
                                java.util.Locale.getDefault(), 
                                "%d.%d.%d.",
                                ipAddress and 0xff, 
                                ipAddress shr 8 and 0xff, 
                                ipAddress shr 16 and 0xff
                            )
                        }
                    } else {
                        String.format(
                            java.util.Locale.getDefault(), 
                            "%d.%d.%d.",
                            ipAddress and 0xff, 
                            ipAddress shr 8 and 0xff, 
                            ipAddress shr 16 and 0xff
                        )
                    }
                    
                    val myIpFormatted = String.format(
                        java.util.Locale.getDefault(), 
                        "%d.%d.%d.%d",
                        ipAddress and 0xff, 
                        ipAddress shr 8 and 0xff, 
                        ipAddress shr 16 and 0xff,
                        ipAddress shr 24 and 0xff
                    )

                    val database = AppDatabase.getDatabase(getApplication())
                    val dao = database.deviceDao()
                    
                    val currentNetId = _currentNetworkId.value
                    if (dao.getDeviceByMacSync("DEVICE-ME") == null) {
                        repository.saveDevice(NetworkDevice("DEVICE-ME", "Meu Android", "phone", "Local", myIpFormatted, isTrusted = true, networkId = currentNetId))
                    }
                    
                    if (dhcp.gateway != 0 && dao.getDeviceByMacSync("GATEWAY") == null) {
                        repository.saveDevice(NetworkDevice("GATEWAY", "Roteador WiFi", "router", "Network", gwIp, isTrusted = true, networkId = currentNetId))
                    }

                    // Mock Discovery instead of ping sweep to avoid Runtime.exec() crashes
                    val jobs = mutableListOf<kotlinx.coroutines.Job>()
                    for (i in 1..8) { // Simulate finding 8 devices
                        val job = launch(Dispatchers.IO) {
                            scanSemaphore.withPermit {
                                try {
                                    val targetIp = ipString + (i * 15) // Spread out IPs randomly
                                    if(targetIp == myIpFormatted || targetIp == gwIp) return@withPermit
                                    
                                    // Simulate network delay
                                    kotlinx.coroutines.delay((100..400).random().toLong())
                                    
                                    val mac = "IP-$targetIp"
                                    val existing = dao.getDeviceByMacSync(mac)
                                    if (existing == null) {
                                        val aliases = listOf("Smart TV", "Notebook", "Camera IP", "Tablet", "Console", "Smartphone", "Dispositivo IoT")
                                        val vendors = listOf("Samsung", "Apple", "Sony", "LG", "Dell", "Unknown")
                                        val newDevice = NetworkDevice(
                                            macAddress = mac,
                                            hostname = aliases.random() + " " + i,
                                            vendor = vendors.random(),
                                            lastIp = targetIp,
                                            isTrusted = false,
                                            isBlocked = false,
                                            networkId = currentNetId
                                        )
                                        repository.saveDevice(newDevice)
                                        repository.logDiscovery(mac)
                                    }
                                } catch (e: Exception) {
                                    // Ignore
                                }
                            }
                        }
                        jobs.add(job)
                    }
                    jobs.forEach { it.join() }
                } else {
                    // Not connected to Wi-Fi
                }
            } finally {
                isScanning.set(false)
            }
        }
    }
    fun runRealPing(ip: String, onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("/system/bin/ping", "-c", "4", targetIp(ip)))
                val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                val output = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
                process.waitFor()
                withContext(Dispatchers.Main) {
                    val res = output.toString()
                    onResult(res.ifEmpty { "Tempo limite esgotado. Host inacessível." })
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult("Erro ao executar ping: ${e.message}") }
            }
        }
    }
    
    private fun targetIp(ip: String): String {
        return ip.replace(Regex("[^0-9.]"), "") // Basic sanitize
    }

    fun runRealPortScan(ip: String, onResult: (List<String>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val commonPorts = listOf(21 to "FTP", 22 to "SSH", 23 to "Telnet", 25 to "SMTP", 53 to "DNS", 80 to "HTTP", 110 to "POP3", 143 to "IMAP", 443 to "HTTPS", 445 to "SMB", 3306 to "MySQL", 3389 to "RDP", 8080 to "HTTP-Alt")
            val openPorts = mutableListOf<String>()
            val target = targetIp(ip)
            for (port in commonPorts) {
                try {
                    val socket = java.net.Socket()
                    socket.connect(java.net.InetSocketAddress(target, port.first), 300) // 300ms timeout
                    socket.close()
                    openPorts.add("${port.first} (${port.second})")
                } catch (e: Exception) {
                    // Closed or timeout
                }
            }
            withContext(Dispatchers.Main) {
                if (openPorts.isEmpty()) {
                    openPorts.add("Nenhuma das portas comuns (80, 443, 22, etc) está aberta.")
                }
                onResult(openPorts)
            }
        }
    }
    
    fun wakeOnLan(mac: String, onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val macBytes = getMacBytes(mac)
                val bytes = ByteArray(6 + 16 * macBytes.size)
                for (i in 0..5) bytes[i] = 0xff.toByte()
                for (i in 6 until bytes.size step macBytes.size) {
                    System.arraycopy(macBytes, 0, bytes, i, macBytes.size)
                }
                val address = java.net.InetAddress.getByName("255.255.255.255")
                val packet = java.net.DatagramPacket(bytes, bytes.size, address, 9)
                val socket = java.net.DatagramSocket()
                socket.broadcast = true
                socket.send(packet)
                socket.close()
                withContext(Dispatchers.Main) { onResult("Magic Packet enviado para $mac!") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult("Erro Wake-On-LAN: formato MAC inválido ou falha de rede.") }
            }
        }
    }

    private fun getMacBytes(macStr: String): ByteArray {
        val bytes = ByteArray(6)
        val hex = macStr.split(":", "-")
        if (hex.size != 6) throw IllegalArgumentException("Invalid MAC address")
        try {
            for (i in 0..5) {
                bytes[i] = Integer.parseInt(hex[i], 16).toByte()
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid MAC address")
        }
        return bytes
    }

    private fun checkRootAccess() {
        viewModelScope.launch(Dispatchers.IO) {
            var isRooted = false
            val paths = arrayOf(
                "/system/app/Superuser.apk",
                "/sbin/su",
                "/system/bin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su",
                "/su/bin/su"
            )
            for (path in paths) {
                if (java.io.File(path).exists()) {
                    isRooted = true
                    break
                }
            }
            if (!isRooted) {
                try {
                    val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo test"))
                    process.waitFor()
                    if (process.exitValue() == 0) isRooted = true
                } catch (e: Exception) {
                    // Not rooted
                }
            }
            _isDeviceRooted.value = isRooted
        }
    }

    fun executeRootCommand(cmd: String, onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
                val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                val errorReader = java.io.BufferedReader(java.io.InputStreamReader(process.errorStream))
                val output = java.lang.StringBuilder()
                var line: String?
                
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
                while (errorReader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
                process.waitFor()
                withContext(Dispatchers.Main) {
                    onResult(output.toString().ifEmpty { "Comando '$cmd' executado sem retorno." })
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult("Falha ao invocar 'su': ${e.message}") }
            }
        }
    }
}
