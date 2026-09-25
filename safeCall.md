# SafeCall: Development Workflow & Logic

This document outlines the step-by-step process used to build the Fake Caller application. It explains the "why" behind the file creation order, the behavior of specific code snippets, and how errors were handled.

---

## 1. Project Exploration & Manifest Foundation
**Files Modified:** `AndroidManifest.xml`  
**Reasoning:** Every Android component must be registered. I started here to define the "contract" of the app: what permissions it needs and what activities/receivers exist.

- **Actions:** 
    - Added `VIBRATE` and `SCHEDULE_EXACT_ALARM` permissions.
    - Registered `FakeCallActivity` and `CallReceiver`.
- **Behavior:** `showOnLockScreen="true"` and `turnScreenOn="true"` were added to ensure the fake call can interrupt the user even if the phone is locked.
- **Errors Encountered:** The IDE immediately flagged `.FakeCallActivity` and `.CallReceiver` as "not found."
- **Why I deferred the fix:** I intentionally ignored these errors because I hadn't created the Kotlin files yet. In Android Studio, it is common to define the Manifest first and then "fill in" the missing classes.

---

## 2. The Background Trigger (CallReceiver)
**Files Created:** `CallReceiver.kt`  
**Reasoning:** We need a way to trigger the call even if the app is closed. A `BroadcastReceiver` is the standard tool for this.

- **Logic:** It receives an "Alarm" from the system, extracts the `CALLER_NAME`, and starts `FakeCallActivity` using the `FLAG_ACTIVITY_NEW_TASK` flag (required when starting an activity from outside a UI context).
- **Errors Encountered:** Unresolved reference to `FakeCallActivity`.
- **Handling:** Again, deferred until the Activity class was actually written.

---

## 3. The Incoming Call UI (FakeCallActivity)
**Files Created:** `FakeCallActivity.kt`  
**Reasoning:** This is the core "illusion" of the app. It needs to look like a real phone call.

- **Behaviors:**
    - **Ringtone:** Uses `RingtoneManager` to play the user's default ringtone.
    - **Vibration:** Uses `VibratorManager` (API 31+) or `Vibrator` (Legacy) to create a pulse pattern.
    - **Compose UI:** A full-screen dark UI with "Accept" and "Decline" buttons.
- **Errors Identified:** 
    1. **Typo:** I accidentally typed `CircleAcces` instead of `CircleShape`. This was identified by the IDE's real-time analysis.
    2. **Missing Icons:** `Icons.Default.Call` and `CallEnd` are part of the "Extended" Material icons set, which isn't included in the default project template.
- **Why fix later?** I had to update the project-wide `build.gradle` first to include the missing library before the Kotlin file could recognize the imports.

---

## 4. Dependency Management
**Files Modified:** `libs.versions.toml`, `build.gradle.kts`  
**Reasoning:** To fix the UI errors in the previous step, I needed to add the `androidx.compose.material:material-icons-extended` dependency.

- **Process:**
    1. Added the version and library alias to `libs.versions.toml`.
    2. Added `implementation(libs.androidx.compose.material.icons.extended)` to the app-level Gradle file.
- **The "Fix":** Once I ran a **Gradle Sync**, the errors in `FakeCallActivity.kt` regarding icons automatically disappeared.

---

## 5. The Setup Screen (MainActivity)
**Files Modified:** `MainActivity.kt`  
**Reasoning:** Finally, we need a way for the user to interact with the system.

- **Logic:** 
    - Uses `OutlinedTextField` for user input.
    - **AlarmManager:** The `scheduleFakeCall` function calculates a "trigger time" using `SystemClock.elapsedRealtime()`.
    - It uses `setExactAndAllowWhileIdle` to ensure the call comes through even if the phone is in power-saving (Doze) mode.
- **Security Check:** A `try-catch` block was added for `SecurityException` because Android 13+ requires specific user permission to schedule exact alarms.

---

## Summary of the Workflow Strategy

1. **Top-Down Definition:** Start with the Manifest to define the app's structure.
2. **Handle Dependencies only when needed:** I didn't add all libraries at the start to keep the project lean. I only added `material-icons-extended` when the UI design required it.
3. **Delayed Resolution:** Error messages are used as a "To-Do list." An unresolved reference to a class simply tells me which file I need to create next.
4. **Hardware Integration:** Ringtone and Vibration were handled early in `onCreate` to ensure the "illusion" starts immediately as the screen appears.
