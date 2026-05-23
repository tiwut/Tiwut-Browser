# Tiwut Browser

Tiwut Browser is a fast, highly-customizable, and secure Android web browser built with Kotlin and Jetpack Compose. It focuses on providing power users with total control over their browsing experience, incorporating over 50 toggleable features including advanced privacy isolation, network management, and developer tools.

## Features

- **Privacy & Isolation:** Website Isolation Core, Do Not Track (DNT), Auto-Delete Browsing Data, Fingerprint Resistance, Strict HTTPS Enforcer, WebRTC Leak Protection.
- **Performance:** Hyper-Fast Engine, Aggressive Image Caching, Preload Hints, Hardware Acceleration, Block Autoplay Media, Lazy Load Assets.
- **Customization:** Glassy Transparent Theme, Custom Fonts Override, High Contrast Mode, Dark Mode for Webpages.
- **Network Management:** Proxy Gateway, Offline Page Rescue, Translate On-The-Fly.
- **Developer Tools:** DOM Storage Control, Service Worker Rules, Inspect Element integration, V8 Optimizer, Advanced Web Debugger.
- **Management Subsystems:** Export Configuration, Theme Engine Options, Storage & Cookie Manager, Website Permission Manager, Resource & Cache Manager.

## Architectural Overview

- **UI Framework:** Jetpack Compose (Material Design 3).
- **Core Technology:** `android.webkit.WebView` with extensive custom WebChromeClient and WebViewClient implementations tailored for extreme feature sets.
- **Storage:** Data persistence uses native Android SharedPreferences and Cookie/WebStorage APIs.

## Requirements

- Minimum SDK: 24
- Target SDK: 36
- Language: Kotlin

## Disclaimer
This project is an experimental platform for rapid prototyping of complex Web APIs directly into a native Android browser view.

## License
MIT License
