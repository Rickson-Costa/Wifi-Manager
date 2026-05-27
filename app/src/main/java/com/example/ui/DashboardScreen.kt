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
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ArrowBack
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
                viewModel.runNetworkScan()
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
            title = { Text("Menu de Configurações", color = colorPureWhite) },
            text = { 
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("Gateway da Rede.", color = colorMutedSilver, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
                        OutlinedTextField(
                            value = tempGateway,
                            onValueChange = { tempGateway = it },
                            placeholder = { Text("Ex: 192.168.1.1", color = colorMutedSilver) },
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
                    
                    HorizontalDivider(color = bgElevatedGrey)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { 
                            currentTab = "LOGS" 
                            showSettingsDialog = false 
                        }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.History, contentDescription = null, tint = colorElectricCyan)
                        Text(" Visualizar Logs de Eventos", color = colorPureWhite, modifier = Modifier.padding(start=8.dp))
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { 
                            currentTab = "REPORTS" 
                            showSettingsDialog = false 
                        }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Summarize, contentDescription = null, tint = colorElectricCyan)
                        Text(" Relatórios e Estatísticas", color = colorPureWhite, modifier = Modifier.padding(start=8.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.updateCustomGateway(tempGateway)
                    showSettingsDialog = false 
                }) {
                    Text("SALVAR CONFS", color = colorElectricCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("FECHAR", color = colorMutedSilver)
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = bgMidnightOnyx,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgDeepSlate)
                    .padding(top = innerPadding.calculateTopPadding() + 16.dp, bottom = 16.dp, start = 24.dp, end = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Rede Block",
                        color = colorElectricCyan,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        currentSSID,
                        color = colorMutedSilver,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(top = 4.dp).clickable {
                            viewModel.fetchRealSSID()
                        }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(bgElevatedGrey, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showSettingsDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Settings, contentDescription = null, tint = colorElectricCyan, modifier = Modifier.size(20.dp))
                    }
                }
            }

            HorizontalDivider(color = colorElectricCyan.copy(alpha = 0.15f))

            // Main Content
            Box(modifier = Modifier.weight(1f)) {
                val isScanningState by viewModel.isScanningState.collectAsStateWithLifecycle()
                val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()

                if (currentTab == "SCAN") {
                    ScanContent(
                        devices, totalCount, blockedCount, bgMidnightOnyx, bgDeepSlate, bgElevatedGrey, colorElectricCyan, colorNeonEmerald,
                        colorSafetyAmber, colorCrimsonKill, colorPureWhite, colorMutedSilver, 
                        isScanning = isScanningState, scanProgress = scanProgress,
                        onNavigateToDevice = onNavigateToDevice,
                        onShowSnackbar = { msg -> coroutineScope.launch { snackbarHostState.showSnackbar(msg) } },
                        onReload = {
                            coroutineScope.launch { snackbarHostState.showSnackbar("Sincronização iniciada...") }
                            viewModel.runNetworkScan()
                        },
                        onNavigateToAssets = { currentTab = "ASSETS" }
                    )
                } else if (currentTab == "ASSETS") {
                    AssetsContent(
                        devices, currentSSID, bgDeepSlate, bgElevatedGrey, colorPureWhite, colorElectricCyan, colorCrimsonKill, onNavigateToDevice
                    )
                } else if (currentTab == "LOGS") {
                    LogsContent(
                        viewModel, bgDeepSlate, bgElevatedGrey, colorPureWhite, colorElectricCyan, colorMutedSilver, colorCrimsonKill, colorNeonEmerald
                    )
                } else if (currentTab == "TOOLS") {
                    ToolsContent(devices, viewModel, bgDeepSlate, bgElevatedGrey, colorPureWhite, colorElectricCyan, colorCrimsonKill, colorSafetyAmber, colorMutedSilver, onShowSnackbar = { msg -> coroutineScope.launch { snackbarHostState.showSnackbar(msg) } })
                } else if (currentTab == "REPORTS") {
                    ReportsContent(devices, viewModel, bgDeepSlate, bgElevatedGrey, colorPureWhite, colorElectricCyan, colorMutedSilver, colorCrimsonKill, bgMidnightOnyx)
                }
            }
            
            // Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgDeepSlate)
                    .border(width = 1.dp, color = bgElevatedGrey, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = innerPadding.calculateBottomPadding() + 8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceAround) {
                    FooterItem(Icons.Outlined.Radar, "SCAN", if (currentTab == "SCAN") colorElectricCyan else colorMutedSilver, if (currentTab == "SCAN") 1f else 0.6f) { currentTab = "SCAN" }
                    FooterItem(Icons.Outlined.Dns, "ATIVOS", if (currentTab == "ASSETS") colorElectricCyan else colorMutedSilver, if (currentTab == "ASSETS") 1f else 0.6f) { currentTab = "ASSETS" }
                    FooterItem(Icons.Outlined.Build, "FERRAM.", if (currentTab == "TOOLS") colorElectricCyan else colorMutedSilver, if (currentTab == "TOOLS") 1f else 0.6f) { currentTab = "TOOLS" }
                    FooterItem(Icons.Outlined.History, "LOGS", if (currentTab == "LOGS") colorElectricCyan else colorMutedSilver, if (currentTab == "LOGS") 1f else 0.6f) { currentTab = "LOGS" }
                    FooterItem(Icons.Outlined.PieChart, "CONFS", if (currentTab == "REPORTS") colorElectricCyan else colorMutedSilver, if (currentTab == "REPORTS") 1f else 0.6f) { currentTab = "REPORTS" }
                }
                HorizontalDivider(color = bgElevatedGrey)
                Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                    Text("DESENVOLVIDO POR RICKSON HENRIQUE", fontSize = 7.sp, fontFamily = FontFamily.Monospace, color = colorMutedSilver.copy(alpha = 0.5f), letterSpacing = 2.sp)
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
    devices: List<com.example.data.NetworkDevice>, totalCount: Int, blockedCount: Int, bgMidnightOnyx: Color, bgDeepSlate: Color, bgElevatedGrey: Color, colorElectricCyan: Color, colorNeonEmerald: Color, colorSafetyAmber: Color, colorCrimsonKill: Color, colorPureWhite: Color, colorMutedSilver: Color, isScanning: Boolean, scanProgress: Float, onNavigateToDevice: (String) -> Unit, onShowSnackbar: (String) -> Unit, onReload: () -> Unit, onNavigateToAssets: () -> Unit = {}
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
                    if (isScanning) {
                        CircularProgressIndicator(
                            progress = { scanProgress },
                            modifier = Modifier.size(48.dp),
                            color = colorElectricCyan,
                            trackColor = bgElevatedGrey,
                        )
                        Text("${(scanProgress * 100).toInt()}%", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colorPureWhite, modifier = Modifier.padding(top = 8.dp))
                        Text("BUSCANDO DA REDE", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp, color = colorElectricCyan, modifier = Modifier.padding(top = 4.dp))
                    } else {
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
                }
                // Sync Button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(48.dp)
                        .background(colorElectricCyan.copy(alpha = 0.15f), CircleShape)
                        .clip(CircleShape)
                        .clickable(onClick = { if (!isScanning) onReload() }),
                    contentAlignment = Alignment.Center
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(2.dp), color = colorElectricCyan, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.Sync, contentDescription = "Sync", tint = colorElectricCyan)
                    }
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
    devices: List<com.example.data.NetworkDevice>, currentSSID: String, bgDeepSlate: Color, bgElevatedGrey: Color, colorPureWhite: Color, colorElectricCyan: Color, colorCrimsonKill: Color, onNavigateToDevice: (String) -> Unit
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

    // Grouping
    val groupedDevices = filteredDevices.groupBy { it.vendor.ifEmpty { "Desconhecido" } }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("REDE: ${currentSSID.replace("SSID: ", "")}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorPureWhite, letterSpacing = 1.sp)
                
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

        if (groupedDevices.isEmpty()) {
            item {
                Text("Nenhum dispositivo encontrado.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp))
            }
        } else {
            groupedDevices.forEach { (vendor, deviceList) ->
                item {
                    Text(
                        text = "TIPO: ${vendor.uppercase()}", 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = colorElectricCyan,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                
                items(deviceList) { device ->
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsContent(
    devices: List<com.example.data.NetworkDevice>, viewModel: NetworkViewModel, bgDeepSlate: Color, bgElevatedGrey: Color, colorPureWhite: Color, colorElectricCyan: Color, colorCrimsonKill: Color, colorSafetyAmber: Color, colorMutedSilver: Color, onShowSnackbar: (String) -> Unit
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

    // State for Root Tools
    var rootActionType by remember { mutableStateOf("KILL") } // KILL or REDIRECT
    var rootTargetMode by remember { mutableStateOf("SPECIFIC") } // ALL or SPECIFIC
    var rootTargetDevice by remember { mutableStateOf("") }
    var rootRedirectUrl by remember { mutableStateOf("http://captiveportal.local") }
    var isRootActionActive by remember { mutableStateOf(false) }

    var selectedApp by remember { mutableStateOf<String?>(null) }

    if (selectedApp == null) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("FERRAMENTAS PROFISSIONAIS", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorPureWhite, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 16.dp))
            
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    AppCard("Latência", "Tempo de resposta de servidores e IPs", Icons.Outlined.Speed, colorElectricCyan, bgDeepSlate, bgElevatedGrey) { selectedApp = "PING" }
                }
                item {
                    AppCard("Scanner", "Faça a varredura TCP em busca de portas", Icons.Outlined.Search, colorElectricCyan, bgDeepSlate, bgElevatedGrey) { selectedApp = "PORTS" }
                }
                item {
                    AppCard("Wake-On-LAN", "Envie pacote mágico (Magic Packet)", Icons.Outlined.PowerSettingsNew, colorSafetyAmber, bgDeepSlate, bgElevatedGrey) { selectedApp = "WOL" }
                }
                item {
                    val rootColor = if (isRooted) colorElectricCyan else colorCrimsonKill
                    AppCard("Avançado", "Controles de iptables e Kernel (Root)", Icons.Outlined.Security, rootColor, bgDeepSlate, bgElevatedGrey) { selectedApp = "ROOT" }
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).clickable { selectedApp = null }) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Voltar", tint = colorElectricCyan, modifier = Modifier.size(24.dp))
                Text(" VOLTAR", color = colorElectricCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
            }

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                if (selectedApp == "PING") {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().background(bgDeepSlate, RoundedCornerShape(12.dp)).border(1.dp, bgElevatedGrey, RoundedCornerShape(12.dp)).padding(16.dp)) {
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
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colorPureWhite, unfocusedTextColor = colorPureWhite, focusedBorderColor = colorElectricCyan, unfocusedBorderColor = bgElevatedGrey),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { pingLoading = true; pingResult = ""; viewModel.runRealPing(ipInput) { result -> pingResult = result; pingLoading = false } },
                                    enabled = !pingLoading && ipInput.isNotEmpty(),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = colorElectricCyan, contentColor = Color.Black)
                                ) { Text(if (pingLoading) "EXECUTANDO..." else "DISPARAR PING", fontWeight = FontWeight.Bold) }
                                if (pingResult.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(pingResult, color = Color.LightGray, fontFamily = FontFamily.Monospace, fontSize = 10.sp, modifier = Modifier.fillMaxWidth().background(Color.Black, RoundedCornerShape(8.dp)).padding(12.dp))
                                }
                            }
                        }
                    }
                }

                if (selectedApp == "PORTS") {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().background(bgDeepSlate, RoundedCornerShape(12.dp)).border(1.dp, bgElevatedGrey, RoundedCornerShape(12.dp)).padding(16.dp)) {
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
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colorPureWhite, unfocusedTextColor = colorPureWhite, focusedBorderColor = colorElectricCyan, unfocusedBorderColor = bgElevatedGrey),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { portLoading = true; portResult = null; viewModel.runRealPortScan(portTarget) { result -> portResult = result; portLoading = false } },
                                    enabled = !portLoading && portTarget.isNotEmpty(),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = colorElectricCyan, contentColor = Color.Black)
                                ) { Text(if (portLoading) "VARRENDO..." else "INICIAR VARREDURA", fontWeight = FontWeight.Bold) }
                                portResult?.let { ports ->
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Column(modifier = Modifier.fillMaxWidth().background(Color.Black, RoundedCornerShape(8.dp)).padding(12.dp)) {
                                        Text("PORTAS ABERTAS:", color = colorPureWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                                        ports.forEach { p -> Text("• Porta $p", color = Color.LightGray, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp)) }
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedApp == "WOL") {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().background(bgDeepSlate, RoundedCornerShape(12.dp)).border(1.dp, bgElevatedGrey, RoundedCornerShape(12.dp)).padding(16.dp)) {
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
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colorPureWhite, unfocusedTextColor = colorPureWhite, focusedBorderColor = colorSafetyAmber, unfocusedBorderColor = bgElevatedGrey),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { wolLoading = true; wolResult = ""; viewModel.wakeOnLan(wolTarget) { result -> wolResult = result; wolLoading = false } },
                                    enabled = !wolLoading && wolTarget.isNotEmpty(),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = colorSafetyAmber, contentColor = Color.Black)
                                ) { Text(if (wolLoading) "ENVIANDO..." else "ENVIAR MAGIC PACKET", fontWeight = FontWeight.Bold) }
                                if (wolResult.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(wolResult, color = Color.LightGray, fontFamily = FontFamily.Monospace, fontSize = 10.sp, modifier = Modifier.fillMaxWidth().background(Color.Black, RoundedCornerShape(8.dp)).padding(12.dp))
                                }
                            }
                        }
                    }
                }

                if (selectedApp == "ROOT") {
                    item {
                        val rootColor = if (isRooted) colorElectricCyan else colorCrimsonKill
                        Box(modifier = Modifier.fillMaxWidth().background(bgDeepSlate, RoundedCornerShape(12.dp)).border(1.dp, if(isRooted) rootColor else rootColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).padding(16.dp)) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Security, contentDescription = null, tint = rootColor, modifier = Modifier.size(20.dp))
                                    Text(if(isRooted) " FERRAMENTAS ROOT (ATIVO)" else " FERRAMENTAS ROOT (DESATIVADAS)", color = rootColor, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                                }
                                
                                if (!isRooted) {
                                    Text("O seu dispositivo não possui permissões necessárias para interagir em baixo nível com o Kernel Linux.", color = colorPureWhite, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                                    Text("• Redirecionamento DNS (Captive Portal)\n• Spoofing ARP / Desautenticação WiFi\n• Escuta de tráfego promíscuo (Sniffing)", color = colorPureWhite.copy(alpha=0.6f), fontSize = 11.sp, lineHeight = 16.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(onClick = { }, enabled = false, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(disabledContainerColor = Color.DarkGray, disabledContentColor = Color.LightGray)) {
                                        Text("MÓDULO DESATIVADO (SEM \"su\")", fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Text("O módulo Kernel está habilitado. Você tem acesso root de baixo nível.", color = colorPureWhite, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    if (!rootMenuExpanded) {
                                        Button(onClick = { rootMenuExpanded = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = rootColor, contentColor = Color.Black)) {
                                            Text("GERENCIAR REGRAS E KERNEL", fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Text("AÇÕES DISPONÍVEIS:", color = colorPureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                                        
                                        val isIpForward by viewModel.ipForwardEnabled.collectAsStateWithLifecycle()
                                        val rootToolLogs by viewModel.rootToolLogs.collectAsStateWithLifecycle()

                                        Box(modifier = Modifier.fillMaxWidth().background(colorSafetyAmber.copy(alpha = 0.15f), RoundedCornerShape(8.dp)).padding(12.dp)) {
                                            Text("⚠️ Aviso: As funções de interceptação (WIFI KILL, Redirect, Sniffer) configuram o firewall. Elas SÓ funcionam se o tráfego do alvo passar pelo seu celular (Ataque ARP Spoofing deve estar ativo via App como zANTI/cSploit ou seu celular ser o Roteador da rede).", color = colorSafetyAmber, fontSize = 10.sp, lineHeight = 14.sp)
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))

                                        Row(modifier = Modifier.fillMaxWidth().background(bgDeepSlate, RoundedCornerShape(8.dp)).border(1.dp, bgElevatedGrey, RoundedCornerShape(8.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("IP Forward (MITM Proxy)", color = if(isIpForward) colorElectricCyan else colorPureWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text("Atua como um Roteador nativo, permitindo que o tráfego interceptado circule em vez de cair. Essencial para Man-In-The-Middle.", color = Color.LightGray, fontSize = 10.sp, lineHeight = 12.sp, modifier = Modifier.padding(top = 4.dp))
                                            }
                                            Switch(
                                                checked = isIpForward,
                                                onCheckedChange = { viewModel.setIpForward(it) },
                                                colors = SwitchDefaults.colors(checkedThumbColor = bgDeepSlate, checkedTrackColor = colorElectricCyan, uncheckedThumbColor = bgDeepSlate, uncheckedTrackColor = Color.Gray)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))

                                        val isArpSpoofing by viewModel.isArpSpoofing.collectAsStateWithLifecycle()
                                        Row(modifier = Modifier.fillMaxWidth().background(bgDeepSlate, RoundedCornerShape(8.dp)).border(1.dp, bgElevatedGrey, RoundedCornerShape(8.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Ataque ARP Spoofing (Nativo)", color = if(isArpSpoofing) colorSafetyAmber else colorPureWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text("Injeta frames ARP para assumir a posição de Roteador para o alvo selecionado. Não precisa de zANTI/cSploit. Escolha o alvo abaixo e depois ative aqui.", color = Color.LightGray, fontSize = 10.sp, lineHeight = 12.sp, modifier = Modifier.padding(top = 4.dp))
                                            }
                                            Switch(
                                                checked = isArpSpoofing,
                                                onCheckedChange = {
                                                    if (it) {
                                                        if (rootTargetMode == "ALL") {
                                                            viewModel.startArpSpoof(targetIp = viewModel.getGatewayIp(), targetMac = "FF:FF:FF:FF:FF:FF")
                                                        } else if (rootTargetDevice.isEmpty() || rootTargetDevice.startsWith("IP-")) {
                                                            onShowSnackbar("Por favor, selecione um dispositivo ou 'Rede Inteira' primeiro.")
                                                        } else {
                                                            val targetConfig = devices.find { d -> d.macAddress == rootTargetDevice }
                                                            if (targetConfig != null) {
                                                                viewModel.startArpSpoof(targetIp = targetConfig.lastIp, targetMac = targetConfig.macAddress)
                                                            } else {
                                                                onShowSnackbar("Dispositivo alvo não encontrado!")
                                                            }
                                                        }
                                                    } else {
                                                        viewModel.stopArpSpoof()
                                                    }
                                                },
                                                colors = SwitchDefaults.colors(checkedThumbColor = bgDeepSlate, checkedTrackColor = colorSafetyAmber, uncheckedThumbColor = bgDeepSlate, uncheckedTrackColor = Color.Gray)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Button(onClick = { rootCommandLoading = true; rootCommandResult = ""; viewModel.executeRootCommand("iptables -L") { res -> rootCommandResult = res; rootCommandLoading = false } }, enabled = !rootCommandLoading, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = bgElevatedGrey, contentColor = colorElectricCyan)) { Text("LISTAR REGRAS IPTABLES") }
                                        
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("FERRAMENTAS OFENSIVAS (ROOT):", color = colorCrimsonKill, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                                        
                                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            FilterChip(selected = rootActionType == "KILL", onClick = { rootActionType = "KILL" }, label = { Text("Desativar Internet", fontSize = 11.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colorCrimsonKill, selectedLabelColor = Color.White))
                                            FilterChip(selected = rootActionType == "REDIRECT", onClick = { rootActionType = "REDIRECT" }, label = { Text("Redirecionar DNS", fontSize = 11.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colorSafetyAmber, selectedLabelColor = Color.Black))
                                            FilterChip(selected = rootActionType == "SNIFF", onClick = { rootActionType = "SNIFF" }, label = { Text("Sniffer Log", fontSize = 11.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colorElectricCyan, selectedLabelColor = Color.Black))
                                        }
                                        
                                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(modifier = Modifier.weight(1f).background(if (rootTargetMode == "ALL") colorElectricCyan.copy(alpha = 0.2f) else bgDeepSlate, RoundedCornerShape(8.dp)).border(1.dp, if (rootTargetMode == "ALL") colorElectricCyan else bgElevatedGrey, RoundedCornerShape(8.dp)).clickable { rootTargetMode = "ALL" }.padding(12.dp), contentAlignment = Alignment.Center) { Text("Toda a Rede", color = if (rootTargetMode == "ALL") colorElectricCyan else colorPureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                            Box(modifier = Modifier.weight(1f).background(if (rootTargetMode == "SPECIFIC") colorElectricCyan.copy(alpha = 0.2f) else bgDeepSlate, RoundedCornerShape(8.dp)).border(1.dp, if (rootTargetMode == "SPECIFIC") colorElectricCyan else bgElevatedGrey, RoundedCornerShape(8.dp)).clickable { rootTargetMode = "SPECIFIC" }.padding(12.dp), contentAlignment = Alignment.Center) { Text("Dispositivo", color = if (rootTargetMode == "SPECIFIC") colorElectricCyan else colorPureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                        }
                                        
                                    if (rootTargetMode == "SPECIFIC") {
                                        var expanded by remember { mutableStateOf(false) }
                                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                            OutlinedTextField(value = rootTargetDevice, onValueChange = {}, readOnly = true, label = { Text("Selecione o Dispositivo", color = colorMutedSilver) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = bgDeepSlate, unfocusedContainerColor = bgDeepSlate, focusedTextColor = colorPureWhite, unfocusedTextColor = colorPureWhite, focusedBorderColor = colorElectricCyan, unfocusedBorderColor = bgElevatedGrey), singleLine = true)
                                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = bgDeepSlate) {
                                                devices.forEach { device -> DropdownMenuItem(text = { val name = device.customName.ifEmpty { device.hostname.ifEmpty { "Desconhecido" } }; Text("$name (${device.lastIp})", color = colorPureWhite) }, onClick = { rootTargetDevice = device.macAddress; expanded = false }) }
                                            }
                                        }
                                    }
                                    
                                    if (rootActionType == "REDIRECT") {
                                        OutlinedTextField(value = rootRedirectUrl, onValueChange = { rootRedirectUrl = it }, label = { Text("IP de Destino (DNS)", color = colorMutedSilver) }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = bgDeepSlate, unfocusedContainerColor = bgDeepSlate, focusedTextColor = colorPureWhite, unfocusedTextColor = colorPureWhite, focusedBorderColor = colorElectricCyan, unfocusedBorderColor = bgElevatedGrey), singleLine = true)
                                    }
                                    
                                    if (rootActionType == "SNIFF") {
                                        val isSniffing by viewModel.isSniffing.collectAsStateWithLifecycle()
                                        val snifferLogs by viewModel.snifferLogs.collectAsStateWithLifecycle()
                                        Column(modifier = Modifier.fillMaxWidth().background(Color.Black, RoundedCornerShape(8.dp)).border(1.dp, bgElevatedGrey, RoundedCornerShape(8.dp)).padding(12.dp).padding(bottom = 8.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                               Text("LOG DE TRÁFEGO (DNS/HTTP)", color = colorElectricCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                               Row {
                                                   if(snifferLogs.isNotEmpty()) {
                                                       IconButton(onClick = { viewModel.clearSnifferLogs() }, modifier = Modifier.size(24.dp)) {
                                                           Icon(Icons.Outlined.Build, contentDescription="Limpar", tint = colorMutedSilver, modifier = Modifier.size(16.dp))
                                                       }
                                                   }
                                                   Spacer(modifier = Modifier.width(8.dp))
                                                   if (!isSniffing) {
                                                       Button(onClick = { viewModel.startSniffer() }, modifier = Modifier.height(28.dp), contentPadding = PaddingValues(horizontal = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = colorElectricCyan, contentColor = Color.Black)) { Text("INICIAR", fontSize = 10.sp) }
                                                   } else {
                                                       Button(onClick = { viewModel.stopSniffer() }, modifier = Modifier.height(28.dp), contentPadding = PaddingValues(horizontal = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = colorCrimsonKill, contentColor = Color.White)) { Text("PARAR", fontSize = 10.sp) }
                                                   }
                                               }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            if (snifferLogs.isEmpty()) {
                                                Text(if(isSniffing) "Aguardando tráfego..." else "Sniffer inativo. Clique em Iniciar.", color = Color.Gray, fontSize = 10.sp)
                                            } else {
                                                LazyColumn(modifier = Modifier.height(200.dp)) {
                                                    items(snifferLogs) { log ->
                                                        Text(log, color = Color.LightGray, fontFamily = FontFamily.Monospace, fontSize = 9.sp, modifier = Modifier.padding(vertical = 2.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (rootActionType != "SNIFF") {
                                        Button(
                                        onClick = {
                                            rootCommandLoading = true; rootCommandResult = ""; isRootActionActive = !isRootActionActive
                                            val actionName = if (rootActionType == "KILL") "Desativação de Internet" else "Redirecionamento"
                                            val targetFilter = if (rootTargetDevice.startsWith("IP-")) "-s ${rootTargetDevice.removePrefix("IP-")}" else "-m mac --mac-source $rootTargetDevice"
                                            val ipRedir = rootRedirectUrl
                                                .replace(Regex("^(https?://|//)"), "")
                                                .substringBefore("/")
                                                .substringBefore(":")
                                                .trim()
                                            val ipTarget = if (rootTargetDevice.startsWith("IP-")) rootTargetDevice.removePrefix("IP-") else {
                                                devices.find { it.macAddress == rootTargetDevice }?.lastIp ?: ""
                                            }
                                            
                                            val command = if (isRootActionActive) {
                                                if (rootActionType == "KILL") {
                                                    if (rootTargetMode == "ALL") "echo 'Bloqueando tráfego FORWARD de todos...' && iptables -I FORWARD -j DROP && echo 'Internet desativada para a rede!'"
                                                    else "echo 'Bloqueando tráfego do alvo $rootTargetDevice...' && iptables -I FORWARD -m mac --mac-source $rootTargetDevice -j DROP && iptables -I FORWARD -d $ipTarget -j DROP && echo 'Internet desativada para o alvo!'"
                                                } else {
                                                    if (rootTargetMode == "ALL") "echo 'Redirecionando DNS de todos...' && iptables -t nat -I PREROUTING -p udp --dport 53 -j DNAT --to-destination $ipRedir && echo 'Redirecionamento Ativo!'"
                                                    else "echo 'Redirecionando alvo $rootTargetDevice...' && iptables -t nat -I PREROUTING -m mac --mac-source $rootTargetDevice -p udp --dport 53 -j DNAT --to-destination $ipRedir && echo 'Redirecionamento Ativo alvo!'"
                                                }
                                            } else "iptables -D FORWARD -j DROP 2>/dev/null; iptables -F FORWARD && iptables -t nat -F PREROUTING && echo 'Regras de $actionName limpas. Normalizando a rede.'"
                                            
                                            viewModel.executeRootCommand(command) { res -> rootCommandResult = res; rootCommandLoading = false }
                                        },
                                            enabled = !rootCommandLoading && (rootTargetMode == "ALL" || rootTargetDevice.isNotEmpty()),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = if (isRootActionActive) bgElevatedGrey else if (rootActionType == "KILL") colorCrimsonKill else colorSafetyAmber, contentColor = if(isRootActionActive) colorElectricCyan else Color.White)
                                        ) { Text(if (isRootActionActive) "DESATIVAR $rootActionType EM CURSO" else "EXECUTAR ${if(rootActionType=="KILL") "WIFI KILL" else "REDIRECIONAMENTO"}") }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("TERMINAL DE COMANDOS (LIVE LOG):", color = colorElectricCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        if (rootToolLogs.isNotEmpty()) {
                                            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                                            IconButton(
                                                onClick = {
                                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(rootToolLogs.joinToString("\n")))
                                                    onShowSnackbar("Log copiado para a área de transferência!")
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copiar", tint = colorElectricCyan)
                                            }
                                        }
                                    }
                                    
                                    Column(
                                        modifier = Modifier.fillMaxWidth().height(150.dp).background(Color.Black, RoundedCornerShape(8.dp)).border(1.dp, bgElevatedGrey, RoundedCornerShape(8.dp)).padding(10.dp)
                                    ) {
                                        if (rootToolLogs.isEmpty()) {
                                            Text("Aguardando execução de comandos...", color = Color.DarkGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                        } else {
                                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                                items(rootToolLogs) { logMsg ->
                                                    Text(logMsg, color = if (logMsg.startsWith("[ERR]")) colorCrimsonKill else Color.LightGray, fontFamily = FontFamily.Monospace, fontSize = 10.sp, modifier = Modifier.padding(vertical = 1.dp))
                                                }
                                            }
                                        }
                                    }

                                    if (rootCommandResult.isNotEmpty() && false) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(rootCommandResult, color = Color.LightGray, fontFamily = FontFamily.Monospace, fontSize = 10.sp, modifier = Modifier.fillMaxWidth().background(Color.Black, RoundedCornerShape(8.dp)).padding(12.dp))
                                    }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppCard(title: String, desc: String, icon: androidx.compose.ui.graphics.vector.ImageVector, iconColor: Color, bgDeepSlate: Color, bgElevatedGrey: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(
                colors = listOf(bgElevatedGrey.copy(alpha = 0.8f), bgDeepSlate)
            ))
            .border(1.dp, bgElevatedGrey, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(16.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            horizontalAlignment = Alignment.Start, 
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(iconColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .border(1.dp, iconColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            
            Column {
                Text(
                    text = title, 
                    color = Color.White, 
                    fontSize = 15.sp, 
                    fontWeight = FontWeight.Bold, 
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc, 
                    color = Color(0xFFAAAAAA), 
                    fontSize = 12.sp, 
                    lineHeight = 16.sp, 
                    maxLines = 2, 
                    overflow = TextOverflow.Ellipsis
                )
            }
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
                            if (activeRatio > 0f) Box(modifier = Modifier.fillMaxWidth(activeRatio).height(12.dp).background(colorElectricCyan, RoundedCornerShape(6.dp)))
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
                                    if (ratio > 0f) Box(modifier = Modifier.fillMaxWidth(ratio).height(6.dp).background(colorElectricCyan, RoundedCornerShape(3.dp)))
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
                                    if (ratio > 0f) Box(modifier = Modifier.fillMaxWidth(ratio).height(6.dp).background(displayColor, RoundedCornerShape(3.dp)))
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

