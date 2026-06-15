# 🧠 Elite Android Agent Skills Matrix
**Purpose:** Orchestrate AI agents to solve the 6 core Android challenges with 0.00000001% precision.
**Rule #1:** The human defines the constraints. The agent generates the solution. The human validates.

---

## 1. OS-Compliant Architecture (Android Restrictions)
- **Objective:** Build background/automation features that survive Play Store reviews and OS updates.
- **Agent Role:** `Policy Auditor`
- **Prompting Strategy:** 
  > "Review this background task implementation. Refactor it to strictly comply with Android 14+ background execution limits. Use `WorkManager` for deferrable tasks and a Foreground Service with a compliant notification only if immediate execution is mandatory. Flag any API deprecations."
- **Human Validation:** Verify the agent didn’t introduce hidden hacks (e.g., abusive Accessibility usage) and that the Foreground Service notification is user-facing and clear.

## 2. Resilient State Management (Reliability)
- **Objective:** Guarantee task completion despite app kills, network drops, or device reboots.
- **Agent Role:** `Edge-Case Generator & Test Writer`
- **Prompting Strategy:** 
  > "Analyze this automation workflow. Make it idempotent. Then, generate Robolectric/Instrumented tests that simulate: 1) Process death mid-task, 2) Network loss, 3) Device reboot. Ensure state is saved via DataStore/Room before each critical step."
- **Human Validation:** Run the generated tests. Ensure the app gracefully resumes from the last known good state without crashing or duplicating actions.

## 3. Contextual Permission Orchestration (Permission Handling)
- **Objective:** Maximize grant rates while respecting user choice and avoiding app blocks.
- **Agent Role:** `UX Flow Designer`
- **Prompting Strategy:** 
  > "Draft a Jetpack Compose permission request flow for [Specific Permission]. Include: 1) A custom pre-permission rationale dialog, 2) The actual system request, 3) A graceful degradation path with a deep-link to App Settings if 'Don't ask again' is selected. Use Accompanist Permissions or Activity Result APIs."
- **Human Validation:** Test the flow on a physical device. Ensure the app does *not* crash or loop infinitely if the user denies the permission.

## 4. Power-Efficient Resource Management (Battery Usage)
- **Objective:** Achieve task goals with minimal CPU wake time and battery drain.
- **Agent Role:** `Performance Profiler`
- **Prompting Strategy:** 
  > "Audit this code for battery drain. Refactor it to: 1) Remove any raw `WakeLocks`, 2) Batch network requests, 3) Apply `WorkManager` constraints (`requiresCharging`, `unmetered`), 4) Downgrade continuous GPS to `FusedLocationProvider` with `PRIORITY_BALANCED_POWER_ACCURACY` or Geofencing."
- **Human Validation:** Check Android Studio Profiler (Energy tab). Ensure the app respects Doze mode and doesn’t hold the device awake unnecessarily.

## 5. Scoped & Secure Automation (Safe Automation)
- **Objective:** Automate tasks without compromising user privacy or triggering security flags.
- **Agent Role:** `Security Scanner`
- **Prompting Strategy:** 
  > "Audit this `AccessibilityService` or automation script. Enforce these rules: 1) Strictly scope `packageNames` to ONLY the target app. 2) Add explicit logic to IGNORE and bypass all password, OTP, and financial input fields. 3) Implement a global 'Emergency Stop' kill switch in the code."
- **Human Validation:** Manually review the `accessibility_service_config.xml`. Verify the kill switch is wired to a persistent notification and UI button.

## 6. Non-Blocking, Reactive UI (Good UX)
- **Objective:** Maintain 60/120 FPS UI while heavy automation runs in the background.
- **Agent Role:** `Main-Thread Guardian`
- **Prompting Strategy:** 
  > "Refactor this UI code. Ensure all heavy lifting, I/O, and automation triggers are delegated to Kotlin Coroutines (`viewModelScope` or `lifecycleScope`) and StateFlow. Add subtle haptic feedback on success/failure and a non-intrusive UI indicator (e.g., a minimal bottom sheet or status badge)."
- **Human Validation:** Run the app with "Show CPU usage" and "Profile GPU rendering" enabled in Developer Options. Ensure zero dropped frames during automation triggers.

---

## 🤖 Agent Orchestration Workflow (The Pro Pipeline)
Do not ask one agent to do everything. Chain them:

1. **Architect Agent:** "Design the high-level flow for this feature respecting the 6 core challenges."
2. **Coder Agent:** "Implement step 1 of the design using Kotlin, Coroutines, and WorkManager."
3. **Auditor Agent:** "Review the Coder Agent's output against the `skills.md` constraints. Flag violations."
4. **Test Agent:** "Write unit and instrumentation tests for the approved code, focusing on edge cases."

> **💡 Pro Mantra:** AI writes the boilerplate; *you* enforce the architecture, security, and UX boundaries.