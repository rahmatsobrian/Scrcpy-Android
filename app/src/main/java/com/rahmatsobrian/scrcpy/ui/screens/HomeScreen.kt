@file:kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.rahmatsobrian.scrcpy.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rahmatsobrian.scrcpy.adb.UsbAdbRepository
import com.rahmatsobrian.scrcpy.scrcpy.ScrcpyController
import com.rahmatsobrian.scrcpy.ui.i18n.t
import com.rahmatsobrian.scrcpy.ui.i18n.tf
import com.rahmatsobrian.scrcpy.ui.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: AppViewModel,
    onOpenMirror: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val devices by vm.usbDevices.collectAsStateWithLifecycle()
    val connecting by vm.connecting.collectAsStateWithLifecycle()
    val usbRefreshing by vm.usbRefreshing.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val state by vm.sessionState.collectAsStateWithLifecycle()
    val lastTarget by vm.lastWireless.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Tawarkan sambung ulang ke target terakhir selama tidak ada sesi yang hidup.
    val showReconnect = lastTarget != null && connecting == null &&
        state !is ScrcpyController.State.Ready && state !is ScrcpyController.State.Running

    var showWireless by remember { mutableStateOf(false) }
    var permissionNotice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { vm.refreshUsb() }
    val navigateToMirror by vm.navigateToMirror.collectAsStateWithLifecycle()
    LaunchedEffect(navigateToMirror) {
        if (navigateToMirror) {
            onOpenMirror()
            vm.consumeNavigation()
        }
    }

    LaunchedEffect(Unit) {
        vm.usbRepository.permissionDenied.collect {
            if (it != null) permissionNotice = t("USB permission denied for this device")
        }
    }

    LaunchedEffect(error) {
        if (error != null) {
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            vm.clearError()
        }
    }

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(permissionNotice) {
        permissionNotice?.let {
            snackbar.showSnackbar(it)
            permissionNotice = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Cast,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Scrcpy Android")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = t("Settings"))
                    }
                },
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = (devices.isNotEmpty() || state is ScrcpyController.State.Ready) &&
                    connecting == null,
                enter = fadeIn(tween(220)) + expandVertically(),
                exit = fadeOut(tween(160)) + shrinkVertically(),
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        when {
                            state is ScrcpyController.State.Ready ||
                                state is ScrcpyController.State.Running -> onOpenMirror()
                            else -> devices.firstOrNull()?.let { vm.connectUsb(it.device) }
                        }
                    },
                    icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                    text = { Text(t(if (state is ScrcpyController.State.Ready) "Open mirror" else "Start mirror")) },
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            SessionBanner(state = state, onOpenMirror = onOpenMirror, onDisconnect = vm::disconnect)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (showReconnect) {
                    item {
                        val target = lastTarget
                        if (target != null) {
                            SectionCard(
                                title = t("Last connected"),
                                subtitle = "${target.host}:${target.port}",
                                icon = {
                                    Icon(
                                        Icons.Filled.History,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                },
                            ) {
                                FilledTonalButton(
                                    onClick = { vm.connectWireless(target.host, target.port) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text(t("Reconnect")) }
                            }
                        }
                    }
                }

                item {
                    SectionCard(
                        title = t("USB connection (OTG)"),
                        subtitle = t("Devices connected through a data cable"),
                        icon = { Icon(Icons.Filled.Usb, null, tint = MaterialTheme.colorScheme.primary) },
                    ) {
                        if (usbRefreshing) {
                            // Judul & deskripsi tetap; seluruh isi diganti blok
                            // solid berisi animasi refresh sampai pemindaian selesai.
                            UsbRefreshingBlock()
                        } else if (devices.isEmpty()) {
                            EmptyUsbState(onRefresh = { vm.refreshUsb() })
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                devices.forEach { entry ->
                                    UsbDeviceRow(
                                        name = entry.device.productName?.toString()
                                            ?: entry.device.deviceName,
                                        manufacturer = entry.device.manufacturerName,
                                        hasPermission = entry.hasPermission,
                                        busy = connecting != null,
                                        onClick = { vm.connectUsb(entry.device) },
                                        onRequestPermission = {
                                            scope.launch {
                                                vm.usbRepository.requestPermission(entry.device)
                                                delay(1200)
                                                vm.refreshUsb()
                                            }
                                        },
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(onClick = { vm.refreshUsb() }) {
                                        Icon(
                                            Icons.Filled.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(t("Refresh"))
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    SectionCard(
                        title = t("Wireless connection"),
                        subtitle = "ADB over TCP/IP — pairing & connect",
                        icon = { Icon(Icons.Filled.Wifi, null, tint = MaterialTheme.colorScheme.tertiary) },
                    ) {
                        val local = remember { vm.wirelessRepository.localIpAddress() }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                tf("This device address: {}", local),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(
                                onClick = { showWireless = true },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Filled.Android, null)
                                Spacer(Modifier.width(8.dp))
                                Text(t("Connect / Pairing"))
                            }
                        }
                    }
                }

            }
        }
    }

    if (showWireless) {
        WirelessSheet(
            vm = vm,
            onDismiss = { showWireless = false },
        )
    }
}

// ---------------------------------------------------------------- session banner

@Composable
private fun SessionBanner(
    state: ScrcpyController.State,
    onOpenMirror: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val visible = state is ScrcpyController.State.Running ||
        state is ScrcpyController.State.Ready ||
        state is ScrcpyController.State.Connecting ||
        state is ScrcpyController.State.Failed

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(tween(260)) + fadeIn(tween(260)),
        exit = shrinkVertically(tween(200)) + fadeOut(tween(160)),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = when (state) {
                is ScrcpyController.State.Running, is ScrcpyController.State.Ready ->
                    MaterialTheme.colorScheme.primaryContainer
                is ScrcpyController.State.Failed -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            tonalElevation = 2.dp,
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedContent(
                        targetState = state,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(160)) },
                        label = "banner",
                    ) { s ->
                        when (s) {
                            is ScrcpyController.State.Running -> Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        tf("Connected to {}", s.deviceName),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    val size =
                                        if (s.videoWidth > 0) "${s.videoWidth}×${s.videoHeight}" else "—"
                                    Text(
                                        tf("Video {} · codec {}", size, codecName(s.videoCodecId)),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }

                            is ScrcpyController.State.Ready -> Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        s.deviceName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        t("ADB connected — open the mirror to start"),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }

                            is ScrcpyController.State.Connecting -> Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularWavyProgressIndicator(
                                    modifier = Modifier.size(26.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(s.label, style = MaterialTheme.typography.bodyMedium)
                            }

                            is ScrcpyController.State.Failed -> Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Filled.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    s.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }

                            else -> Unit
                        }
                    }
                }
                if (state is ScrcpyController.State.Connecting) {
                    Spacer(Modifier.height(10.dp))
                    LinearWavyProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(16.dp),
                    )
                }
                if (state is ScrcpyController.State.Running ||
                    state is ScrcpyController.State.Ready
                ) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onOpenMirror, modifier = Modifier.weight(1f)) {
                            Text(t("Open mirror screen"))
                        }
                        TextButton(onClick = onDisconnect) { Text(t("Disconnect")) }
                    }
                }
            }
        }
    }
}

