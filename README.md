# Floatify - Floating Multi-App Launcher

**Multitasking untuk Semua Ponsel** 🫧

Floatify adalah aplikasi Android yang memungkinkan ponsel dengan keterbatasan multitasking untuk menjalankan beberapa aplikasi sekaligus melalui floating bubble interface.

## Fitur Utama

- 🫧 **Floating Bubble** - Bubble mengambang yang selalu terlihat di atas aplikasi lain
- 📱 **Quick App Launcher** - Akses cepat ke aplikasi favorit dari bubble
- 🎨 **Tema Otomatis** - Mengikuti tema sistem (terang/gelap)
- ⚡ **Super Ringan** - Tidak membebani RAM ponsel
- 🔄 **Auto-Start** - Bubble otomatis aktif setelah restart ponsel

## Screenshots

Coming soon...

## Persyaratan

- Android 8.0 (Oreo) atau lebih baru
- Izin overlay (tampilkan di atas aplikasi lain)

## Cara Install

1. Download file APK dari [Releases](../../releases)
2. Izinkan instalasi dari sumber tidak dikenal
3. Install APK
4. Buka Floatify dan ikuti onboarding
5. Berikan izin overlay saat diminta
6. Aktifkan bubble dan pilih aplikasi!

## Build dari Source

### Menggunakan GitHub Actions (Recommended)

1. Fork repository ini
2. Pergi ke tab "Actions"
3. Jalankan workflow "Build Floatify APK"
4. Download APK dari Artifacts

### Build Manual

```bash
# Clone repository
git clone https://github.com/YOUR_USERNAME/floatify.git
cd floatify

# Build debug APK
./gradlew assembleDebug

# APK akan ada di: app/build/outputs/apk/debug/app-debug.apk
```

## Struktur Project

```
android-floatify/
├── app/
│   ├── src/main/
│   │   ├── java/com/floatify/app/
│   │   │   ├── ui/          # Activities & Adapters
│   │   │   ├── service/     # FloatBubbleService
│   │   │   ├── data/        # Data models
│   │   │   └── util/        # Preferences & helpers
│   │   ├── res/             # Resources (layouts, drawables, values)
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/wrapper/
```

## Teknologi

- **Language**: Kotlin
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Architecture**: Single Activity with Foreground Service

## Izin yang Digunakan

- `SYSTEM_ALERT_WINDOW` - Menampilkan bubble di atas aplikasi lain
- `FOREGROUND_SERVICE` - Menjaga bubble tetap aktif di background
- `POST_NOTIFICATIONS` - Menampilkan notifikasi service (Android 13+)
- `QUERY_ALL_PACKAGES` - Membaca daftar aplikasi terinstall
- `RECEIVE_BOOT_COMPLETED` - Auto-start setelah restart ponsel

## Lisensi

MIT License - Bebas digunakan dan dimodifikasi

## Kontribusi

Kontribusi sangat diterima! Silakan buat Pull Request atau Issue jika ada saran/bug.

---

**Dibuat dengan ❤️ untuk komunitas Android Indonesia**
