package com.rahmatsobrian.scrcpy.ui

import android.app.Application
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flyfishxu.kadb.DelayedAckMode
import com.flyfishxu.kadb.Kadb
import com.flyfishxu.kadb.KadbOptions
import com.rahmatsobrian.scrcpy.adb.LastConnectionStore
import com.rahmatsobrian.scrcpy.adb.LastWirelessTarget
import com.rahmatsobrian.scrcpy.adb.UsbAdbRepository
import com.rahmatsobrian.scrcpy.adb.WirelessAdbRepository
import com.rahmatsobrian.scrcpy.scrcpy.OptionStore
import com.rahmatsobrian.scrcpy.ui.i18n.t
import com.rahmatsobrian.scrcpy.scrcpy.ScrcpyController
import com.rahmatsobrian.scrcpy.scrcpy.ScrcpyOptions
import com.rahmatsobrian.scrcpy.scrcpy.ScrcpySession
import com.rahmatsobrian.scrcpy.scrcpy.openServerAsset
import com.rahmatsobrian.scrcpy.service.MirrorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Jenis mode daftar scrcpy (`--list-*`), server mencetak lalu keluar. */
enum class ListKind(val label: String) {
    ENCODERS("Video & audio encoders"),
    DISPLAYS("Displays"),
    CAMERAS("Cameras"),
    CAMERA_SIZES("Camera sizes"),
    APPS("Installed apps"),
}

/** Hasil satu eksekusi mode daftar. */
data class ListResult(val label: String, val text: String)

/** Status koneksi yang dipilih pengguna. */
sealed interface ConnectionTarget {
    data class Usb(val device: UsbDevice) : ConnectionTarget
    data class Wireless(val host: String, val port: Int) : ConnectionTarget
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    val controller = ScrcpyController(app)

    val usbRepository = UsbAdbRepository(app)
    val wirelessRepository = WirelessAdbRepository(app)

    /** Target nirkabel terakhir; dipakai untuk tombol "Sambungkan ulang". */
    private val _lastWireless = MutableStateFlow(LastConnectionStore.load(app))
    val lastWireless: StateFlow<LastWirelessTarget?> = _lastWireless.asStateFlow()

    val sessionState = controller.state
    val clipboard = controller.clipboard
    val videoSize = controller.videoSize

    private val _selectedTarget = MutableStateFlow<ConnectionTarget?>(null)
    val selectedTarget: StateFlow<ConnectionTarget?> = _selectedTarget.asStateFlow()

    private val _connecting = MutableStateFlow<String?>(null)
    val connecting: StateFlow<String?> = _connecting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** Galat koneksi nirkabel, ditampilkan di dalam bottom sheet. */
    private val _wirelessError = MutableStateFlow<String?>(null)
    val wirelessError: StateFlow<String?> = _wirelessError.asStateFlow()

    fun clearWirelessError() {
        _wirelessError.value = null
    }

    private fun rememberLastWireless(host: String, port: Int) {
        val target = LastWirelessTarget(host, port)
        _lastWireless.value = target
        LastConnectionStore.save(getApplication(), target)
    }

    private val _options = MutableStateFlow(OptionStore.load(app))
    val options: StateFlow<ScrcpyOptions> = _options.asStateFlow()

    /**
     * Nyalakan hanya saat koneksi baru berhasil dibuat, lalu konsumsi oleh UI.
     * Mencegah layar mirror "memantul" kembali saat sesi berhenti (state Ready).
     */
    private val _navigateToMirror = MutableStateFlow(false)
    val navigateToMirror: StateFlow<Boolean> = _navigateToMirror.asStateFlow()

    private val _listLoading = MutableStateFlow<ListKind?>(null)
    val listLoading: StateFlow<ListKind?> = _listLoading.asStateFlow()

    private val _listResult = MutableStateFlow<ListResult?>(null)
    val listResult: StateFlow<ListResult?> = _listResult.asStateFlow()

    fun consumeNavigation() {
        _navigateToMirror.value = false
    }

    private val _usbDevices = usbRepository.devices
    val usbDevices = _usbDevices

    // WAJIB dideklarasikan sebelum `init` — blok init memanggil refreshUsb().
    private val _usbRefreshing = MutableStateFlow(false)
    val usbRefreshing: StateFlow<Boolean> = _usbRefreshing.asStateFlow()

