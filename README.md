# 🔬 JARVISH: Elite Android AI Assistant

JARVISH is a next-generation, production-grade Android voice assistant designed with a futuristic **Cyber-Glass UI** and powered by Gemini 2.5 Flash. It is built around a native voice loop (STT → LLM → TTS) and features a rich tool orchestration framework for deep system automation, scoped accessibility automation, local RAG knowledge execution, and secure Keystore credentials protection.

---

## 🏗️ Architectural Overview & Design Patterns

The codebase is structured under a layered, reactive architecture with clear boundaries between the UI, business logic, system services, and background agents:

```
┌─────────────────────────────────────────────┐
│  UI Layer (Jetpack Compose Screens)         │  ← HomeScreen, ToolsScreen, ControlScreen, etc.
├─────────────────────────────────────────────┤
│  State / Flow Layer (StateFlow, Coroutines) │  ← PermissionManager, SafetyLayer, ContextEngine
├─────────────────────────────────────────────┤
│  Brain / AI Layer                           │  ← LlmClient, IntentRouter, ToolRegistry
├─────────────────────────────────────────────┤
│  Tool / Action Layer                        │  ← AppLauncherTool, PhoneActionsTool, ScopedAccessibilityService
├─────────────────────────────────────────────┤
│  Persistence / Security Layer               │  ← MemoryManager (Room), VaultManager (EncryptedSharedPreferences)
├─────────────────────────────────────────────┤
│  System Services Layer                      │  ← NotificationListener, ContextEngine, SpeechToTextManager
└─────────────────────────────────────────────┘
```

