# Floatify - Floating Multi-App Launcher & Notification Aggregator

## Overview

Floatify is an Android application that enables multitasking on phones with limited multitasking capabilities through a floating bubble interface. The app provides quick access to favorite applications via an always-visible floating bubble overlay, with automatic theme switching and minimal RAM usage. Additionally, it features a **NotificationListenerService** to capture and aggregate notifications from all apps (WhatsApp, TikTok, Instagram, etc.) in one place.

**Key Features:**
- Floating bubble overlay that appears above other apps
- Quick app launcher accessible from the bubble
- **Notification Listener (Penangkap Notifikasi)** - Captures and aggregates notifications from all apps
- Tabbed bubble menu with Apps and Notifications tabs
- Automatic theme switching (light/dark based on system settings)
- Lightweight performance optimization
- Auto-start capability after device restart
- Modern Material Design 3 UI
- Full-screen dark/light theme support

**Target Platform:** Android 8.0 (Oreo) and newer

## Recent Changes (December 2024)

### v2.1.0 - NotificationListenerService Feature

**New Features:**
- **NotificationListenerService** implementation (`FloatifyNotificationListener.kt`)
  - Captures notifications from all apps (WhatsApp, TikTok, Instagram, etc.)
  - Stores up to 50 most recent notifications
  - Real-time notification updates
  - Supports notification filtering by monitored apps
- **Tabbed Bubble Menu** with Apps and Notifications tabs
- **Notification item layout** for displaying captured notifications
- **Notification Access Permission** in onboarding flow
- New `ic_notification.xml` and `ic_notification_access.xml` vector icons

**Bug Fixes:**
- Fixed AppListAdapter checkbox listener issue causing incorrect state when recycling ViewHolders
- Fixed potential memory leaks with BroadcastReceiver in FloatBubbleService

**Code Improvements:**
- Added `NotificationInfo` data model for notification storage
- Added `NotificationAdapter` for RecyclerView display
- Updated `PrefsManager` with notification-related preferences
- Updated bubble badge to show notification count

### v2.0.0 - Major UI/UX Update

**Theme & UI Improvements:**
- Complete redesign with Material Design 3 principles
- Full-screen dark/light theme that applies consistently across the entire app
- Modern color palette with indigo primary and purple secondary colors
- Improved card designs with subtle shadows and borders
- Better typography hierarchy with proper font weights

**App List Improvements:**
- Removed 50 app limit - now shows ALL installed apps
- Better app item layout with cleaner icon display
- App count indicator showing selected apps

**Bubble Service Improvements:**
- Enhanced stability to prevent crashes
- Better error handling and null safety
- Improved touch detection for taps vs drags
- Handler-based UI updates for thread safety
- Better notification management

**Layout Updates:**
- Modernized onboarding screens with consistent theming
- Improved MainActivity layout with CoordinatorLayout
- Better bubble menu design with app count display
- Added empty state for when no apps are selected

## User Preferences

Preferred communication style: Simple, everyday language (Bahasa Indonesia).

## System Architecture

### Mobile Application Architecture

**Platform:** Native Android application
- Minimum SDK: Android 8.0 (Oreo/API 26)
- Build system: Gradle with Kotlin DSL
- Primary language: Kotlin
- UI: Material Design 3 Components

**Core Components:**

1. **FloatifyApp** - Application class
   - Global exception handler
   - Notification channel creation
   - Theme initialization

2. **FloatBubbleService** - Foreground service
   - Implements Android's overlay window permission (SYSTEM_ALERT_WINDOW)
   - Manages persistent floating bubble UI element
   - Handles touch interactions and dragging behavior
   - Tabbed menu popup for quick apps and notifications
   - Notification broadcast receiver for real-time updates

3. **FloatifyNotificationListener** - NotificationListenerService
   - Captures notifications from all apps
   - Filters by monitored apps (optional)
   - Stores up to 50 recent notifications
   - Broadcasts notification changes to FloatBubbleService

4. **MainActivity** - Dashboard
   - App list with all installed applications
   - Theme selection (Auto/Light/Dark)
   - Bubble activation/deactivation

5. **OnboardingActivity** - First-time setup
   - Welcome screen with feature highlights
   - Permission request flow (overlay, notification, notification access)
   - Theme selection

6. **PrefsManager** - Preferences manager
   - Selected apps storage
   - Theme mode persistence
   - Bubble state tracking
   - Notification listener settings
   - Monitored apps for notifications

**Design Patterns:**
- Service-based architecture for background floating bubble
- Broadcast receivers for system events (boot completion)
- Intent-based communication for app launching
- View binding for type-safe UI access

### Project Structure

```
app/src/main/
├── java/com/floatify/app/
│   ├── FloatifyApp.kt
│   ├── data/
│   │   ├── AppInfo.kt
│   │   └── NotificationInfo.kt
│   ├── service/
│   │   ├── BootReceiver.kt
│   │   ├── FloatBubbleService.kt
│   │   └── FloatifyNotificationListener.kt
│   ├── ui/
│   │   ├── AppListAdapter.kt
│   │   ├── MainActivity.kt
│   │   ├── MenuAppAdapter.kt
│   │   ├── NotificationAdapter.kt
│   │   ├── OnboardingActivity.kt
│   │   └── SplashActivity.kt
│   └── util/
│       └── PrefsManager.kt
└── res/
    ├── drawable/
    ├── layout/
    ├── values/
    └── values-night/
```

### Build & Distribution

**CI/CD Pipeline:**
- GitHub Actions workflow for automated APK building
- Artifact generation for distribution
- Support for both manual and automated builds

**Build Variants:**
- Debug builds via Gradle (`assembleDebug`)
- Release builds for production distribution

## External Dependencies

### Android Framework Dependencies

1. **Core Android Services**
   - WindowManager for overlay management
   - PackageManager for app information retrieval
   - ActivityManager for app launching

2. **Required Permissions**
   - `SYSTEM_ALERT_WINDOW` - Display overlay bubble
   - `FOREGROUND_SERVICE` - Run background service
   - `FOREGROUND_SERVICE_SPECIAL_USE` - Android 14+ requirement
   - `POST_NOTIFICATIONS` - Show service notification
   - `RECEIVE_BOOT_COMPLETED` - Auto-start functionality
   - `QUERY_ALL_PACKAGES` - Discover installed apps
   - `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` - Prevent service kill

### Libraries

- AndroidX Core KTX
- Material Design 3 Components
- AndroidX ConstraintLayout
- AndroidX CoordinatorLayout
- AndroidX ViewPager2
- AndroidX SplashScreen

### Build Infrastructure

1. **GitHub Actions**
   - Automated APK compilation workflow
   - Artifact storage and distribution
   - Build trigger on code push

2. **Gradle Build System**
   - Android Gradle Plugin
   - Kotlin DSL configuration
   - Dependency management
