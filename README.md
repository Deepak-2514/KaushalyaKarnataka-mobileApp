# 🔧 Kaushalya Karnataka
### *Connecting Skilled Workers with Karnataka's Households & Businesses*

[![Android](https://img.shields.io/badge/Platform-Android-green?style=for-the-badge&logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple?style=for-the-badge&logo=kotlin)](https://kotlinlang.org)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-orange?style=for-the-badge&logo=firebase)](https://firebase.google.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue?style=for-the-badge&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-red?style=for-the-badge)](LICENSE)

---

## 📱 About The Project

**Kaushalya Karnataka** is a full-stack Android application that digitally empowers skilled workers across Karnataka — electricians, plumbers, carpenters, painters, and more — by giving them a platform to showcase their skills and receive job requests directly from households and businesses.

> Built as the capstone project of a 90-day Android + Generative AI internship at **MindMatrix.io (CL Infotech Pvt. Ltd.), Bangalore**.

---

## 🎯 Problem Statement

Karnataka has millions of skilled workers who:
- ❌ Have **no digital presence** to showcase their skills
- ❌ Rely entirely on **word-of-mouth** for work
- ❌ Have **no way to receive job requests** digitally

Households and businesses:
- ❌ Cannot **find or verify** local skilled workers
- ❌ Have **no way to communicate** before hiring
- ❌ Have **no review system** to judge quality

**Kaushalya Karnataka solves all of this** in one app. 

---

## ✨ Features

| Feature | Description |
|--------|-------------|
| 🔐 **Google Sign-In** | Secure authentication via Firebase Auth |
| 🔍 **Worker Discovery** | Search and filter workers by trade and location |
| 👷 **Worker Profiles** | Detailed profiles with skills, experience, rating, and photo |
| 📩 **Hire Requests** | Send, accept, or reject job requests with real-time status |
| 💬 **Real-time Chat** | Live in-app messaging between employer and worker |
| 📋 **My Hires** | Full hiring history for employers |
| 🖼️ **Photo Upload** | Profile and portfolio images via Firebase Storage |
| 🧭 **Category Filter** | Filter by Electrician, Plumber, Carpenter, Painter & more |

---

## 🛠️ Tech Stack

```
📱 Language       →  Kotlin
🎨 UI Framework   →  Jetpack Compose (Material 3)
🔐 Auth           →  Firebase Authentication + Google Sign-In
☁️  Database       →  Cloud Firestore (real-time)
🗂️  Storage        →  Firebase Storage
🧭 Navigation     →  Jetpack Navigation Compose
🖼️  Image Loading  →  Coil-Compose
🏗️  Architecture   →  MVVM (ViewModel + StateFlow + Coroutines)
🔧 Build System   →  Gradle (Kotlin DSL)
🤖 Dev Aid        →  Generative AI (code generation & prototyping)
```

---

## 📸 App Workflow.

> *Login → Home → Worker Profile → Chat → Requests*

| Login | Home | Worker Profile | Chat |
|-------|------|---------------|------|
| Sign in with Google | Search & Filter Workers | View Skills & Hire | Real-time Messaging |

---

## 🗂️ Project Structure

```
KaushalyaKarnataka/
├── app/
│   ├── src/main/
│   │   ├── java/com/kaushalyakarnataka/app/
│   │   │   ├── ui/screens/         # All Compose screens
│   │   │   ├── viewmodel/          # ViewModels (MVVM)
│   │   │   ├── repository/         # Firestore & Auth repos
│   │   │   ├── model/              # Data classes
│   │   │   ├── navigation/         # Nav graph
│   │   │   └── FirebaseBootstrap.kt
│   │   └── res/                    # Resources
│   ├── build.gradle.kts            # App-level Gradle
│   └── google-services.json        # ⚠️ NOT in repo — add manually
├── build.gradle.kts                # Project-level Gradle
├── local.properties                # ⚠️ NOT in repo — add manually
└── settings.gradle.kts
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio (Hedgehog or later)
- JDK 17
- Android device or emulator (API 26+)
- A Firebase project ([create one here](https://console.firebase.google.com))

### Installation

**1. Clone the repository**
```bash
git clone https://github.com/Deepak-2514/KaushalyaKarnataka-mobileApp.git
cd KaushalyaKarnataka-mobileApp
```

**2. Set up Firebase**
- Go to [Firebase Console](https://console.firebase.google.com)
- Create a new project (use region `us-central1`)
- Add an Android app with package name: `com.kaushalyakarnataka.app`
- Enable **Authentication** → Google Sign-In
- Create **Firestore Database** → Start in test mode
- Enable **Firebase Storage**
- Download `google-services.json` → place it in `app/`

**3. Configure local.properties**

Create a `local.properties` file in the project root:
```properties
sdk.dir=YOUR_ANDROID_SDK_PATH
WEB_CLIENT_ID=your-web-client-id.apps.googleusercontent.com
FIREBASE_API_KEY=your-firebase-api-key
FIREBASE_PROJECT_ID=your-firebase-project-id
FIREBASE_APP_ID=your-firebase-app-id
FIREBASE_STORAGE_BUCKET=your-project.appspot.com
```

**4. Add SHA-1 fingerprint to Firebase**
```bash
./gradlew signingReport
```
Copy the `SHA1` value → Firebase Console → Project Settings → Your App → Add fingerprint

**5. Build and Run**
- Open project in Android Studio
- Click **Run ▶** or press `Shift + F10`

---

## 🗃️ Firestore Data Structure

```
📦 Firestore
 ┣ 📂 users
 ┃  └── {uid} → name, email, photoUrl, role, phone, location
 ┣ 📂 workers
 ┃  └── {uid} → name, trade, experience, rating, bio, photoUrl, available
 ┣ 📂 requests
 ┃  └── {requestId} → employerId, workerId, status, message, timestamp
 ┗ 📂 chats
    └── {chatId}
         └── 📂 messages
              └── {msgId} → senderId, text, timestamp
```

---

## 🔒 Security

- All API keys and Firebase credentials are stored in `local.properties` (excluded from git)
- `google-services.json` is excluded via `.gitignore`
- Keys are injected at build time via `BuildConfig`
- Never hardcode secrets in source files

---

## 🌱 Future Enhancements

- [ ] 🌐 Multi-language support (Hindi and others)
- [ ] 🔔 Push notifications via Firebase Cloud Messaging
- [ ] ⭐ Worker rating and review system
- [ ] 📍 Location-based worker search (Google Maps API)
- [ ] 📱 OTP phone verification for workers
- [ ] 💳 Payment gateway integration
- [ ] 🤖 AI-powered worker recommendation engine

---


<p align="center">
  Made with ❤️ in Karnataka, India 🇮🇳
</p>
