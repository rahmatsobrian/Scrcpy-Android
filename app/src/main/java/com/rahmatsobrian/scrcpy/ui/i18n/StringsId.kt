package com.rahmatsobrian.scrcpy.ui.i18n

/**
 * Terjemahan Inggris → Indonesia untuk seluruh teks UI.
 *
 * Kunci selalu berupa teks Inggris persis seperti yang tertulis di kode
 * sumber; kunci yang belum punya padanan akan ditampilkan apa adanya (Inggris).
 */
internal val StringsId: Map<String, String> = mapOf(

    // ---------------------------------------------------------------- aplikasi
    "scrcpy Settings" to "Pengaturan scrcpy",
    "Back" to "Kembali",
    "Reset" to "Reset",
    "Close" to "Tutup",
    "Copy" to "Salin",
    "Settings apply to the next scrcpy session. A random scid is generated every time for socket security."
        to "Pengaturan berlaku pada sesi scrcpy berikutnya. Scid dibuat acak tiap sesi untuk keamanan socket.",

    // ---------------------------------------------------------------- tampilan
    "Appearance" to "Tampilan",
    "Theme, accent color and interface language."
        to "Tema, warna aksen, dan bahasa antarmuka.",
    "Theme" to "Tema",
    "Light, dark, true-black AMOLED, or follow the system setting."
        to "Terang, gelap, AMOLED hitam pekat, atau ikut pengaturan sistem.",
    "System" to "Sistem",
    "Light" to "Terang",
    "Dark" to "Gelap",
    "AMOLED" to "AMOLED",
    "Dynamic color" to "Warna dinamis",
    "Build the palette from your wallpaper, like Material You (Android 12+)."
        to "Susun palet dari wallpaper, seperti Material You (Android 12+).",
    "Accent color" to "Warna aksen",
    "Seed color used to build the palette when dynamic color is off."
        to "Warna dasar penyusun palet saat warna dinamis dimatikan.",
    "Turn off dynamic color to pick an accent color."
        to "Matikan warna dinamis untuk memilih warna aksen.",
    "Language" to "Bahasa",
    "Language used across the whole app." to "Bahasa yang dipakai di seluruh aplikasi.",

    // ---------------------------------------------------------------- video
    "Video" to "Video",
    "Source, codec and quality of the video stream from the target device."
        to "Sumber, codec, dan kualitas stream video dari perangkat target.",
    "Capture video" to "Tangkap video",
    "Turn off to connect without a video stream; audio and control keep working."
        to "Matikan untuk menyambung tanpa stream video; audio dan kontrol tetap bisa jalan.",
    "Video codec" to "Codec video",
    "Video encoder codec used by the scrcpy server."
        to "Codec encoder video yang dipakai server scrcpy.",
    "Video bit rate (bps)" to "Bit rate video (bps)",
    "Target bitrate of the video encoder; 0 means automatic."
        to "Target bitrate encoder video; 0 berarti otomatis.",
    "Maximum size (px, 0 = unlimited)" to "Batas ukuran maksimum (px, 0 = tanpa batas)",
    "The longest side of a frame never exceeds this value."
        to "Sisi terpanjang frame tidak akan melebihi nilai ini.",
    "Unlimited" to "Tanpa batas",
    "Maximum FPS" to "Batas FPS",
    "Cap the frame rate of the stream, for example 60."
        to "Batasi frame per detik stream, misalnya 60.",
    "e.g. 60 (empty = unlimited)" to "mis. 60 (kosong = bebas)",
    "Video source" to "Sumber video",
    "Capture the device screen or its physical camera."
        to "Rekam layar perangkat atau kamera fisiknya.",
    "Display" to "Layar",
    "Camera" to "Kamera",
    "Crop (W:H:X:Y)" to "Crop (W:H:X:Y)",
    "Cut the captured area using width:height:x:y."
        to "Potong area tangkapan dengan format lebar:tinggi:x:y.",
    "empty = no crop" to "kosong = tanpa crop",
    "Video encoder" to "Encoder video",
    "Force a specific video encoder; empty for automatic."
        to "Paksa encoder video tertentu; kosong untuk pemilihan otomatis.",
    "automatic" to "otomatis",
    "Video codec options" to "Opsi codec video",
    "Extra options for the video encoder, key=value pairs separated by commas."
        to "Opsi tambahan encoder video, format key=value dipisah koma.",
    "Capture orientation" to "Orientasi tangkap",
    "Rotate or mirror the capture, for example 90 or 0x1000."
        to "Rotasi atau mirror hasil tangkapan, misalnya 90 atau 0x1000.",
    "0|90|180|270, 0x prefix = mirror" to "0|90|180|270, awalan 0x untuk mirror",
    "ANGLE backend (Android 10+)" to "Backend ANGLE (Android 10+)",
    "ANGLE render backend used for video processing, for example sw."
        to "Backend render ANGLE untuk pemrosesan video, misalnya sw.",
    "e.g. sw, angle" to "mis. sw, angle",
    "Minimum size alignment" to "Perataan ukuran minimum",
    "Round the frame width and height to a multiple of this value."
        to "Bulatkan lebar/tinggi frame ke kelipatan nilai ini.",
    "Ignore encoder constraints" to "Abaikan batasan encoder",
    "Force the encoder to use the requested profile or level even if the target does not support it."
        to "Paksa encoder memakai profil/level yang diminta walau di luar dukungan perangkat target.",
    "Downsize on error" to "Perkecil saat error",
    "Lower the resolution automatically and retry when the encoder fails."
        to "Turunkan resolusi otomatis lalu coba lagi bila encoder gagal.",

    // ---------------------------------------------------------------- audio
    "Audio" to "Audio",
    "Source, codec and quality of the audio stream from the target device."
        to "Sumber, codec, dan kualitas stream audio dari perangkat target.",
    "Capture audio" to "Tangkap audio",
    "Turn off to stop the audio stream coming from the target device."
        to "Matikan untuk menghentikan stream audio dari perangkat target.",
    "Audio codec" to "Codec audio",
    "Server audio encoder. AAC is the default and works on almost every device."
        to "Encoder audio server. Bawaan AAC (paling kompatibel).",
    "Audio bit rate (bps)" to "Bit rate audio (bps)",
    "Target bitrate of the audio encoder." to "Target bitrate encoder audio.",
    "Audio source" to "Sumber audio",
    "Audio endpoint on the target device, for example output."
        to "Endpoint sumber audio di perangkat target, misalnya output.",
    "output (default)" to "output (bawaan)",
    "Keep target audio playing" to "Target tetap bersuara",
    "The target device keeps making sound while mirroring. Turn off only if you want it silent."
        to "Target tetap mengeluarkan suara saat mirror. Matikan hanya jika ingin HP target diam.",
    "Audio encoder" to "Encoder audio",
    "Force a specific audio encoder; empty for automatic."
        to "Paksa encoder audio tertentu; kosong untuk pemilihan otomatis.",
    "Audio codec options" to "Opsi codec audio",
    "Extra options for the audio encoder, key=value pairs separated by commas."
        to "Opsi tambahan encoder audio, format key=value dipisah koma.",

    // ------------------------------------------------------- tampilan & kontrol
    "Display & control" to "Tampilan & kontrol",
    "Screen behaviour, clipboard and input permissions on the target device."
        to "Perilaku layar, klipbord, dan izin kontrol ke perangkat target.",
    "Target display ID" to "ID display target",
    "Pick a physical display on the target device by its ID."
        to "Pilih display fisik perangkat target berdasarkan ID-nya.",
    "Control (touch and type)" to "Kontrol (sentuh/ketik)",
    "Turn off for view-only mode; gestures and keyboard are not sent to the device."
        to "Matikan untuk mode lihat-saja; gestur dan keyboard tidak dikirim ke perangkat.",
    "Show touches" to "Tampilkan sentuhan",
    "Draw touch points and trails on the target device screen."
        to "Tampilkan titik/jejak sentuhan langsung di layar perangkat target.",
    "Stay awake" to "Tetap terjaga",
    "Keep the target screen from sleeping while the session runs."
        to "Cegah layar target tidur selama sesi berlangsung.",
    "Screen off timeout (ms)" to "Screen off timeout (ms)",
    "Screen timeout of the target in milliseconds; -1 keeps the device default."
        to "Timeout layar target dalam milidetik; -1 memakai setelan pabrik.",
    "-1 = default" to "-1 = bawaan",
    "Turn screen off when closed" to "Putuskan daya saat ditutup",
    "Switch off the target screen as soon as the scrcpy session ends."
        to "Matikan layar target begitu sesi scrcpy berakhir.",
    "Turn screen on at start" to "Nyalakan layar saat mulai",
    "Wake the target screen when the connection begins."
        to "Bangunkan layar target saat koneksi dimulai.",
    "Automatic clipboard sync" to "Sinkron klipbord otomatis",
    "Two-way copy and paste between this device and the target."
        to "Salin-tempel dua arah antara perangkat ini dan perangkat target.",
    "Clean up state when finished" to "Bersihkan state saat selesai",
    "Remove the extra state scrcpy created once the session closes."
        to "Hapus state tambahan yang dibuat scrcpy ketika sesi ditutup.",
    "Keep session active" to "Tetap aktif",
    "Keep the session alive even while the target screen is off."
        to "Pertahankan sesi walau layar perangkat target sedang mati.",

    // ---------------------------------------------------------------- kamera
    "Camera ID" to "ID kamera",
    "Pick a physical camera by its ID, for example 0."
        to "Pilih kamera fisik berdasarkan ID, misalnya 0.",
    "e.g. 0" to "mis. 0",
    "Camera size (WxH)" to "Ukuran kamera (WxH)",
    "Force the camera resolution; empty for automatic size."
        to "Paksa resolusi kamera; kosong untuk ukuran otomatis.",
    "Camera facing" to "Arah kamera",
    "Which camera to use: front, back, external or automatic."
        to "Arah kamera: depan, belakang, eksternal, atau otomatis.",
    "Automatic" to "Otomatis",
    "Camera aspect ratio" to "Rasio aspek kamera",
    "Aspect ratio of the camera, for example 4:3 or 16:9."
        to "Rasio aspek kamera, misalnya 4:3 atau 16:9.",
    "e.g. 4:3 / 16:9" to "mis. 4:3 / 16:9",
    "Camera FPS" to "FPS kamera",
    "Force the camera frame rate; empty or 0 means automatic."
        to "Paksa frame rate kamera; kosong/0 berarti otomatis.",
    "High-speed camera" to "Kamera kecepatan tinggi",
    "Allow the high-speed mode for camera frame rates above 60 fps."
        to "Izinkan mode high-speed untuk frame rate kamera di atas 60 fps.",
    "Camera torch" to "Lampu kilat kamera",
    "Turn on the camera flashlight while the camera stream runs."
        to "Nyalakan senter kamera selama stream kamera berjalan.",
    "Camera zoom" to "Zoom kamera",
    "Digital camera zoom as a ratio, for example 2.0."
        to "Zoom digital kamera dalam rasio, misalnya 2.0.",
    "ratio, e.g. 2.0" to "rasio, mis. 2.0",

    // ---------------------------------------------------------- virtual display
    "Virtual display" to "Virtual display",
    "Create an extra virtual display on the target device."
        to "Buat display virtual tambahan di perangkat target.",
    "New virtual display" to "Display virtual baru",
    "Create a new virtual display; empty, WxH, WxH/dpi or /dpi."
        to "Buat display virtual baru; kosong, WxH, WxH/dpi, atau /dpi.",
    "Flexible display" to "Display lentur",
    "Let the display follow orientation and window size changes."
        to "Aktifkan mode display lentur yang menyesuaikan orientasi dan ukuran jendela.",
    "Clear the source display" to "Kosongkan display asal",
    "Empty the content of the original display while the new virtual display is used."
        to "Kosongkan isi display asal saat memakai virtual display baru.",
    "Show system bars" to "Tampilkan bilah sistem",
    "Show the status bar and navigation bar on the new virtual display."
        to "Tampilkan status bar dan navigation bar pada virtual display baru.",
    "Keyboard policy" to "Kebijakan keyboard",
    "Where the keyboard appears on the new display: 0, 1 or 2."
        to "Kebijakan keyboard pada display baru: 0, 1, atau 2.",

    // ------------------------------------------------------------ klien
    "Client (input & behaviour)" to "Klien (input & perilaku)",
    "Input handling done by this app instead of the server."
        to "Perilaku input yang diproses di sisi aplikasi ini, bukan server.",
    "Forward mouse hover" to "Teruskan hover mouse",
    "Send the cursor position while no mouse button is pressed."
        to "Kirim posisi kursor ketika tidak ada tombol mouse yang ditekan.",
    "Send characters as text" to "Kirim karakter sebagai teks",
    "The whole keyboard is sent as text instead of key events."
        to "Seluruh keyboard dikirim sebagai teks, bukan key event.",
    "Raw key events" to "Key event mentah",
    "The whole keyboard is sent as raw key events; text input is turned off."
        to "Seluruh keyboard dikirim sebagai key event mentah; input teks dimatikan.",
    "Repeat keys while held" to "Ulangi tombol saat ditahan",
    "Forward repeated key events so key repeat keeps working."
        to "Kirim event berulang saat tombol ditahan agar key repeat tetap jalan.",
    "Turn screen off at start" to "Matikan layar saat mulai",
    "Switch the target screen off as soon as the session starts."
        to "Langsung mematikan layar perangkat target begitu sesi dimulai.",
    "Launch an app at start" to "Buka aplikasi saat mulai",
    "Open an app when the session starts; a + prefix forces it, ? only if already running."
        to "Buka aplikasi saat sesi mulai; awalan + memaksa, ? hanya bila sudah berjalan.",
    "e.g. +com.android.settings" to "mis. +com.android.settings",

    // ---------------------------------------------------------- server & log
    "Server & log" to "Server & log",
    "scrcpy server logging, state cleanup and device information."
        to "Logging server scrcpy, pembersihan state, dan info perangkat.",
    "Log level" to "Level log",
    "Server log verbosity, from verbose down to error."
        to "Tingkat log server, dari verbose sampai error.",
    "Clean up temporary files" to "Bersihkan file sementara",
    "Delete the temporary files the scrcpy server created once it finishes."
        to "Hapus file sementara yang dibuat server scrcpy setelah selesai.",
    "Device info" to "Informasi perangkat",
    "Fullscreen" to "Layar penuh",
    "Exit fullscreen" to "Keluar layar penuh",
    "Run the scrcpy server once and exit to read encoders, displays, cameras and installed apps from the target device."
        to "Menjalankan server scrcpy sekali lalu keluar untuk membaca encoder, display, kamera, dan aplikasi terpasang di perangkat target.",
    "Read the video and audio encoders available on the target device."
        to "Baca encoder video & audio yang tersedia di perangkat target.",
    "Read the displays available on the target device."
        to "Baca display yang tersedia di perangkat target.",
    "Read the cameras available on the target device."
        to "Baca kamera yang tersedia di perangkat target.",
    "Read the resolutions supported by every camera."
        to "Baca resolusi yang didukung setiap kamera.",
    "Read the applications installed on the target device."
        to "Baca aplikasi yang terpasang di perangkat target.",

    // ---------------------------------------------------------------- list_*
    "Video & audio encoders" to "Encoder video & audio",
    "Displays" to "Display",
    "Cameras" to "Kamera",
    "Camera sizes" to "Ukuran kamera",
    "Installed apps" to "Aplikasi terpasang",
    // ---------------------------------------------------------------- beranda
    "USB permission denied for this device" to "Izin USB ditolak untuk perangkat",
    "Settings" to "Pengaturan",
    "Open mirror" to "Buka Mirror",
    "Start mirror" to "Mulai Mirror",
    "Last connected" to "Terakhir terhubung",
    "Reconnect" to "Sambungkan ulang",
    "USB connection (OTG)" to "Koneksi USB (OTG)",
    "Devices connected through a data cable" to "Perangkat yang terhubung lewat kabel data",
    "Refresh" to "Segarkan",
    "Wireless connection" to "Koneksi Nirkabel",
    "This device address: {}" to "Alamat perangkat ini: {}",
    "Connect / Pairing" to "Hubungkan / Pairing",
    "Connected to {}" to "Terhubung ke {}",
    "Video {} · codec {}" to "Video {} · codec {}",
    "ADB connected — open the mirror to start" to "ADB tersambung — buka mirror untuk mulai",
    "Open mirror screen" to "Buka Layar Mirror",
    "Disconnect" to "Putus",
    "USB device" to "Perangkat USB",
    "Connect" to "Hubungkan",
    "Grant permission" to "Berikan Izin",
    "No ADB device detected yet" to "Belum ada perangkat ADB terdeteksi",
    "Plug in an OTG cable, enable USB debugging, then tap Refresh."
        to "Colokkan kabel OTG, aktifkan USB debugging, lalu tekan Segarkan.",
    "Quick setup" to "Persiapan singkat",
    "Open Settings → About phone → tap “Build number” 7 times."
        to "Buka Pengaturan → Tentang → ketuk “Build number” 7×.",
    "Enable USB debugging (and Wireless debugging for wireless use)."
        to "Aktifkan USB debugging (dan Wireless debugging bila nirkabel).",
    "Plug in the OTG cable, allow USB access, then Start mirror."
        to "Tancapkan kabel OTG, izinkan akses USB, lalu Mulai Mirror.",

    // ------------------------------------------------------------- nirkabel
    "Wireless" to "Nirkabel",
    "Pairing stores the device security key, not its IP address — so the address is still typed manually when connecting. It only needs to be done once per pair of devices."
        to "Pairing menyimpan kunci keamanan perangkat, bukan alamat IP — jadi alamat tetap diisi manual saat Connect. Cukup dilakukan sekali untuk tiap pasangan perangkat.",
    "1. Pairing (Android 11+)" to "1. Pairing (Android 11+)",
    "The pairing port appears in the Pair device with pairing code dialog and differs from the connect port. Open Settings → Developer options → Wireless debugging to see it."
        to "Port pairing muncul di dialog Pair device with pairing code dan berbeda dengan port Connect. Buka Pengaturan → Developer options → Wireless debugging untuk membukanya.",
    "Pairing code" to "Kode pairing",
    "6 digits" to "6 digit",
    "Pairing port" to "Port pairing",
    "Enter the device IP address first" to "Isi alamat IP dulu",
    "Pairing succeeded" to "Pairing berhasil",
    "2. Connect" to "2. Connect",
    "Device IP" to "IP perangkat",
    "Port" to "Port",
    "This IP and port appear on the main Wireless debugging screen, next to the device name. Do not use the pairing port."
        to "IP dan port ini tampil di layar utama Wireless debugging, di samping nama perangkat. Jangan pakai port pairing.",
    "Close" to "Tutup",

    // ------------------------------------------------------------- mirror
    "Clipboard requested" to "Clipboard diminta",
    "Screenshot saved to Pictures/Scrcpy" to "Screenshot tersimpan di Pictures/Scrcpy",
    "Screenshot failed" to "Screenshot gagal",
    "Camera torch on" to "Senter kamera menyala",
    "Camera torch off" to "Senter kamera mati",
    "Camera zoomed in" to "Zoom kamera diperbesar",
    "Camera zoomed out" to "Zoom kamera diperkecil",
    "Switching camera…" to "Beralih arah kamera…",
    "Video reset" to "Video di-reset",
    "Screen turned off" to "Layar dimatikan",
    "Text sent" to "Teks dikirim",
    "Preparing scrcpy-server…" to "Menyiapkan scrcpy-server…",
    "Opening scrcpy-server on the device…" to "Membuka scrcpy-server di perangkat…",
    "Waiting for video frames…" to "Menunggu frame video…",
    "Decoder is preparing the surface" to "Decoder menyiapkan surface",
    "Session failed" to "Sesi gagal",
    "Try again" to "Coba lagi",
    "No active session" to "Tidak ada sesi aktif",
    "Go back to the home screen and pick a device" to "Kembali ke layar utama untuk memilih perangkat",
    "Send text" to "Kirim teks",
    "More options" to "Menu lainnya",
    "Paste clipboard" to "Ambil clipboard",
    "Screenshot" to "Screenshot",
    "Reset video" to "Reset video",
    "Turn torch off" to "Matikan senter",
    "Turn torch on" to "Nyalakan senter",
    "Zoom in" to "Perbesar",
    "Zoom out" to "Perkecil",
    "Switch camera" to "Ganti kamera",
    "End session" to "Putuskan sesi",
    "Send text to the device" to "Kirim teks ke perangkat",
    "Text (max. 300 characters)" to "Teks (maks. 300 karakter)",
    "Send" to "Kirim",
    "Cancel" to "Batal",
    "Mirror active" to "Mirror aktif",

    // --------------------------------------------------------- status sesi
    "No ADB connection yet" to "Belum ada koneksi ADB",
    "Starting scrcpy-server…" to "Menjalankan scrcpy-server…",
    "ADB device" to "Perangkat ADB",
    "USB permission needed — unplug & replug, or try again"
        to "Izin USB diperlukan — cabut & tancap ulang atau ulangi",
    "ADB handshake failed" to "Handshake ADB gagal",
    "Connect an ADB device first" to "Hubungkan perangkat ADB dulu",

    // ------------------------------------------------------- galat nirkabel
    "TLS handshake failed — this app has not paired with the device yet. Open Settings → Wireless debugging → Pair device with pairing code, enter the pairing port and the 6-digit code, then tap Pair. After that tap Connect using the port shown on the Wireless debugging screen."
        to "TLS handshake gagal — perangkat belum di-pair dengan aplikasi ini. Buka Pengaturan → Wireless debugging → Pair device with pairing code, isi port pairing + kode 6 digit lalu tekan Pair. Setelah itu tekan Connect memakai port di layar Wireless debugging.",
    "Cannot reach {}. Make sure Wireless debugging is still enabled and both devices are on the same Wi-Fi network. Note: the Wireless debugging port changes every time it is turned off — check the port again on the Wireless debugging screen."
        to "Tidak bisa tersambung ke {}. Pastikan Wireless debugging masih aktif dan kedua perangkat satu jaringan Wi-Fi. Catatan: port Wireless debugging berubah tiap kali dihidupkan ulang — cek lagi portnya di layar Wireless debugging.",

    // ------------------------------------------------------------ navigasi
    "Recent apps" to "Aplikasi terbaru",
    "Notification panel" to "Panel notifikasi",
    "Rotate screen" to "Rotasi layar",
    "Turn screen off" to "Layar mati",

    // --------------------------------------------------------- notifikasi
    "Mirror session" to "Sesi Mirror",
    "Shown while a scrcpy session is running" to "Notifikasi saat sesi scrcpy berjalan",
    "ADB service" to "Layanan ADB",
    "Shown by the ADB connection service" to "Notifikasi untuk layanan koneksi ADB",

    // --------------------------------------------------------- pilihan preset
    "Reload" to "Muat ulang",
    "Loading settings…" to "Memuat pengaturan…",
    "Write activity log" to "Tulis catatan aktivitas",
    "Save every action to scrcpy.log so it can be inspected later. Crash reports are always written."
        to "Simpan setiap aksi ke scrcpy.log agar bisa diperiksa nanti. Laporan crash tetap selalu ditulis.",
    "Custom…" to "Kustom…",
    "Custom value" to "Nilai kustom",
    "Device default" to "Default perangkat",
    "30 seconds" to "30 detik",
    "60 seconds" to "60 detik",
    "5 minutes" to "5 menit",
    "Software (sw)" to "Perangkat lunak (sw)",
    "Output (output)" to "Keluaran (output)",
    "Playback (playback)" to "Pemutaran (playback)",
    "Microphone (mic)" to "Mikrofon (mic)",
    "Mirror (0x1000)" to "Cermin (0x1000)",
    "Mirror + 90 (0x1090)" to "Cermin + 90 (0x1090)",
    "Mirror + 180 (0x10e0)" to "Cermin + 180 (0x10e0)",
    "Mirror + 270 (0x1110)" to "Cermin + 270 (0x1110)",

    "Active mirror session" to "Sesi mirror aktif",
    "Tells you that a scrcpy session is running" to "Notifikasi sesi scrcpy yang sedang berjalan",
    "(no output)" to "(tidak ada keluaran)",
)
