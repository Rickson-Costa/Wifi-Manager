package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.NetworkWifi
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(mac: String, viewModel: NetworkViewModel, onBack: () -> Unit) {
    val device by remember(mac) { viewModel.getDevice(mac) }.collectAsStateWithLifecycle(initialValue = null)
    val events by remember(mac) { viewModel.getEvents(mac) }.collectAsStateWithLifecycle(initialValue = emptyList())
    
    var showPortScan by remember { mutableStateOf(false) }
    var ports by remember { mutableStateOf<List<String>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf("") }
    
    val bgMidnightOnyx = Color(0xFF0A0A0B)
    val bgDeepSlate = Color(0xFF161618)
    val bgElevatedGrey = Color(0xFF242426)
    val colorElectricCyan = Color(0xFF00E5FF)
    val colorCrimsonKill = Color(0xFFFF3B30)
    val colorPureWhite = Color(0xFFFFFFFF)
    val colorMutedSilver = Color(0xFFA1A1A6)
    
    if (showEditNameDialog && device != null) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Editar Nome do Dispositivo", color = colorPureWhite) },
            text = {
                OutlinedTextField(
                    value = editNameInput,
                    onValueChange = { editNameInput = it },
                    label = { Text("Nome Personalizado", color = colorMutedSilver) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colorPureWhite,
                        unfocusedTextColor = colorPureWhite,
                        focusedBorderColor = colorElectricCyan,
                        cursorColor = colorElectricCyan,
                        focusedLabelColor = colorElectricCyan
                    )
                )
            },
            containerColor = bgDeepSlate,
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.updateCustomName(device!!.macAddress, editNameInput)
                    showEditNameDialog = false 
                }) {
                    Text("SALVAR", color = colorElectricCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("CANCELAR", color = colorMutedSilver)
                }
            }
        )
    }

    val detailSnackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    
    Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(detailSnackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("AUDITORIA DO DISPOSITIVO", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorPureWhite, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colorPureWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgMidnightOnyx)
            )
        },
        containerColor = bgMidnightOnyx
    ) { padding ->
        device?.let { d ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bgDeepSlate, RoundedCornerShape(16.dp))
                            .border(1.dp, bgElevatedGrey, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(d.customName.ifEmpty { d.hostname }, fontSize = 18.sp, color = colorPureWhite, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { 
                                    editNameInput = d.customName.ifEmpty { d.hostname }
                                    showEditNameDialog = true 
                                }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Editar Nome", tint = colorElectricCyan)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("IP:  ${d.lastIp}", color = colorElectricCyan, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            Text("MAC: ${d.macAddress.uppercase()}", color = colorMutedSilver, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                            Text("FABRICANTE: ${d.vendor.uppercase()}", color = colorMutedSilver, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                }
                
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bgDeepSlate, RoundedCornerShape(16.dp))
                            .border(1.dp, bgElevatedGrey, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Shield, contentDescription = null, tint = colorElectricCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AUDITORIA DE PORTAS (TCP SCAN)", color = colorPureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            if (!showPortScan && !scanning) {
                                Button(
                                    onClick = { 
                                        scanning = true 
                                        viewModel.runRealPortScan(d.lastIp) { result ->
                                            ports = result
                                            scanning = false
                                            showPortScan = true
                                        }
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colorElectricCyan),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, colorElectricCyan.copy(alpha=0.3f))
                                ) {
                                    Text("INICIAR VARREDURA", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                }
                            } else if (scanning) {
                                CircularProgressIndicator(color = colorElectricCyan, modifier = Modifier.size(24.dp))
                            } else {
                                ports.forEach { port ->
                                    val isDanger = port.contains("⚠️")
                                    Text(
                                        " PORTA $port ", 
                                        color = if (isDanger) colorCrimsonKill else colorElectricCyan,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(vertical = 4.dp).background(if(isDanger) colorCrimsonKill.copy(alpha=0.1f) else colorElectricCyan.copy(alpha=0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical=2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Text("HISTÓRICO DE ATIVIDADE (LGPD)", color = colorPureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(top = 16.dp))
                }

                items(events) { event ->
                    TimelineItemHighDensity(event)
                }
            }
        }
    }
}

@Composable
fun TimelineItemHighDensity(event: com.example.data.NetworkEvent) {
    val formatter = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
    val dateStr = formatter.format(Date(event.timestamp))
    
    val colorElectricCyan = Color(0xFF00E5FF)
    val colorNeonEmerald = Color(0xFF10F093)
    val colorSafetyAmber = Color(0xFFFFB800)
    val colorCrimsonKill = Color(0xFFFF3B30)
    val colorPureWhite = Color(0xFFFFFFFF)
    val colorMutedSilver = Color(0xFFA1A1A6)

    val (icon, color, text) = when (event.eventType) {
        "BLOCK" -> Triple(Icons.Outlined.Block, colorCrimsonKill, "BLOQUEIO MANUAL APLICADO")
        "UNBLOCK" -> Triple(Icons.Outlined.NetworkWifi, colorNeonEmerald, "ACESSO RESTABELECIDO")
        "DISCOVERY" -> Triple(Icons.Outlined.Warning, colorSafetyAmber, "DISPOSITIVO ENTROU NA REDE")
        else -> Triple(Icons.Outlined.Warning, colorMutedSilver, "EVENTO DESCONHECIDO")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF161618), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF242426), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(Color(0xFF0A0A0B), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text, color = colorPureWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Text("$dateStr • ORIGEM: ${event.origin}", color = colorElectricCyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
