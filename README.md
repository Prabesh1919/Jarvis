# 🔬 JARVISH: Elite Android AI Assistant

JARVISH is a next-generation, production-grade Android voice assistant designed with a futuristic **Cyber-Glass UI** and powered by a **Hybrid Dual-LLM Architecture** (Gemini 2.5 Flash + OpenRouter Free-Tier Pool). It is built around a native voice loop (STT → LLM → TTS) and features a rich tool orchestration framework for deep system automation, autonomous multi-step agent execution, cross-device desktop bridging, scoped accessibility automation, local RAG knowledge execution, and secure Keystore credentials protection.

---

## 🆕 What's New (July 2026)

- 🔀 **Hybrid Dual-LLM Gateway** — New `OpenRouterClient` routes heavy reasoning tasks through a pool of free-tier models (Llama 3.3 70B, Gemma 4, Nemotron 120B) with automatic rate-limit failover, while Gemini handles real-time voice.
- 🧩 **Autonomous Multi-Step Agent Engine** — New `TaskPlanner` + `TaskQueue` system decomposes complex user requests into structured, dependency-aware execution plans with background WorkManager support.
- 🖥️ **Cross-Device Desktop Bridge** — New `DesktopBridgeService` enables bidirectional WebSocket communication between Android JARVISH and Mark-XXXIX-OR on Mac/PC for remote command dispatch.
- 👁️ **Real-Time Screen Awareness** — New `ScreenAwarenessEngine` provides continuous MediaProjection-based screen capture and ML vision analysis.
- 🛡️ **Enhanced Security & Code Review** — All new modules passed principal developer code review and QA audit (3 bugs found and fixed).

---

## 🏗️ Architectural Overview & Design Patterns

The codebase is structured under a layered, reactive architecture with clear boundaries between the UI, business logic, system services, and background agents:

```
┌─────────────────────────────────────────────────┐
│  UI Layer (Jetpack Compose Screens)             │  ← HomeScreen, ToolsScreen, ControlScreen, etc.
├─────────────────────────────────────────────────┤
│  State / Flow Layer (StateFlow, Coroutines)     │  ← PermissionManager, SafetyLayer, ContextEngine
├─────────────────────────────────────────────────┤
│  Brain / AI Layer (Dual-LLM Gateway)            │  ← LlmClient, OpenRouterClient, IntentRouter, ToolRegistry
├─────────────────────────────────────────────────┤
│  Agent / Planner Layer                          │  ← TaskPlanner, TaskQueue, AgentWorker
├─────────────────────────────────────────────────┤
│  Tool / Action Layer                            │  ← AppLauncherTool, PhoneActionsTool, ScopedAccessibilityService
├─────────────────────────────────────────────────┤
│  Cross-Device Bridge Layer                      │  ← DesktopBridgeService (WebSocket → Mark-XXXIX-OR)
├─────────────────────────────────────────────────┤
│  Persistence / Security Layer                   │  ← MemoryManager (Room), VaultManager (AES-256-GCM)
├─────────────────────────────────────────────────┤
│  System Services Layer                          │  ← NotificationListener, ContextEngine, SpeechToTextManager
└─────────────────────────────────────────────────┘
```

### Key Design Patterns Implemented:
*   **Repository Pattern:** Managed via `MemoryManager` abstracting the Room database.
*   **Strategy Registry:** Standardized under `ToolRegistry` for dynamic parameter extraction and execution routing.
*   **Command Pattern:** Realized in `IntentRouter` to execute multi-step workflows.
*   **Observer Pattern:** System changes monitored reactively via network callbacks and broadcast receivers in `ContextEngine`.
*   **Failover Pool Pattern:** Implemented in `OpenRouterClient` with rate-limit detection, model cooldowns, and automatic cascading to the next available free model.
*   **WeakReference Safety:** Memory leak protections implemented within `PermissionManager` and `ScopedAccessibilityService`.

---

## ⚡ Main Subsystems

### 1. Futuristic Cyber-Glass UI & Theming
*   **Screens:** Built entirely in Jetpack Compose, featuring:
    *   **HomeScreen**: Rotating telemetry HUD dial, sin-wave Canvas animations, pulsing microphone voice button, and floating docks.
    *   **ToolsScreen**: Active registration metrics and control configurations.
    *   **ControlScreen**: Subsystem monitoring dashboard.
    *   **FilesScreen**: Local document indexing interface.
    *   **ProfileScreen**: Application preference center with Desktop Bridge IP configuration.
*   **Themes:** 5 glowing dark & light configurations (Matrix Red, Ocean Blue, Tech Green, Purple Void, Orange Heat) defined in `Theme.kt`.

### 2. Native Walkie-Talkie Voice Loop
*   **SpeechToTextManager**: Manages low-latency speech transcription utilizing Android's native `SpeechRecognizer` service.
*   **TextToSpeechManager**: Delivers immediate voice synthesis back to the user with audio ducking support.

### 3. Hybrid Dual-LLM Brain & Tool Router
*   **LlmClient**: Performs direct HTTP REST requests to Google Gemini endpoint APIs for real-time voice and fast tool dispatch.
*   **OpenRouterClient** *(NEW)*: Routes heavy text, reasoning, and vision tasks through OpenRouter's free-tier model pool (Llama 3.3 70B, Gemma 4 31B, Nemotron 120B) with automatic rate-limit failover and 60s cooldown management.
*   **IntentRouter**: Receives JSON tool specifications from the LLM response and routes commands sequentially.
*   **ToolRegistry**: Registers features conforming to the `Tool` interface layout.