private fun codecName(id: Int): String = when (id) {
    0x68323634 -> "H.264"
    0x68323635 -> "H.265"
    0x00617631 -> "AV1"
    0x00767038 -> "VP8"
    0x00767039 -> "VP9"
    0 -> "—"
    else -> "0x${id.toString(16)}"
}

// ---------------------------------------------------------------- section card

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "cardScale",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
                ?: MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) { icon() }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

// ---------------------------------------------------------------- usb row

@Composable
private fun UsbDeviceRow(
    name: String,
    manufacturer: String?,
    hasPermission: Boolean,
    busy: Boolean,
    onClick: () -> Unit,
    onRequestPermission: () -> Unit,
) {
    Surface(
        onClick = if (hasPermission) onClick else onRequestPermission,
        enabled = !busy,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.PhoneAndroid,
                contentDescription = null,
                tint = if (hasPermission) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    manufacturer ?: t("USB device"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Jarak antar nama device dan tombol "Grant permission".
            Spacer(Modifier.width(14.dp))
            if (hasPermission) {
                Text(
                    t("Connect"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                FilledTonalButton(onClick = onRequestPermission, enabled = !busy) {
                    Text(t("Grant permission"))
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

/** Blok solid berisi animasi refresh; menutup isi kartu selama pemindaian USB. */
@Composable
private fun UsbRefreshingBlock() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            ContainedLoadingIndicator(Modifier.size(56.dp))
        }
    }
}

@Composable
private fun EmptyUsbState(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.Usb,
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            t("No ADB device detected yet"),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            t("Plug in an OTG cable, enable USB debugging, then tap Refresh."),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        FilledTonalButton(onClick = onRefresh) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(t("Refresh"))
        }
    }
}

// ---------------------------------------------------------------- wireless sheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WirelessSheet(
    vm: AppViewModel,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val connecting by vm.connecting.collectAsStateWithLifecycle()

    // Pracetak IP/port terakhir supaya tidak perlu diketik ulang.
    val remembered = vm.lastWireless.value
    var host by remember { mutableStateOf(remembered?.host ?: "") }
    var port by remember { mutableStateOf(remembered?.port?.toString() ?: "5555") }
    var pairPort by remember { mutableStateOf("37400") }
    var code by remember { mutableStateOf("") }
    var pairingMessage by remember { mutableStateOf<String?>(null) }
    var pairingFailed by remember { mutableStateOf(false) }
    val wirelessError by vm.wirelessError.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        // Angkat isi sheet di atas keyboard, bukan tertutup keyboard.
        contentWindowInsets = { WindowInsets.ime },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Cadangan: bila tinggi keyboard membuat isi melebihi layar,
                // field yang fokus tetap bisa digulir ke atas.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(t("Wireless"), style = MaterialTheme.typography.headlineSmall)
            Text(
                t("Pairing stores the device security key, not its IP address — " +
                    "so the address is still typed manually when connecting. " +
                    "It only needs to be done once per pair of devices."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalThinDivider()

            Text(t("1. Pairing (Android 11+)"), style = MaterialTheme.typography.titleMedium)
            // Urutan: kode pairing di kiri, port pairing di kanan.
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { c -> code = c.filter { it.isDigit() }.take(6) },
                    label = { Text(t("Pairing code")) },
                    placeholder = { Text(t("6 digits")) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = pairPort,
                    onValueChange = { pairPort = it.filter { c -> c.isDigit() } },
                    label = { Text(t("Pairing port")) },
                    singleLine = true,
                    modifier = Modifier.weight(0.5f),
                )
            }
            Text(
                t("The pairing port appears in the Pair device with pairing code dialog " +
                    "and differs from the connect port. Open Settings → Developer options → " +
                    "Wireless debugging to see it."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = {
                    val h = host.trim()
                    val p = pairPort.trim().toIntOrNull() ?: 37400
                    if (h.isEmpty()) {
                        pairingFailed = true
                        pairingMessage = t("Enter the device IP address first")
                    } else {
                        vm.pairWireless(h, p, code) {
                            pairingFailed = it != null
                            pairingMessage = it ?: t("Pairing succeeded")
                        }
                    }
                },
                enabled = code.length == 6 && connecting == null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Pair") }

            AnimatedVisibility(
                visible = pairingMessage != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Text(
                    pairingMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (pairingFailed) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }

            HorizontalThinDivider()

            Text(t("2. Connect"), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text(t("Device IP")) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter { c -> c.isDigit() } },
                    label = { Text(t("Port")) },
                    singleLine = true,
                    modifier = Modifier.weight(0.5f),
                )
            }
            Text(
                t("This IP and port appear on the main Wireless debugging screen, next to " +
                    "the device name. Do not use the pairing port."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FilledTonalButton(
                onClick = {
                    val h = host.trim()
                    val p = port.trim().toIntOrNull() ?: 5555
                    if (h.isNotEmpty()) vm.connectWireless(h, p)
                },
                enabled = connecting == null && host.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (connecting != null) {
                    ContainedLoadingIndicator(Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Text("Connect")
            }

            AnimatedVisibility(
                visible = wirelessError != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Text(
                    wirelessError.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text(t("Close"))
            }
        }
    }
}

@Composable
private fun HorizontalThinDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Suppress("unused")
private val unusedUsbAction: String = UsbAdbRepository.ACTION_USB_PERMISSION
