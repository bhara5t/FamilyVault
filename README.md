# 🔒 Family Vault

A secure, offline-first document vault for storing and managing your family's important documents with end-to-end encryption.

[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-7.0%2B-brightgreen.svg)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9%2B-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-1.7%2B-blue.svg)](https://developer.android.com/jetpack/compose)

---

## 📱 Features

- 🔐 **Secure Storage** — All documents are encrypted using AES-256 encryption
- 📂 **Category Management** — Organize documents by categories (Aadhaar, PAN, Medical, etc.)
- 🔍 **Smart Search** — Quickly find documents by name or category
- 📤 **Export / Import** — Backup and restore all your documents with a single ZIP file
- 👁️ **Document Preview** — View images and PDFs directly in the app
- 📲 **Share Support** — Share documents directly to WhatsApp and other apps
- 🌙 **Dark Theme** — Beautiful dark interface for comfortable viewing
- 🔑 **PIN Protection** — 5-digit PIN to secure access to your vault
- 📥 **Download** — Save decrypted copies to your device when needed

---

## 📥 Download

### Latest Release: v1.0.0

| Version | Release Date | Download |
|---------|--------------|----------|
| v1.0.0 | 2025-01-15 | [📦 FamilyVault-v1.0.0.apk](https://github.com/bhara5t/FamilyVault/releases/download/v1.0.0/FamilyVault-v1.0.0.apk) |

> Check the [Releases Page](https://github.com/bhara5t/FamilyVault/releases) for all versions and release notes.

---

## 🔧 Installation

### Method 1: Direct APK Download

1. Download the latest APK from the [Releases](https://github.com/bhara5t/FamilyVault/releases) section
2. On your Android device, go to **Settings → Security → Unknown Sources** and enable it
3. Open the downloaded APK file and tap **Install**
4. Launch Family Vault and enter the default PIN: `00000`

### Method 2: Build from Source

#### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 11 or higher
- Android SDK with API 34+
- Kotlin 1.9+

#### Steps

```bash
git clone https://github.com/bhara5t/FamilyVault.git
cd family-vault
```

1. Launch **Android Studio**
2. Select **Open an Existing Project**
3. Navigate to the cloned folder and open it
4. Wait for Gradle sync to complete
5. Run via **Build → Generate Signed APK** or connect a device and hit ▶️

---

## 🔑 Default PIN Configuration

The default PIN is `00000`.

**To change it:**

1. Open `app/src/main/java/com/example/familyvault/security/PinScreen.kt`
2. Find:
```kotlin
val fixedPin = "00000"
```
3. Replace with your desired 5-digit PIN:
```kotlin
val fixedPin = "12345"
```
4. Rebuild the app

---

## 🚀 Quick Start Guide

### First Time Setup
1. Launch the app and enter the default PIN `00000`
2. The dashboard appears — you're ready to add documents!

### Adding Documents
1. Tap the **+ FAB** button
2. Enter a document name
3. Select a category
4. Pick a file from your device
5. Tap **Add** to encrypt and save

### Managing Categories
1. Tap the category icon 📁 in the top bar
2. Add new categories with the **+** button
3. Delete categories by tapping the delete icon
4. Tap **Save** to apply changes

### Exporting Your Vault
1. Tap the export icon ⬆️ in the top bar
2. ZIP file saves to `Downloads/FamilyVault/`
3. Transfer this file to your new device

### Importing Your Vault
1. Install Family Vault on the new device
2. Tap the import icon ⬇️ in the top bar
3. Select the backup ZIP file
4. All documents restore automatically

---

## 🛠️ Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Kotlin | 1.9+ | Programming Language |
| Jetpack Compose | 1.7+ | UI Framework |
| Material 3 | 1.2+ | Design System |
| Room Database | 2.6.1 | Local Storage |
| Android Security Crypto | 1.1.0-alpha06 | Encryption |
| Coil | 2.6.0 | Image Loading |
| AndroidPdfViewer | 3.2.0-beta.3 | PDF Rendering |
| Kotlin Coroutines | 1.7+ | Async Operations |

---

## 📁 Project Structure

```
app/src/main/java/com/example/familyvault/
├── MainActivity.kt           
├── DashboardActivity.kt      
├── MemberDetailActivity.kt   
├── PreviewActivity.kt        
├── data/
│   ├── AppDatabase.kt        
│   ├── FamilyDao.kt          
│   └── FamilyMember.kt       
├── security/
│   ├── PinScreen.kt
│   ├── PinStorage.kt    
│   └── FileSecurity.kt       
└── ui/theme/
    └── Color.kt              
```

---

## 🔒 Security Notes

- **Encryption:** AES-256 encryption for all stored documents
- **Local Only:** All data stays on your device — no cloud, no servers
- **PIN Protection:** 5-digit PIN required for every access
- **Export Security:** Exported documents are decrypted — handle with care

---

## 📝 License

This project is licensed under the [MIT License](LICENSE).

---

## 🙏 Acknowledgments

- [AndroidPdfViewer](https://github.com/barteksc/AndroidPdfViewer) — PDF rendering
- [Coil](https://github.com/coil-kt/coil) — Image loading
- [Material Design 3](https://m3.material.io/) — UI components

---

## 📞 Support

- 🐛 [Open an Issue](https://github.com/bhara5t/FamilyVault/issues)
- 📧 Email: bharatzt1258@gmail.com