### 4. Autonomous Agent Execution Engine *(NEW)*
*   **TaskPlanner**: Deconstructs complex user prompts into structured JSON execution plans with ordered steps and dependency tracking.
*   **TaskQueue**: Manages step states (`PENDING`, `RUNNING`, `SUCCESS`, `FAILED`) with queue prioritization.
*   **AgentWorker**: Android `CoroutineWorker` extending WorkManager for persistent multi-step task execution even when the app is backgrounded or screen is off.

### 5. Cross-Device Desktop Bridge *(NEW)*
*   **DesktopBridgeService**: WebSocket client maintaining real-time bidirectional communication between Android JARVISH and Mark-XXXIX-OR desktop assistant.
*   **Capabilities:** Voice command on Android (e.g., *"Run Mac build and tell me when it passes"*) dispatches to desktop and streams status back to Android HUD.

### 6. Native Automation & Intent Integration
*   **AppLauncherTool**: Safely resolves package targets to open local apps.
*   **PhoneActionsTool**: Operates system calls, SMS triggers, alarms, media keys, and hardware flashlight toggles.
*   **IntentResolverHelper**: Safe wrapper parsing that guards launch intent failures.
*   **NotificationListener**: Scans incoming status notifications and supports automatic inline replies via `NotificationReplyHelper`.

### 7. Scoped Accessibility Service
*   **ScopedAccessibilityService**: Handles on-screen element interaction, clicks, and text typing automation.
*   **Privacy Protections:** The service automatically ignores and bypasses text fields containing passwords, credit card numbers, OTP keys, and PIN indicators.

### 8. Real-Time Screen Awareness *(NEW)*
*   **ScreenAwarenessEngine**: Continuous real-time screen capture via Android `MediaProjectionManager` with ML vision model streaming for active app analysis.
*   **OcrEngine**: Integrates on-device ML Kit Text Recognition for screen content understanding.

### 9. Local RAG & Document Search
*   **LocalRagEngine**: Implements a lightweight, local text indexing pipeline using TF-IDF token scoring, chunking, and stop-word filtering.

### 10. Security & Key Vaulting
*   **VaultManager**: Secures sensitive API keys and session information using Android Keystore-backed AES-256-GCM.
*   **Resiliency Fallback:** Includes a 3-tier corruption recovery plan for handling Keystore decrypt failures (`AEADBadTagException`) without application crash loops.
*   **SafetyLayer**: Provides a global emergency stop command execution guard.
*   **PermissionManager**: Manages runtime prompt Rationales and lifecycle bindings.

---

## 🛠️ Setup & Configuration

To compile and build JARVISH, you must configure the API target details.

### 1. Build Environment Injection
Create a `.env` file in the root project folder (same folder as `settings.gradle.kts`):

```env
GEMINI_API_KEY=your_gemini_api_studio_key_here
OPENROUTER_API_KEY=your_openrouter_key_here
LLM_BASE_URL=https://generativelanguage.googleapis.com/v1beta
LLM_MODEL=gemini-2.5-flash
```

| Key | Where to get it |
|---|---|
| `GEMINI_API_KEY` | [aistudio.google.com/apikey](https://aistudio.google.com/apikey) — Free |
| `OPENROUTER_API_KEY` | [openrouter.ai/settings/keys](https://openrouter.ai/settings/keys) — Free |

### 2. Android Build System
Ensure you have the Android SDK (Compile API 35) and JDK 17 installed. Compile the debug build target using Gradle:

```bash
./gradlew assembleDebug
```

---

## 📱 Hardware Testing Guidelines

> ⚠️ **DO NOT USE THE ANDROID EMULATOR.** Android emulators lack proper microphone recording mapping and Google Speech Recognizer integrations. Running the voice walkie-talkie loop on an emulator will result in silent failures.

1.  Connect a **Physical Android Device** (API 26+) via USB.
2.  Enable **USB Debugging** in the device's Developer Options.
3.  Compile and run the project from Android Studio.
4.  Grant the application **Microphone** and **Notification Listener** permissions.
5.  Tap the glowing microphone on the Home Screen dashboard and talk to JARVISH!

---

## 🎯 Master Plan & Implementation Status

| Phase | Scope | Status |
|---|---|---|
| Phase 1 | Core Base (Voice Loop, Gemini LLM, UI) | ✅ Completed |
| Phase 2 | Control Layer & Deep-Linking | ✅ Completed |
| Phase 3 | Dual-LLM Gateway (OpenRouter Integration) | ✅ Completed |
| Phase 4 | Autonomous Agent Engine (TaskPlanner, WorkManager) | ✅ Completed |
| Phase 5 | Cross-Device Desktop Bridge (WebSocket) | ✅ Completed |
| Phase 6 | Real-Time Screen Awareness (MediaProjection) | ✅ Completed |
| Phase 7 | Local RAG, OCR & Document Search | ✅ Completed |
| Phase 8 | Security, Vault & Accessibility Automation | ✅ Completed |
| Phase 9 | Memory Sync & Cross-Device Persistence | 🔄 In Progress |
| Phase 10 | Production Polish & Performance Optimization | 🔄 In Progress |

For the detailed roadmap, refer to `plan.md` and `skills.md`.
