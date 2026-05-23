package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Summarize
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(viewModel: NetworkViewModel, onNavigateToDevice: (String) -> Unit) {
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val blockedCount by viewModel.blockedCount.collectAsStateWithLifecycle()
    val devices by viewModel.allDevices.collectAsStateWithLifecycle()
    val currentSSID by viewModel.currentSSID.collectAsStateWithLifecycle()

    val redirectUrl by viewModel.redirectUrl.collectAsStateWithLifecycle()
    val targetMode by viewModel.targetMode.collectAsStateWithLifecycle()
    val targetDevice by viewModel.targetDevice.collectAsStateWithLifecycle()
    val isRedirectActive by viewModel.isRedirectActive.collectAsStateWithLifecycle()
    val customGateway by viewModel.customGateway.collectAsStateWithLifecycle()

    val bgMidnightOnyx = Color(0xFF0A0A0B)
    val bgDeepSlate = Color(0xFF161618)
    val bgElevatedGrey = Color(0xFF242426)
    val colorElectricCyan = Color(0xFF00E5FF)
    val colorNeonEmerald = Color(0xFF10F093)
    val colorSafetyAmber = Color(0xFFFFB800)
    val colorCrimsonKill = Color(0xFFFF3B30)
    val colorPureWhite = Color(0xFFFFFFFF)
    val colorMutedSilver = Color(0xFFA1A1A6)
    val colorTechnicalGrey = Color(0xFF636366)
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf("SCAN") }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                viewModel.fetchRealSSID()
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Permissão de localização é necessária para ler o SSID no Android.")
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        val fineLocationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineLocationGranted) {
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            viewModel.fetchRealSSID()
        }
    }

    if (showSettingsDialog) {
        var tempGateway by remember { mutableStateOf(customGateway) }
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            containerColor = bgDeepSlate,
            title = { Text("Configurações da Rede", color = colorPureWhite) },
            text = { 
                Column {
                    Text("Configurações avançadas do Gateway.", color = colorMutedSilver, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))
                    OutlinedTextField(
                        value = tempGateway,
                        onValueChange = { tempGateway = it },
                        label = { Text("Gateway da Rede (Ex: 192.168.1.1)", color = colorMutedSilver) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = bgDeepSlate,
                            unfocusedContainerColor = bgDeepSlate,
                            focusedTextColor = colorPureWhite,
                            unfocusedTextColor = colorPureWhite,
                            focusedBorderColor = colorElectricCyan,
                            unfocusedBorderColor = bgElevatedGrey,
                            cursorColor = colorElectricCyan
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.updateCustomGateway(tempGateway)
                    showSettingsDialog = false 
                }) {
                    Text("SALVAR", color = colorElectricCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("CANCELAR", color = colorMutedSilver)
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = bgMidnightOnyx,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 16.dp, start = 24.dp, end = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        "WifiManager Pro",
                        color = colorPureWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        currentSSID,
                        color = colorElectricCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 4.dp).alpha(0.8f).clickable {
                            viewModel.fetchRealSSID()
                        }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(bgDeepSlate, RoundedCornerShape(12.dp))
                            .border(1.dp, bgElevatedGrey, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { 
                                currentTab = "REPORTS"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Summarize, contentDescription = null, tint = colorElectricCyan, modifier = Modifier.size(20.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(bgDeepSlate, RoundedCornerShape(12.dp))
                            .border(1.dp, bgElevatedGrey, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showSettingsDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Settings, contentDescription = null, tint = colorPureWhite, modifier = Modifier.size(20.dp))
                    }
                }
            }

            HorizontalDivider(color = bgDeepSlate)

            // Main Content
            Box(modifier = Modifier.weight(1f)) {
                if (currentTab == "SCAN") {
                    ScanContent(
                        devices, totalCount, blockedCount, bgMidnightOnyx, bgDeepSlate, bgElevatedGrey, colorElectricCyan, colorNeonEmerald,
                        colorSafetyAmber, colorCrimsonKill, colorPureWhite, colorMutedSilver, onNavigateToDevice,
                        onShowSnackbar = { msg -> coroutineScope.launch { snackbarHostState.showSnackbar(msg) } },
                        onReload = {
                            coroutineScope.launch { snackbarHostState.showSnackbar("Sincronização iniciada...") }
                            viewModel.runNetworkScan()
                        },
                        onNavigateToAssets = { currentTab = "ASSETS" }
                    )
                } else if (currentTab == "ASSETS") {
                    AssetsContent(
                        devices, bgDeepSlate, bgElevatedGrey, colorPureWhite, colorElectricCyan, colorCrimsonKill, onNavigateToDevice
                    )
                } else if (currentTab == "LOGS") {
                    LogsContent(
                        viewModel, bgDeepSlate, bgElevatedGrey, colorPureWhite, colorElectricCyan, colorMutedSilver, colorCrimsonKill, colorNeonEmerald
                    )
                } else if (currentTab == "TOOLS") {
                    ToolsContent(viewModel, bgDeepSlate, bgElevatedGrey, colorPureWhite, colorElectricCyan, colorCrimsonKill, colorSafetyAmber)
                } else if (currentTab == "REPORTS") {
                    ReportsContent(devices, viewModel, bgDeepSlate, bgElevatedGrey, colorPureWhite, colorElectricCyan, colorMutedSilver, colorCrimsonKill, bgMidnightOnyx)
                }
            }
            
            // Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgDeepSlate)
                    .border(1.dp, bgElevatedGrey)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    FooterItem(Icons.Outlined.Radar, "SCAN", if (currentTab == "SCAN") colorElectricCyan else colorPureWhite, if (currentTab == "SCAN") 1f else 0.5f) { currentTab = "SCAN" }
                    FooterItem(Icons.Outlined.Dns, "ATIVOS", if (currentTab == "ASSETS") colorElectricCyan else colorPureWhite, if (currentTab == "ASSETS") 1f else 0.5f) { currentTab = "ASSETS" }
                    FooterItem(Icons.Outlined.Build, "FERRAM.", if (currentTab == "TOOLS") colorElectricCyan else colorPureWhite, if (currentTab == "TOOLS") 1f else 0.5f) { currentTab = "TOOLS" }
                    FooterItem(Icons.Outlined.History, "LOGS", if (currentTab == "LOGS") colorElectricCyan else colorPureWhite, if (currentTab == "LOGS") 1f else 0.5f) { currentTab = "LOGS" }
                    FooterItem(Icons.Outlined.Summarize, "RELATS", if (currentTab == "REPORTS") colorElectricCyan else colorPureWhite, if (currentTab == "REPORTS") 1f else 0.5f) { currentTab = "REPORTS" }
                }
                HorizontalDivider(color = bgElevatedGrey)
                Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                    Text("DESENVOLVIDO POR RICKSON HENRIQUE + AI STUDIO • COMPATÍVEL COM LGPD", fontSize = 7.sp, fontFamily = FontFamily.Monospace, color = colorTechnicalGrey, letterSpacing = 2.sp)
                }
            }
        }
    }
}

