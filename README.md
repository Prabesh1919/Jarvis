# JARVISH: Elite Android AI Assistant

JARVISH is a next-generation, voice-activated AI assistant for Android, designed with a **Top 0.001% Cyber-Glass UI** and powered by Gemini 2.5 Flash.

This project aims to build an "OpenClaw-like" Android voice assistant capable of intelligent reasoning, system-wide automation, and high-fidelity speech interaction.

---

## 🌟 Key Features

*   **Elite "Cyber-Glass" UI:**
    *   Floating translucent navigation docks and top bars.
    *   Intricate HUD dials with micro-animations and glowing sweeps.
    *   Frosted glass system cards with real-time feedback.
    *   5 distinct futuristic color themes (Red Matrix, Blue Ocean, Green Tech, Purple Void, Orange Heat).
*   **Native Voice Loop:**
    *   Utilizes native Android `SpeechRecognizer` for low-latency Speech-to-Text.
    *   Utilizes native Android `TextToSpeech` for immediate auditory feedback.
*   **Gemini AI Brain (`LlmClient`):**
    *   Lightweight native client directly interfacing with the Gemini API (no heavy SDKs).
    *   Maintains conversation context and processes voice prompts rapidly.
*   **Secure API Key Injection:**
    *   Supports `.env` file configuration at build time.
    *   Secure runtime vault fallback via Android `EncryptedSharedPreferences`.

---

## 🛠️ Setup & Configuration

To run JARVISH, you must configure your Gemini API Key. The app supports a seamless build-time injection.

### 1. Configure the `.env` File
Create a `.env` file in the root of the project (same directory as this README):

```env
GEMINI_API_KEY=your_gemini_api_key_here
LLM_BASE_URL=https://generativelanguage.googleapis.com/v1beta
LLM_MODEL=gemini-2.5-flash
```

*(Note: The build system will automatically inject these values into `BuildConfig` during compilation, allowing the app to use them instantly without manual in-app entry.)*

### 2. Build the Project
Open the project in Android Studio or run Gradle from the command line:
```bash
./gradlew assembleDebug
```

---

## 📱 How to Test the Voice Module

> [!WARNING]
> **DO NOT USE AN EMULATOR.** The Android Emulator notoriously lacks proper microphone mapping and the required Google Speech engines. **The Voice Loop will fail silently on an emulator.**

To test the STT -> LLM -> TTS loop successfully:

1.  **Connect a Physical Android Device** to your Mac via USB.
2.  Enable **USB Debugging** in Developer Options on the device.
3.  In Android Studio, select your physical device and click **Run**.
4.  When the app opens, it will prompt you for Microphone permissions. Select **"While using the app"**.
5.  Tap the pulsing microphone icon on the Home Screen and speak naturally. JARVISH will transcribe your speech, query Gemini, and speak the response back to you.

---

## 🗺️ Master Plan & Roadmap

This project is being built in phases. Currently, we are in **Phase 2: Android Control Layer**.

### Phase 1: Core Assistant (Completed ✅)
*   [x] Speech-to-text integration
*   [x] Text-to-speech feedback
*   [x] AI Brain (Gemini Flash)
*   [x] Premium UI Shell

### Phase 2: Android Control Layer (In Progress ⏳)
*   [ ] App Launcher & Deep Linking
*   [ ] System Actions (Call, SMS, Alarms, Flashlight)
*   [ ] Notification Reading

### Future Phases
*   **Phase 3:** Accessibility Automation (Screen parsing, tapping).
*   **Phase 4:** Intelligence Layer (Context engine, Tool routing).
*   **Phase 5:** Vision (OCR, Screen reading via MediaProjection).
*   **Phase 6:** Autonomous RAG (Document reading, long-term memory).
