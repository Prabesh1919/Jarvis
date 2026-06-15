# 📏 Elite Android Agent Benchmark (0.00000001% Standard)

**Purpose:** A strict, measurable grading rubric for AI-generated Android code.
**Rule:** The AI agent MUST self-evaluate its output against this checklist. Any "FAIL" requires an immediate code rewrite before showing it to the user.

---

## 1. Android Restrictions & Compliance

- [ ] **Background Execution:** Does the code use `WorkManager` for deferrable tasks? (PASS/FAIL)
- [ ] **Foreground Service:** If used, does it declare `foregroundServiceType` in the manifest and show a persistent, user-clearable notification? (PASS/FAIL)
- [ ] **No Shady Hacks:** Is the code free of hidden loops, abusive `AlarmManager` usage, or undocumented reflection hacks? (PASS/FAIL)
- [ ] **Android 14+ Ready:** Are all exported components explicitly declared (`android:exported="true/false"`)? (PASS/FAIL)

## 2. Reliability & State Management

- [ ] **Idempotency:** Can this function be safely retried multiple times without duplicating actions or corrupting state? (PASS/FAIL)
- [ ] **Crash Recovery:** Are all critical automation steps wrapped in `try/catch` blocks with graceful fallback logic? (PASS/FAIL)
- [ ] **State Persistence:** Is the current task state saved to `DataStore` or `Room` *before* executing the next step? (PASS/FAIL)
- [ ] **Test Coverage:** Did the agent generate at least one Robolectric or Instrumented test simulating a process death or network drop? (PASS/FAIL)

## 3. Permission Handling

- [ ] **Contextual Request:** Is the permission requested *at the moment of need*, not at app startup? (PASS/FAIL)
- [ ] **Pre-Permission Rationale:** Is there a custom UI dialog explaining *why* the permission is needed before triggering the system prompt? (PASS/FAIL)
- [ ] **Graceful Degradation:** If permanently denied, does the code provide a button that deep-links to `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`? (PASS/FAIL)
- [ ] **No App Blocking:** Does the app remain functional (with degraded features) if a non-critical permission is denied? (PASS/FAIL)

## 4. Battery & Performance Usage

- [ ] **No Raw WakeLocks:** Is the code free of manual `PowerManager.WakeLock` acquisitions? (PASS/FAIL)
- [ ] **WorkManager Constraints:** Are background tasks constrained appropriately (e.g., `requiresCharging = true`, `requiredNetworkType = UNMETERED`)? (PASS/FAIL)
- [ ] **Location Efficiency:** If location is used, does it use `FusedLocationProviderClient` with `PRIORITY_BALANCED_POWER_ACCURACY` or Geofencing, rather than continuous GPS? (PASS/FAIL)
- [ ] **Network Batching:** Are network calls grouped or debounced to prevent radio wake-ups? (PASS/FAIL)

## 5. Safe Automation

- [ ] **Strict Scoping:** If Accessibility is used, is the `packageNames` array in `accessibility_service_config.xml` strictly limited to the target app(s)? (PASS/FAIL)
- [ ] **Privacy Protection:** Does the code explicitly contain logic to IGNORE/BYPASS any UI node containing "password", "otp", "cvv", or "pin" in its className or contentDescription? (PASS/FAIL)
- [ ] **Kill Switch:** Is there a globally accessible `emergencyStop()` function that cancels all active Coroutines/Jobs and stops services? (PASS/FAIL)
- [ ] **User Consent:** Does the automation require explicit user confirmation before executing multi-step or destructive actions? (PASS/ is missing)

## 6. Good UX & Architecture

- [ ] **Main Thread Sacred:** Are all heavy operations, I/O, and automation triggers dispatched to `viewModelScope`, `lifecycleScope`, or `Dispatchers.IO`? (PASS/FAIL)
- [ ] **Reactive UI:** Does the UI observe state via `StateFlow` or `LiveData` without blocking the main thread? (PASS/FAIL)
- [ ] **Feedback:** Does the code include subtle user feedback (e.g., `HapticFeedbackConstants`, Toast, or a minimal status badge) on success/failure? (PASS/FAIL)
- [ ] **Memory Leaks:** Are all listeners, flows, and callbacks properly cleared in `onCleared()` or `onDestroy()`? (PASS/FAIL)

---

## 🤖 Agent Self-Evaluation Prompt (Copy & Paste this at the end of every generation)

> "Before outputting the final code, act as the 'Policy Auditor' and 'Main-Thread Guardian'. Review your generated code against the `benchmark.md` checklist above.
>
> Output a strict table:
> | Category | Check | Status (PASS/FAIL) | Fix Applied (if FAIL) |
>
> If ANY item is FAIL, rewrite the code immediately to fix it. Do not show me failing code."

---

## 📊 Performance Metrics to Track (For Human Validation)

When testing the agent's code, the human developer must verify:

1. **CPU:** < 5% usage when idle in the background.
2. **Battery:** < 2% drain per hour during active automation.
3. **UI:** 0 dropped frames (green bars only) in Android Studio GPU Rendering profiler during automation triggers.
4. **Cold Start:** App launches to interactive state in < 1.5 seconds.