@Composable
fun FooterItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, alpha: Float, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .alpha(alpha)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp, color = color, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun StatCard(title: String, value: String, valueColor: Color, modifier: Modifier, bgDeepSlate: Color, bgElevatedGrey: Color, colorMutedSilver: Color, borderColor: Color? = null) {
    Box(
        modifier = modifier
            .background(bgDeepSlate, RoundedCornerShape(16.dp))
            .border(
                1.dp,
                bgElevatedGrey,
                RoundedCornerShape(16.dp)
            )
            .then(
                if (borderColor != null) Modifier.border(1.dp, borderColor, RoundedCornerShape(16.dp))
                else Modifier
            )
            .padding(12.dp)
    ) {
        Column {
            Text(title, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = colorMutedSilver)
            Text(value, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = valueColor, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun DeviceCardHighDensity(name: String, ip: String, mac: String, isBlocked: Boolean, vendor: String, onClick: () -> Unit) {
    val bgMidnightOnyx = Color(0xFF0A0A0B)
    val bgDeepSlate = Color(0xFF161618)
    val bgElevatedGrey = Color(0xFF242426)
    val colorElectricCyan = Color(0xFF00E5FF)
    val colorNeonEmerald = Color(0xFF10F093)
    val colorCrimsonKill = Color(0xFFFF3B30)
    val colorPureWhite = Color(0xFFFFFFFF)

    val cardBg = if (isBlocked) Brush.horizontalGradient(listOf(colorCrimsonKill.copy(alpha = 0.05f), bgDeepSlate)) else Brush.horizontalGradient(listOf(bgDeepSlate, bgDeepSlate))
    val cardBorder = if (isBlocked) colorCrimsonKill.copy(alpha = 0.3f) else bgElevatedGrey
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg, RoundedCornerShape(16.dp))
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(bgMidnightOnyx, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                val v = vendor.lowercase()
                val n = name.lowercase()
                val icon = when {
                    v.contains("apple") || v.contains("samsung") || v.contains("motorola") || v.contains("xiaomi") || n.contains("phone") -> Icons.Outlined.Smartphone
                    v.contains("intel") || v.contains("dell") || v.contains("hp") || v.contains("lenovo") || v.contains("asus") || n.contains("pc") || n.contains("desktop") -> Icons.Outlined.Computer
                    v.contains("lg") || v.contains("sony") || v.contains("roku") || n.contains("tv") -> Icons.Outlined.Tv
                    n.contains("router") || n.contains("gateway") -> Icons.Outlined.Router
                    else -> Icons.Outlined.Devices
                }
                Icon(icon, contentDescription = null, tint = if (isBlocked) colorCrimsonKill else colorElectricCyan, modifier = Modifier.size(20.dp))
            }
            
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    name,
                    color = colorPureWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp
                )
                Text(
                    if (isBlocked) "STATUS: BLOQUEADO" else "$ip • $mac",
                    color = if (isBlocked) colorCrimsonKill else colorElectricCyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            if (isBlocked) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(colorCrimsonKill, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Bolt, contentDescription = null, tint = colorPureWhite, modifier = Modifier.size(20.dp))
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(colorNeonEmerald, CircleShape))
                    Box(modifier = Modifier.width(32.dp).height(16.dp).background(bgElevatedGrey, RoundedCornerShape(50))) {
                        Box(modifier = Modifier.padding(4.dp).size(8.dp).background(Color(0xFF636366), CircleShape))
                    }
                }
            }
        }
    }
}