### Key Design Patterns Implemented:
*   **Repository Pattern:** Managed via [MemoryManager](file:///Users/prabeshshah/Desktop/jarvis/app/src/main/java/com/jarvis/memory/MemoryManager.kt) abstracting the Room database.
*   **Strategy Registry:** Standardized under [ToolRegistry](file:///Users/prabeshshah/Desktop/jarvis/app/src/main/java/com/jarvis/brain/ToolRegistry.kt) to dynamic parameter extraction and execution routing.
*   **Command Pattern:** Realized in [IntentRouter](file:///Users/prabeshshah/Desktop/jarvis/app/src/main/java/com/jarvis/brain/IntentRouter.kt) to execute multi-step workflows.
*   **Observer Pattern:** System changes monitored reactively via network callbacks and broadcast receivers in [ContextEngine](file:///Users/prabeshshah/Desktop/jarvis/app/src/main/java/com/jarvis/context/ContextEngine.kt).
*   **WeakReference Safety:** Memory leak protections implemented within [PermissionManager](file:///Users/prabeshshah/Desktop/jarvis/app/src/main/java/com/jarvis/safety/PermissionManager.kt) and [ScopedAccessibilityService](file:///Users/prabeshshah/Desktop/jarvis/app/src/main/java/com/jarvis/accessibility/ScopedAccessibilityService.kt).

---

## ⚡ Main Subsystems

### 1. Futuristic Cyber-Glass UI & Theming
*   **Screens:** Built entirely in Jetpack Compose, featuring:
    *   [HomeScreen](file:///Users/prabeshshah/Desktop/jarvis/app/src/main/java/com/jarvis/ui/screens/HomeScreen.kt): Rotating telemetry HUD dial, sin-wave Canvas animations, pulsing microphone voice button, and floating docks.
    *   [ToolsScreen](file:///Users/prabeshshah/Desktop/jarvis/app/src/main/java/com/jarvis/ui/screens/ToolsScreen.kt): Active registration metrics and control configurations.
    *   [ControlScreen](file:///Users/prabeshshah/Desktop/jarvis/app/src/main/java/com/jarvis/ui/screens/ControlScreen.kt): Subsystem monitoring dashboard.
    *   [FilesScreen](file:///Users/prabeshshah/Desktop/jarvis/app/src/main/java/com/jarvis/ui/screens/FilesScreen.kt): Local document indexing interface.
    *   [ProfileScreen](file:///Users/prabeshshah/Desktop/jarvis/app/src/main/java/com/jarvis/ui/screens/ProfileScreen.kt): Application preference center.
*   **Themes:** 5 glowing dark & light configurations (Matrix Red, Ocean Blue, Tech Green, Purple Void, Orange Heat) defined in [Theme.kt](file:///Users/prabeshshah/Desktop/jarvis/app/src/main/java/com/jarvis/ui/theme/Theme.kt).

### 2. Native Walkie-Talkie Voice Loop
*   [SpeechToTextManager](file:///Users/prabeshshah/Desktop/jarvis/app/src/main/java/com/jarvis/voice/SpeechToTextManager.kt): Manages low-latency speech transcription utilizing Android's native `SpeechRecognizer` service.
*   [TextToSpeechManager](file:///Users/prabeshshah/Desktop/jarvis/app/src/main/java/com/jarvis/voice/TextToSpeechManager.kt): Delivers immediate voice synthesis back to the user with audio ducking support.

### 3. LLM Brain & Tool Router
*   [LlmClient](file:///Users/prabeshshah/Desktop/jarvis/app/src/main/java/com/jarvis/brain/LlmClient.kt): Performs direct HTTP REST requests to Google Gemini endpoint APIs without loading heavy external libraries.
*   [IntentRouter](file:///Users/prabeshshah/Desktop/jarvis/app/src/main/java/com/jarvis/brain/IntentRouter.kt): Receives JSON tool specifications from the LLM response and routes commands sequentially.
*   [ToolRegistry](file:///Users/prabeshshah/Desktop/jarvis/app/src/main/java/com/jarvis/brain/ToolRegistry.kt): Registers features conforming to the [Tool](file:///Users/prabeshshah/Desktop/jarvis/app/src/main/java/com/jarvis/brain/Tool.kt) layout.

### 4. Native Automation & Intent Integration
*   [AppLauncherTool](file:///Users/prabeshshah/Desktop/jarvis/app/src/main/java/com/jarvis/tools/AppLauncherTool.kt): Safely resolves package targets to open local apps.
*   [PhoneActionsTool](file:///Users/prabeshshah/Desktop/jarvis/app/src/main/java/com/jarvis/tools/PhoneActionsTool.kt): Operates system calls, SMS triggers, alarms, media keys, and hardware flashlight toggles.
*   [IntentResolverHelper](file:///Users/prabeshshah/Desktop/jarvis/app/src/main/java/com/jarvis/tools/IntentResolverHelper.kt): Safe wrapper parsing that guards launch intent failures.
*   [NotificationListener](file:///Users/prabeshshah/Desktop/jarvis/app/src/main/java/com/jarvis/notifications/NotificationListener.kt): Scans incoming status notifications and supports automatic inline replies via [NotificationReplyHelper](file:///Users/prabeshshah/Desktop/jarvis/app/src/main/java/com/jarvis/notifications/NotificationReplyHelper.kt).

### 5. Scoped Accessibility Service
*   [ScopedAccessibilityService](file:///Users/prabeshshah/Desktop/jarvis/app/src/main/java/com/jarvis/accessibility/ScopedAccessibilityService.kt): Handles on-screen element interaction, clicks, and text typing automation.
*   **Privacy Protections:** The service automatically ignores and bypasses text fields containing passwords, credit card numbers, OTP keys, and PIN indicators.
*   **Configuration:** Configured under [accessibility_service_config.xml](file:///Users/prabeshshah/Desktop/jarvis/app/src/main/res/xml/accessibility_service_config.xml).

### 6. Local RAG & Document Search
*   [LocalRagEngine](file:///Users/prabeshshah/Desktop/jarvis/app/src/main/java/com/jarvis/knowledge/LocalRagEngine.kt): Implements a lightweight, local text indexing pipeline using TF-IDF token scoring, chunking, and stop-word filtering.
*   [OcrEngine](file:///Users/prabeshshah/Desktop/jarvis/app/src/main/java/com/jarvis/vision/OcrEngine.kt): Integrates on-device ML Kit Text Recognition for screen content understanding.

### 7. Security & Key Vaulting
*   [VaultManager](file:///Users/prabeshshah/Desktop/jarvis/app/src/main/java/com/jarvis/security/VaultManager.kt): Secures sensitive API keys and session information using Android Keystore-backed AES-256-GCM.
*   **Resiliency Fallback:** Includes a 3-tier corruption recovery plan for handling Keystore decrypt failures (`AEADBadTagException`) without application crash loops.
*   [SafetyLayer](file:///Users/prabeshshah/Desktop/jarvis/app/src/main/java/com/jarvis/safety/SafetyLayer.kt): Provides a global emergency stop command execution guard.
*   [PermissionManager](file:///Users/prabeshshah/Desktop/jarvis/app/src/main/java/com/jarvis/safety/PermissionManager.kt): Manages runtime prompt Rationales and lifecycle bindings.

---

## 🛠️ Setup & Configuration

To compile and build JARVISH, you must configure the Gemini API target details.

### 1. Build Environment Injection
Create a `.env` file in the root project folder (same folder as `settings.gradle.kts`):

```env
GEMINI_API_KEY=your_gemini_api_studio_key_here
LLM_BASE_URL=https://generativelanguage.googleapis.com/v1beta
LLM_MODEL=gemini-2.5-flash
```

*(Note: The build scripts configured in [build.gradle.kts](file:///Users/prabeshshah/Desktop/jarvis/app/build.gradle.kts) automatically read these env parameters and expose them directly within the compiler generated `BuildConfig` class.)*

### 2. Android Build System
Ensure you have the Android SDK (Compile API 35) and JDK 17 installed. Compile the debug build target using Gradle:

```bash
./gradlew assembleDebug
```

---

## 📱 Hardware Testing Guidelines

> [!WARNING]
> **DO NOT USE THE ANDROID EMULATOR.** Android emulators lack proper microphone recording mapping and Google Speech Recognizer integrations. Running the voice walkie-talkie loop on an emulator will result in silent failures.

1.  Connect a **Physical Android Device** (API 26+) via USB.
2.  Enable **USB Debugging** in the device's Developer Options.
3.  Compile and run the project from Android Studio.
4.  Grant the application **Microphone** and **Notification Listener** permissions.
5.  Tap the glowing microphone on the Home Screen dashboard and talk to JARVISH!

---

## 🎯 Master Plan & Implementation Status

For the detailed 10-phase roadmap and target development order, refer to the [plan.md](file:///Users/prabeshshah/Desktop/jarvis/plan.md) and [skills.md](file:///Users/prabeshshah/Desktop/jarvis/skills.md) references.
*   **Phase 1 (Core Base):** Completed ✅
*   **Phase 2 (Control Layer):** Fully implemented, currently integrating deep-linking paths. ⏳
*   **Phases 3-9 (Automation, Local Knowledge, OCR, & Security):** Framework engines fully implemented and being optimized. 🔄