    /** Sedang memuat ulang opsi dari penyimpanan (tombol *refresh* Settings). */
    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    init {
        usbRepository.start()
        refreshUsb()
        MirrorService.sessionStopper = controller
        // Notifikasi layanan latar depan mengikuti siklus sesi mirror.
        viewModelScope.launch {
            var started = false
            sessionState.collect { state ->
                val running = state is ScrcpyController.State.Running
                val app = getApplication<Application>()
                if (running && !started) {
                    started = true
                    val name = state.deviceName.ifBlank { "Perangkat ADB" }
                    MirrorService.start(app, name)
                } else if (!running && started) {
                    started = false
                    MirrorService.stop(app)
                } else if (running && started) {
                    // Nama perangkat bisa berubah; notifikasi diperbarui lewat start ulang.
                    val name = state.deviceName.ifBlank { "Perangkat ADB" }
                    runCatching {
                        MirrorService.start(app, name)
                    }
                }
            }
        }
    }

    fun refreshUsb() {
        if (_usbRefreshing.value) return
        _usbRefreshing.value = true
        viewModelScope.launch {
            runCatching { usbRepository.refresh() }
            // Tahan sejenak supaya ContainedLoadingIndicator sempat terbaca.
            delay(400)
            _usbRefreshing.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    // ------------------------------------------------------------- connect

    fun connectUsb(device: UsbDevice) {
        viewModelScope.launch {
            _connecting.value = device.productName?.toString() ?: device.deviceName
            _error.value = null
            try {
                if (!usbRepositoryUsbManager().hasPermission(device)) {
                    usbRepository.requestPermission(device)
                    // Tunggu receiver; kalau belum ada izin, beri tahu pengguna.
                    _connecting.value = null
                    _error.value = t("USB permission needed — unplug & replug, or try again")
                    return@launch
                }
                val kadb = withContext(Dispatchers.IO) { openUsbKadb(device) }
                val name = device.productName?.toString() ?: device.deviceName
                controller.attach(kadb, name)
                _selectedTarget.value = ConnectionTarget.Usb(device)
                _navigateToMirror.value = true
                _connecting.value = null
            } catch (t: Throwable) {
                _connecting.value = null
                _error.value = t.message ?: t.toString()
            }
        }
    }

    private fun usbRepositoryUsbManager(): UsbManager =
        getApplication<Application>().getSystemService(UsbManager::class.java)

    private suspend fun openUsbKadb(device: UsbDevice): Kadb =
        withContext(Dispatchers.IO) {
            val repo = usbRepository
            Kadb.fromChannel(
                channelProvider = { repo.openChannel(device) },
                socketTimeout = 15_000,
                options = KadbOptions(DelayedAckMode.ENABLED),
            ).also { kadb ->
                runCatching {
                    check(kadb.shell("echo ok").output.contains("ok")) { t("ADB handshake failed") }
                }.onFailure { t ->
                    kadb.close()
                    throw t
                }
            }
        }

    fun connectWireless(host: String, port: Int) {
        viewModelScope.launch {
            _connecting.value = "$host:$port"
            _error.value = null
            _wirelessError.value = null
            try {
                val kadb = wirelessRepository.connect(host, port)
                controller.attach(kadb, host)
                _selectedTarget.value = ConnectionTarget.Wireless(host, port)
                rememberLastWireless(host, port)
                _navigateToMirror.value = true
                _connecting.value = null
            } catch (t: Throwable) {
                _connecting.value = null
                _wirelessError.value = WirelessAdbRepository.describeError(t, host, port)
            }
        }
    }

    fun pairWireless(host: String, port: Int, code: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            _wirelessError.value = null
            try {
                wirelessRepository.pair(host, port, code)
                onDone(null)
            } catch (t: Throwable) {
                onDone(WirelessAdbRepository.describeError(t, host, port))
            }
        }
    }

    // ------------------------------------------------------------- session

    fun startSession() {
        if (_selectedTarget.value == null) return
        controller.start(_options.value)
    }

    fun restartSession() {
        controller.stop()
        viewModelScope.launch {
            withContext(Dispatchers.Default) { kotlinx.coroutines.delay(150) }
            startSession()
        }
    }

    /** Pulihkan dari state Failed lalu jalankan ulang sesi. */
    fun retrySession() {
        if (!controller.acknowledgeFailure()) return
        startSession()
    }

    /** Perbarui opsi sekaligus menyimpannya — tidak perlu tombol simpan. */
    fun setOptions(options: ScrcpyOptions) {
        _options.value = options
        runCatching { OptionStore.save(getApplication(), options) }
    }

    /**
     * Muat ulang opsi dari penyimpanan. [refreshing] bernilai `true` selama
     * proses berjalan agar tombol refresh bisa memunculkan indikator.
     */
    fun reloadOptions() {
        if (_refreshing.value) return
        _refreshing.value = true
        viewModelScope.launch {
            val stored = withContext(Dispatchers.IO) {
                runCatching { OptionStore.load(getApplication()) }.getOrNull()
            }
            if (stored != null) _options.value = stored
            // Tahan sejenak supaya indikator sempat terbaca oleh pengguna.
            delay(400)
            _refreshing.value = false
        }
    }

    /**
     * Balik arah kamera (`front` ↔ `back`) lalu jalankan ulang sesi.
     * scrcpy 4.1 tidak punya pesan kontrol untuk bertukar kamera saat sesi
     * berjalan, jadi satu-satunya cara adalah restart dengan opsi berbeda.
     */
    fun switchCameraFacing() {
        val cur = _options.value
        val flipped = if (cur.cameraFacing == "back") "front" else "back"
        // `camera_id` dan `camera_facing` saling eksklusif.
        setOptions(cur.copy(cameraId = null, cameraFacing = flipped))
        restartSession()
    }

    fun stopSession() = controller.stop()

    // ------------------------------------------------------------- list_*

    /**
     * Jalankan server sekali dalam mode [kind] (mis. `list_encoders=true`)
     * dan kumpulkan stdout-nya. Opsi sesi utama tidak diubah.
     */
    fun runList(kind: ListKind) {
        val conn = controller.connectedKadb
        if (conn == null) {
            _error.value = t("Connect an ADB device first")
            return
        }
        if (_listLoading.value != null) return
        _listLoading.value = kind
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val opts = when (kind) {
                    ListKind.ENCODERS -> _options.value.copy(
                        listEncoders = true, listDisplays = false, listCameras = false,
                        listCameraSizes = false, listApps = false,
                    )
                    ListKind.DISPLAYS -> _options.value.copy(
                        listEncoders = false, listDisplays = true, listCameras = false,
                        listCameraSizes = false, listApps = false,
                    )
                    ListKind.CAMERAS -> _options.value.copy(
                        listEncoders = false, listDisplays = false, listCameras = true,
                        listCameraSizes = false, listApps = false,
                    )
                    ListKind.CAMERA_SIZES -> _options.value.copy(
                        listEncoders = false, listDisplays = false, listCameras = false,
                        listCameraSizes = true, listApps = false,
                    )
                    ListKind.APPS -> _options.value.copy(
                        listEncoders = false, listDisplays = false, listCameras = false,
                        listCameraSizes = false, listApps = true,
                    )
                }
                val session = ScrcpySession(
                    kadb = conn,
                    options = opts,
                    openServerAsset = { getApplication<Application>().openServerAsset() },
                    listener = object : ScrcpySession.Listener {},
                )
                val raw = session.listOutput()
                _listResult.value = ListResult(kind.label, cleanServerLog(raw))
            } catch (t: Throwable) {
                _error.value = t.message ?: t.toString()
            } finally {
                _listLoading.value = null
            }
        }
    }

    fun clearListResult() {
        _listResult.value = null
    }

    fun disconnect() {
        controller.close()
        _selectedTarget.value = null
    }

    private fun cleanServerLog(raw: String): String =
        raw.lineSequence()
            .map { it.replace(Regex("^\\[server] (INFO|DEBUG|VERBOSE): "), "") }
            .filter { it.isNotBlank() }
            .joinToString("\n")
            .ifBlank { t("(no output)") }

    override fun onCleared() {
        MirrorService.sessionStopper = null
        runCatching { MirrorService.stop(getApplication()) }
        controller.close()
        usbRepository.close()
    }
}