@Composable
fun RadarAnimation(primaryColor: Color, secondaryColor: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val maxRadius = size.minDimension / 2
        
        drawCircle(color = secondaryColor, radius = maxRadius * 0.8f, style = Stroke(width = 1f), alpha = 0.1f)
        drawCircle(color = secondaryColor, radius = maxRadius * 0.5f, style = Stroke(width = 1f), alpha = 0.1f)
        drawCircle(color = secondaryColor, radius = maxRadius * 0.2f, style = Stroke(width = 1f), alpha = 0.1f)
        
        drawCircle(
            color = primaryColor,
            radius = maxRadius * 0.4f,
            style = Stroke(width = 2f),
            alpha = 0.5f
        )
    }
}

@Composable
fun ScanContent(
    devices: List<com.example.data.NetworkDevice>, totalCount: Int, blockedCount: Int, bgMidnightOnyx: Color, bgDeepSlate: Color, bgElevatedGrey: Color, colorElectricCyan: Color, colorNeonEmerald: Color, colorSafetyAmber: Color, colorCrimsonKill: Color, colorPureWhite: Color, colorMutedSilver: Color, onNavigateToDevice: (String) -> Unit, onShowSnackbar: (String) -> Unit, onReload: () -> Unit, onNavigateToAssets: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(bgDeepSlate, RoundedCornerShape(24.dp))
                    .border(1.dp, bgElevatedGrey, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp)),
            ) {
                // Centered Content
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(totalCount.toString(), fontSize = 48.sp, fontWeight = FontWeight.Bold, color = colorPureWhite)
                    Text("DISPOSITIVOS ATIVOS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp, color = colorPureWhite, modifier = Modifier.padding(top = 4.dp))
                    Box(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .background(bgMidnightOnyx.copy(alpha = 0.6f), RoundedCornerShape(50))
                            .border(1.dp, colorElectricCyan.copy(alpha = 0.2f), RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("VARREDURA EM TEMPO REAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colorNeonEmerald, letterSpacing = (-0.2).sp)
                    }
                }
                // Sync Button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(48.dp)
                        .background(colorElectricCyan.copy(alpha = 0.15f), CircleShape)
                        .clip(CircleShape)
                        .clickable(onClick = onReload),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Sync, contentDescription = "Sync", tint = colorElectricCyan)
                }
            }
        }

        // 3 Column Grid Stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(title = "PONTUAÇÃO", value = "85/100", valueColor = colorPureWhite, modifier = Modifier.weight(1f), bgDeepSlate, bgElevatedGrey, colorMutedSilver)
                StatCard(title = "BLOQUEADOS", value = "0${blockedCount}", valueColor = colorCrimsonKill, modifier = Modifier.weight(1f), bgDeepSlate, bgElevatedGrey, colorMutedSilver, borderColor = colorCrimsonKill)
                StatCard(title = "NOVOS", value = "00", valueColor = colorSafetyAmber, modifier = Modifier.weight(1f), bgDeepSlate, bgElevatedGrey, colorMutedSilver)
            }
        }

        // Inventory List
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("INVENTÁRIO DE ATIVOS", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = colorPureWhite)
                Text("Filtrar e Ordenar", modifier = Modifier.clickable { onNavigateToAssets() }, fontSize = 10.sp, color = colorElectricCyan)
            }
        }

        items(devices) { device ->
            DeviceCardHighDensity(
                name = device.customName.ifEmpty { device.hostname },
                ip = device.lastIp,
                mac = device.macAddress,
                isBlocked = device.isBlocked,
                vendor = device.vendor,
                onClick = { onNavigateToDevice(device.macAddress) }
            )
        }
    }
}

@Composable
fun AssetsContent(
    devices: List<com.example.data.NetworkDevice>, bgDeepSlate: Color, bgElevatedGrey: Color, colorPureWhite: Color, colorElectricCyan: Color, colorCrimsonKill: Color, onNavigateToDevice: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf("ALL") } // ALL, BLOCKED, ACTIVE
    var sortMode by remember { mutableStateOf("IP") } // IP, NAME, MAC
    var isSortMenuExpanded by remember { mutableStateOf(false) }
    
    val filteredDevices = devices.filter { device ->
        val matchesSearch = device.customName.contains(searchQuery, ignoreCase = true) ||
                            device.hostname.contains(searchQuery, ignoreCase = true) ||
                            device.lastIp.contains(searchQuery, ignoreCase = true) ||
                            device.macAddress.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (filterMode) {
            "BLOCKED" -> device.isBlocked
            "ACTIVE" -> !device.isBlocked
            else -> true
        }
        matchesSearch && matchesFilter
    }.let { list ->
        when (sortMode) {
            "NAME" -> list.sortedBy { (it.customName.ifEmpty { it.hostname }).lowercase() }
            "MAC" -> list.sortedBy { it.macAddress }
            else -> list.sortedBy { it.lastIp.split('.').lastOrNull()?.toIntOrNull() ?: 0 }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("TODOS OS ATIVOS", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorPureWhite, letterSpacing = 1.sp)
                
                Box {
                    Text("Ordenar: $sortMode", modifier = Modifier.clickable { isSortMenuExpanded = true }, fontSize = 11.sp, color = colorElectricCyan, fontWeight = FontWeight.Bold)
                    DropdownMenu(
                        expanded = isSortMenuExpanded,
                        onDismissRequest = { isSortMenuExpanded = false },
                        modifier = Modifier.background(bgElevatedGrey)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Por IP", color = colorPureWhite) },
                            onClick = { sortMode = "IP"; isSortMenuExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Por Nome", color = colorPureWhite) },
                            onClick = { sortMode = "NAME"; isSortMenuExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Por MAC", color = colorPureWhite) },
                            onClick = { sortMode = "MAC"; isSortMenuExpanded = false }
                        )
                    }
                }
            }
        }
        
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Pesquisar por IP, MAC ou Nome", color = Color.Gray, fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colorPureWhite,
                    unfocusedTextColor = colorPureWhite,
                    focusedBorderColor = colorElectricCyan,
                    unfocusedBorderColor = bgElevatedGrey
                ),
                singleLine = true,
                trailingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = colorElectricCyan) }
            )
        }
        
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                FilterChip(
                    selected = filterMode == "ALL",
                    onClick = { filterMode = "ALL" },
                    label = { Text("Todos", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colorElectricCyan, selectedLabelColor = Color.Black)
                )
                FilterChip(
                    selected = filterMode == "ACTIVE",
                    onClick = { filterMode = "ACTIVE" },
                    label = { Text("Ativos", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colorPureWhite, selectedLabelColor = Color.Black)
                )
                FilterChip(
                    selected = filterMode == "BLOCKED",
                    onClick = { filterMode = "BLOCKED" },
                    label = { Text("Bloqueados", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colorCrimsonKill, selectedLabelColor = Color.White)
                )
            }
        }

        if (filteredDevices.isEmpty()) {
            item {
                Text("Nenhum dispositivo encontrado.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp))
            }
        }
        
        items(filteredDevices) { device ->
            DeviceCardHighDensity(
                name = device.customName.ifEmpty { device.hostname },
                ip = device.lastIp,
                mac = device.macAddress,
                isBlocked = device.isBlocked,
                vendor = device.vendor,
                onClick = { onNavigateToDevice(device.macAddress) }
            )
        }
    }
}

@Composable
fun LogsContent(
    viewModel: NetworkViewModel, bgDeepSlate: Color, bgElevatedGrey: Color, colorPureWhite: Color, colorElectricCyan: Color, colorMutedSilver: Color, colorCrimsonKill: Color, colorNeonEmerald: Color
) {
    val events by viewModel.allEvents.collectAsStateWithLifecycle()
    var filterType by remember { mutableStateOf("ALL") }
    
    val filteredEvents = events.filter { 
        if (filterType == "ALL") true 
        else it.eventType.contains(filterType) 
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("LOGS DA REDE", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorPureWhite, letterSpacing = 1.sp)
                if (events.isNotEmpty()) {
                    Text("Limpar", modifier = Modifier.clickable { viewModel.clearAllLogs() }, fontSize = 11.sp, color = colorCrimsonKill, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                FilterChip(
                    selected = filterType == "ALL",
                    onClick = { filterType = "ALL" },
                    label = { Text("Todos", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colorElectricCyan, selectedLabelColor = Color.Black)
                )
                FilterChip(
                    selected = filterType == "BLOCK",
                    onClick = { filterType = "BLOCK" },
                    label = { Text("Bloqueios", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colorCrimsonKill, selectedLabelColor = Color.White)
                )
                FilterChip(
                    selected = filterType == "DISCOVERY",
                    onClick = { filterType = "DISCOVERY" },
                    label = { Text("Descobertas", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colorPureWhite, selectedLabelColor = Color.Black)
                )
            }
        }
        
        if (filteredEvents.isEmpty()) {
            item {
                Text("Nenhum evento registrado ainda.", color = colorMutedSilver, fontSize = 12.sp)
            }
        }
        items(filteredEvents) { event ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgDeepSlate, RoundedCornerShape(12.dp))
                    .border(1.dp, bgElevatedGrey, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val viewEventType = when (event.eventType) {
                            "BLOCK" -> "BLOQUEIO ARP"
                            "UNBLOCK" -> "ACESSO RESTAURADO"
                            "DISCOVERY" -> "DISPOSITIVO DESCOBERTO"
                            else -> event.eventType
                        }
                        
                        val formatter = java.text.SimpleDateFormat("dd/MM/yy HH:mm", java.util.Locale.getDefault())
                        val dateStr = formatter.format(java.util.Date(event.timestamp))
                        
                        Text(viewEventType, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (event.eventType.contains("BLOCK")) colorCrimsonKill else if (event.eventType.contains("UNBLOCK")) colorNeonEmerald else colorElectricCyan)
                        Text(dateStr, fontSize = 10.sp, color = colorMutedSilver)
                    }
                    Text("MAC: ${event.macAddress}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = colorPureWhite, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

@Composable
fun SafeContent(
    blockedCount: Int, totalCount: Int, bgDeepSlate: Color, bgElevatedGrey: Color, colorPureWhite: Color, colorElectricCyan: Color, colorSafetyAmber: Color, colorCrimsonKill: Color
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("PAINEL DE SEGURANÇA", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorPureWhite, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(bgDeepSlate, RoundedCornerShape(16.dp))
                    .border(1.dp, bgElevatedGrey, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("85 / 100", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = colorPureWhite)
                    Text("PONTUAÇÃO DE SAÚDE", fontSize = 10.sp, color = colorElectricCyan, letterSpacing = 2.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        item {
            Text("Alertas Ativos ($blockedCount)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colorCrimsonKill, modifier = Modifier.padding(top = 16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedirectorContent(
    devices: List<com.example.data.NetworkDevice>,
    bgDeepSlate: Color, bgElevatedGrey: Color, colorPureWhite: Color, colorElectricCyan: Color, colorMutedSilver: Color,
    initialRedirectUrl: String, initialTargetMode: String, initialTargetDevice: String, initialIsActive: Boolean,
    onSaveRedirectRule: (String, String, String, Boolean) -> Unit
) {
    var redirectUrl by remember { mutableStateOf(initialRedirectUrl) }
    var targetMode by remember { mutableStateOf(initialTargetMode) }
    var targetDevice by remember { mutableStateOf(initialTargetDevice) }
    var isActive by remember { mutableStateOf(initialIsActive) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("REDIRECIONADOR", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorPureWhite, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))
        }
        item {
            Text(
                "Configurar captive portal e redirecionamento de DNS da rede.",
                color = colorMutedSilver,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (targetMode == "ALL") colorElectricCyan.copy(alpha = 0.2f) else bgDeepSlate, RoundedCornerShape(8.dp))
                        .border(1.dp, if (targetMode == "ALL") colorElectricCyan else bgElevatedGrey, RoundedCornerShape(8.dp))
                        .clickable { targetMode = "ALL" }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Toda a Rede", color = if (targetMode == "ALL") colorElectricCyan else colorPureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (targetMode == "SPECIFIC") colorElectricCyan.copy(alpha = 0.2f) else bgDeepSlate, RoundedCornerShape(8.dp))
                        .border(1.dp, if (targetMode == "SPECIFIC") colorElectricCyan else bgElevatedGrey, RoundedCornerShape(8.dp))
                        .clickable { targetMode = "SPECIFIC" }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Dispositivo", color = if (targetMode == "SPECIFIC") colorElectricCyan else colorPureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (targetMode == "SPECIFIC") {
            item {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    OutlinedTextField(
                        value = targetDevice,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Selecione o Dispositivo", color = colorMutedSilver) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = bgDeepSlate,
                            unfocusedContainerColor = bgDeepSlate,
                            focusedTextColor = colorPureWhite,
                            unfocusedTextColor = colorPureWhite,
                            focusedBorderColor = colorElectricCyan,
                            unfocusedBorderColor = bgElevatedGrey
                        ),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        containerColor = bgDeepSlate
                    ) {
                        devices.forEach { device ->
                            DropdownMenuItem(
                                text = { 
                                    val name = device.customName.ifEmpty { device.hostname.ifEmpty { "Desconhecido" } }
                                    Text("$name (${device.lastIp})", color = colorPureWhite) 
                                },
                                onClick = {
                                    targetDevice = device.lastIp
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = redirectUrl,
                onValueChange = { redirectUrl = it },
                label = { Text("URL de Redirecionamento", color = colorMutedSilver) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = bgDeepSlate,
                    unfocusedContainerColor = bgDeepSlate,
                    focusedTextColor = colorPureWhite,
                    unfocusedTextColor = colorPureWhite,
                    focusedBorderColor = colorElectricCyan,
                    unfocusedBorderColor = bgElevatedGrey,
                    cursorColor = colorElectricCyan
                ),
                singleLine = true
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .background(bgDeepSlate, RoundedCornerShape(12.dp))
                    .border(1.dp, bgElevatedGrey, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Forçar Redirecionamento", color = colorPureWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(if (targetMode == "ALL") "Restringir toda a rede para este host." else "Restringir dispositivo específico.", color = colorMutedSilver, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
                Switch(
                    checked = isActive,
                    onCheckedChange = { isActive = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colorPureWhite,
                        checkedTrackColor = colorElectricCyan,
                        uncheckedThumbColor = colorMutedSilver,
                        uncheckedTrackColor = bgElevatedGrey
                    )
                )
            }
        }
        if (isActive) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorElectricCyan.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .border(1.dp, colorElectricCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    val targetMsg = if (targetMode == "ALL") "Todo o tráfego externo" else "O tráfego de $targetDevice"
                    Text(
                        "O redirecionador está ATIVO!\n$targetMsg está sendo capturado e redirecionado para:\n\n$redirectUrl\n\n(Simulação ativa localmente. Todo tráfego está sob controle.)",
                        color = colorElectricCyan,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ToolsContent(
    viewModel: NetworkViewModel, bgDeepSlate: Color, bgElevatedGrey: Color, colorPureWhite: Color, colorElectricCyan: Color, colorCrimsonKill: Color, colorSafetyAmber: Color
) {
    val isRooted by viewModel.isDeviceRooted.collectAsStateWithLifecycle()
    var rootMenuExpanded by remember { mutableStateOf(false) }
    var rootCommandResult by remember { mutableStateOf("") }
    var rootCommandLoading by remember { mutableStateOf(false) }

    var ipInput by remember { mutableStateOf("") }
    var pingResult by remember { mutableStateOf("") }
    var pingLoading by remember { mutableStateOf(false) }
    
    var portTarget by remember { mutableStateOf("") }
    var portResult by remember { mutableStateOf<List<String>?>(null) }
    var portLoading by remember { mutableStateOf(false) }
    
    var wolTarget by remember { mutableStateOf("") }
    var wolResult by remember { mutableStateOf("") }
    var wolLoading by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("FERRAMENTAS PROFISSIONAIS", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorPureWhite, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))
        }
        
        // --- TESTE DE PING ---
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgDeepSlate, RoundedCornerShape(12.dp))
                    .border(1.dp, bgElevatedGrey, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Speed, contentDescription = null, tint = colorElectricCyan, modifier = Modifier.size(20.dp))
                        Text(" TESTE DE LATÊNCIA (PING)", color = colorElectricCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = ipInput,
                        onValueChange = { ipInput = it },
                        label = { Text("IP ou Hostname (ex: 8.8.8.8)", color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colorPureWhite,
                            unfocusedTextColor = colorPureWhite,
                            focusedBorderColor = colorElectricCyan,
                            unfocusedBorderColor = bgElevatedGrey
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            pingLoading = true
                            pingResult = ""
                            viewModel.runRealPing(ipInput) { result ->
                                pingResult = result
                                pingLoading = false
                            }
                        },
                        enabled = !pingLoading && ipInput.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colorElectricCyan, contentColor = Color.Black)
                    ) {
                        Text(if (pingLoading) "EXECUTANDO..." else "DISPARAR PING", fontWeight = FontWeight.Bold)
                    }
                    if (pingResult.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(pingResult, color = Color.LightGray, fontFamily = FontFamily.Monospace, fontSize = 10.sp, modifier = Modifier.fillMaxWidth().background(Color.Black, RoundedCornerShape(8.dp)).padding(12.dp))
                    }
                }
            }
        }

        // --- SCANNER DE PORTAS ---
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgDeepSlate, RoundedCornerShape(12.dp))
                    .border(1.dp, bgElevatedGrey, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Search, contentDescription = null, tint = colorElectricCyan, modifier = Modifier.size(20.dp))
                        Text(" SCANNER DE PORTAS (TCP)", color = colorElectricCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = portTarget,
                        onValueChange = { portTarget = it },
                        label = { Text("Alvo IP (ex: 192.168.0.1)", color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colorPureWhite,
                            unfocusedTextColor = colorPureWhite,
                            focusedBorderColor = colorElectricCyan,
                            unfocusedBorderColor = bgElevatedGrey
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            portLoading = true
                            portResult = null
                            viewModel.runRealPortScan(portTarget) { result ->
                                portResult = result
                                portLoading = false
                            }
                        },
                        enabled = !portLoading && portTarget.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colorElectricCyan, contentColor = Color.Black)
                    ) {
                        Text(if (portLoading) "VARRENDO..." else "INICIAR VARREDURA", fontWeight = FontWeight.Bold)
                    }
                    portResult?.let { ports ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(modifier = Modifier.fillMaxWidth().background(Color.Black, RoundedCornerShape(8.dp)).padding(12.dp)) {
                            Text("PORTAS ABERTAS:", color = colorPureWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                            ports.forEach { p ->
                                Text("• Porta $p", color = Color.LightGray, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }
                }
            }
        }
        
        // --- WOL ---
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgDeepSlate, RoundedCornerShape(12.dp))
                    .border(1.dp, bgElevatedGrey, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.PowerSettingsNew, contentDescription = null, tint = colorSafetyAmber, modifier = Modifier.size(20.dp))
                        Text(" WAKE-ON-LAN (WOL)", color = colorSafetyAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = wolTarget,
                        onValueChange = { wolTarget = it },
                        label = { Text("MAC Address (ex: AA:BB:CC:DD:EE:FF)", color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colorPureWhite,
                            unfocusedTextColor = colorPureWhite,
                            focusedBorderColor = colorSafetyAmber,
                            unfocusedBorderColor = bgElevatedGrey
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            wolLoading = true
                            wolResult = ""
                            viewModel.wakeOnLan(wolTarget) { result ->
                                wolResult = result
                                wolLoading = false
                            }
                        },
                        enabled = !wolLoading && wolTarget.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colorSafetyAmber, contentColor = Color.Black)
                    ) {
                        Text(if (wolLoading) "ENVIANDO..." else "ENVIAR MAGIC PACKET", fontWeight = FontWeight.Bold)
                    }
                    if (wolResult.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(wolResult, color = Color.LightGray, fontFamily = FontFamily.Monospace, fontSize = 10.sp, modifier = Modifier.fillMaxWidth().background(Color.Black, RoundedCornerShape(8.dp)).padding(12.dp))
                    }
                }
            }
        }
        
        // --- FUNCOES ROOT ---
        item {
            val rootColor = if (isRooted) colorElectricCyan else colorCrimsonKill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgDeepSlate, RoundedCornerShape(12.dp))
                    .border(1.dp, if(isRooted) rootColor else rootColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Security, contentDescription = null, tint = rootColor, modifier = Modifier.size(20.dp))
                        Text(if(isRooted) " FERRAMENTAS ROOT (ATIVO)" else " FERRAMENTAS ROOT (DESATIVADAS)", color = rootColor, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                    
                    if (!isRooted) {
                        Text("O seu dispositivo não possui permissões necessárias para interagir em baixo nível com o Kernel Linux.", color = colorPureWhite, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                        Text("• Redirecionamento DNS (Captive Portal)\n• Spoofing ARP / Desautenticação WiFi\n• Escuta de tráfego promíscuo (Sniffing)", color = colorPureWhite.copy(alpha=0.6f), fontSize = 11.sp, lineHeight = 16.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { /* Does nothing */ },
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(disabledContainerColor = Color.DarkGray, disabledContentColor = Color.LightGray)
                        ) {
                            Text("MÓDULO DESATIVADO (SEM \"su\")", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text("O módulo Kernel está habilitado. Você tem acesso root de baixo nível.", color = colorPureWhite, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        if (!rootMenuExpanded) {
                            Button(
                                onClick = { rootMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = rootColor, contentColor = Color.Black)
                            ) {
                                Text("GERENCIAR REGRAS E KERNEL", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text("AÇÕES DISPONÍVEIS:", color = colorPureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                            
                            Button(
                                onClick = {
                                    rootCommandLoading = true
                                    rootCommandResult = ""
                                    viewModel.executeRootCommand("iptables -L") { res ->
                                        rootCommandResult = res
                                        rootCommandLoading = false
                                    }
                                },
                                enabled = !rootCommandLoading,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = bgElevatedGrey, contentColor = colorElectricCyan)
                            ) {
                                Text("LISTAR REGRAS IPTABLES")
                            }
                            
                            Button(
                                onClick = {
                                    rootCommandLoading = true
                                    rootCommandResult = ""
                                    viewModel.executeRootCommand("cat /proc/sys/net/ipv4/ip_forward") { res ->
                                        rootCommandResult = res
                                        rootCommandLoading = false
                                    }
                                },
                                enabled = !rootCommandLoading,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = bgElevatedGrey, contentColor = colorElectricCyan)
                            ) {
                                Text("VERIFICAR IP FORWARD")
                            }
                            
                            Button(
                                onClick = {
                                    rootCommandLoading = true
                                    rootCommandResult = ""
                                    viewModel.executeRootCommand("echo 1 > /proc/sys/net/ipv4/ip_forward && echo 'IP Forward Habilitado! (MITM Ativo)'") { res ->
                                        rootCommandResult = res
                                        rootCommandLoading = false
                                    }
                                },
                                enabled = !rootCommandLoading,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B0000), contentColor = colorPureWhite)
                            ) {
                                Text("HABILITAR IP FORWARD (MITM)", fontWeight = FontWeight.Bold)
                            }
                            
                            if (rootCommandResult.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(rootCommandResult, color = Color.LightGray, fontFamily = FontFamily.Monospace, fontSize = 10.sp, modifier = Modifier.fillMaxWidth().background(Color.Black, RoundedCornerShape(8.dp)).padding(12.dp))
                            }
                        }
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun ReportsContent(
    devices: List<com.example.data.NetworkDevice>, 
    viewModel: NetworkViewModel, 
    bgDeepSlate: Color, 
    bgElevatedGrey: Color, 
    colorPureWhite: Color, 
    colorElectricCyan: Color, 
    colorMutedSilver: Color, 
    colorCrimsonKill: Color,
    bgMidnightOnyx: Color
) {
    val events by viewModel.allEvents.collectAsStateWithLifecycle()
    val blockedCount = devices.count { it.isBlocked }
    val activeCount = devices.size - blockedCount

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("RELATÓRIOS E ESTATÍSTICAS", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorPureWhite, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))
        }

        // --- DISPOSITIVOS ATIVOS VS BLOQUEADOS ---
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgDeepSlate, RoundedCornerShape(12.dp))
                    .border(1.dp, bgElevatedGrey, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                        Icon(Icons.Outlined.Analytics, contentDescription = null, tint = colorElectricCyan, modifier = Modifier.size(20.dp))
                        Text(" VISÃO GERAL DE ATIVOS", color = colorElectricCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(devices.size.toString(), color = colorPureWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text("Total", color = colorMutedSilver, fontSize = 10.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(activeCount.toString(), color = colorElectricCyan, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text("Autorizados", color = colorMutedSilver, fontSize = 10.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(blockedCount.toString(), color = colorCrimsonKill, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text("Bloqueados", color = colorMutedSilver, fontSize = 10.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (devices.isNotEmpty()) {
                        val activeRatio = activeCount.toFloat() / devices.size
                        Box(modifier = Modifier.fillMaxWidth().height(12.dp).background(colorCrimsonKill, RoundedCornerShape(6.dp))) {
                            Box(modifier = Modifier.fillMaxWidth(activeRatio).height(12.dp).background(colorElectricCyan, RoundedCornerShape(6.dp)))
                        }
                    }
                }
            }
        }

        // --- DISPOSITIVOS POR FABRICANTE ---
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgDeepSlate, RoundedCornerShape(12.dp))
                    .border(1.dp, bgElevatedGrey, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                        Icon(Icons.Outlined.PieChart, contentDescription = null, tint = colorElectricCyan, modifier = Modifier.size(20.dp))
                        Text(" ATIVOS POR FABRICANTE", color = colorElectricCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                    
                    val vendors = devices.groupBy { it.vendor.ifEmpty { "Desconhecido" } }.mapValues { it.value.size }.toList().sortedByDescending { it.second }.take(8)
                    
                    if (vendors.isEmpty()) {
                        Text("Nenhum dado disponível.", color = colorMutedSilver, fontSize = 12.sp)
                    } else {
                        vendors.forEach { (vendor, count) ->
                            val ratio = count.toFloat() / devices.size
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(vendor, color = colorPureWhite, fontSize = 11.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(bgElevatedGrey, RoundedCornerShape(3.dp)))
                                    Box(modifier = Modifier.fillMaxWidth(ratio).height(6.dp).background(colorElectricCyan, RoundedCornerShape(3.dp)))
                                }
                                Text("$count", color = colorMutedSilver, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
                            }
                        }
                    }
                }
            }
        }
        
        // --- ESTATÍSTICA DE EVENTOS ---
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgDeepSlate, RoundedCornerShape(12.dp))
                    .border(1.dp, bgElevatedGrey, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                        Icon(Icons.Outlined.Warning, contentDescription = null, tint = colorElectricCyan, modifier = Modifier.size(20.dp))
                        Text(" HISTÓRICO DE INCIDENTES", color = colorElectricCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                    
                    val eventStats = events.groupBy { it.eventType }.mapValues { it.value.size }
                    val totalEvents = events.size
                    
                    if (totalEvents == 0) {
                        Text("Nenhum incidente registrado.", color = colorMutedSilver, fontSize = 12.sp)
                    } else {
                        eventStats.forEach { (type, count) ->
                            val ratio = count.toFloat() / totalEvents
                            val displayType = when(type) {
                                "BLOCK" -> "Bloqueios (Ameaças Interceptadas)"
                                "UNBLOCK" -> "Desbloqueios (Acessos Restaurados)"
                                "DISCOVERY" -> "Novos Dispositivos Descobertos"
                                else -> type
                            }
                            val displayColor = if (type == "BLOCK") colorCrimsonKill else if (type == "UNBLOCK") Color(0xFF10F093) else colorElectricCyan
                            
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(displayType, color = colorPureWhite, fontSize = 11.sp, modifier = Modifier.weight(1.5f), maxLines = 1)
                                Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(bgElevatedGrey, RoundedCornerShape(3.dp)))
                                    Box(modifier = Modifier.fillMaxWidth(ratio).height(6.dp).background(displayColor, RoundedCornerShape(3.dp)))
                                }
                                Text("$count", color = colorMutedSilver, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
                            }
                        }
                    }
                }
            }
        }
        
        // --- GERAR RELATÓRIO PDF (Mock) ---
        item {
            Button(
                onClick = { /* MOCK: Generate PDF */ },
                modifier = Modifier.fillMaxWidth().height(56.dp).border(1.dp, colorElectricCyan, RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = bgDeepSlate, contentColor = colorElectricCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("EXPORTAR RELATÓRIO (PDF)", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

