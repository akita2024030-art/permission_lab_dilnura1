# Permissions Practice Lab - Android

**Author:** Dilnura (TTPU Student)  
**Course:** Mobile Application Development  
**Topic:** Android Permissions Management (Install-Time, Runtime, and Special Permissions)

---

## 📱 Project Overview

This Android application demonstrates proper permission handling in Android 14+ (API 34+) using **Jetpack Compose** and modern AndroidX Activity Result APIs. The app features a central **Settings Screen** where users can manage runtime permissions for **Camera**, **Geolocation**, and **Photos/Videos**.

### Core Requirements Implemented:
1. **Single Settings Screen**: Displays interactive items for Camera, Geolocation, and Photos/Videos.
2. **Retry Mechanism ($N = 2$ attempts)**:
   - When a permission item is selected, the system permission dialog is requested.
   - If denied, a `Snackbar` is shown informing the user: `"Permission denied, you've N tries left."`
   - When $N$ tries are exhausted, clicking the item automatically redirects the user to the system **App Settings** screen.
3. **Lifecycle Re-checking (`ON_RESUME`)**: When returning from system settings, the app automatically re-evaluates `checkSelfPermission` and updates the UI state.
4. **Optional Feature Screens**:
   - **Camera Screen**: Viewfinder interface with photo capture simulator.
   - **Geolocation Screen**: Displays live GPS coordinates (Latitude & Longitude).
   - **Photos & Videos Screen**: Interactive media box with image/video selection launchers.

---

## 🎥 Video Demonstrations

<details>
<summary><b>1. Camera Permission Flow (Click to expand)</b></summary>

<br>

> **Video Description (Max 40 seconds):**
> 1. Select Camera item -> Deny permission on first attempt (Snackbar shows "1 try left").
> 2. Click Camera item again -> Deny permission on second attempt (Snackbar shows "0 tries left").
> 3. Click Camera item a third time -> Automatically opens System Settings.
> 4. Grant Camera permission in Settings.
> 5. Return to app (ON_RESUME detects permission granted) -> Opens Camera Feature Screen.

https://github.com/user-attachments/assets/CAMERA_DEMO_VIDEO.mp4

</details>

<details>
<summary><b>2. Geolocation Permission Flow (Click to expand)</b></summary>

<br>

> **Video Description (Max 40 seconds):**
> 1. Select Geolocation item -> Deny location request (Snackbar shows remaining tries).
> 2. Repeat until tries are exhausted -> App redirects to System Settings.
> 3. Enable Location in System Settings.
> 4. Return to app -> Opens Geolocation screen displaying Latitude and Longitude coordinates.

https://github.com/user-attachments/assets/LOCATION_DEMO_VIDEO.mp4

</details>

<details>
<summary><b>3. Photos & Videos Permission Flow (Click to expand)</b></summary>

<br>

> **Video Description (Max 40 seconds):**
> 1. Select Photos & Videos item -> Deny media permission request.
> 2. Exhaust attempts -> Redirect to System Settings to grant Media access (API 34+).
> 3. Grant Photos & Videos permission in Settings.
> 4. Return to app -> Opens Media Gallery Box screen with photo and video picker.

https://github.com/user-attachments/assets/MEDIA_DEMO_VIDEO.mp4

</details>

---

## 💻 Code Explanations & Architecture

### 1. Permission State & Attempts Management

In `SettingsScreen.kt`, we maintain state variables for tracking permission status and remaining attempts:

```kotlin
private const val MAX_TRIES = 2

// Tracking granted status and remaining attempts
var isCameraGranted by remember { mutableStateOf(checkCameraPermission(context)) }
var cameraTriesLeft by remember { mutableIntStateOf(MAX_TRIES) }
```

When an item is clicked, if the permission is not yet granted and tries remain (`cameraTriesLeft > 0`), we request it via `rememberLauncherForActivityResult`. If denied, `cameraTriesLeft` decreases and a Snackbar is displayed:

```kotlin
val cameraLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
) { granted ->
    isCameraGranted = granted
    if (granted) {
        cameraTriesLeft = MAX_TRIES
        onNavigateToFeature(PermissionType.CAMERA)
    } else {
        cameraTriesLeft = (cameraTriesLeft - 1).coerceAtLeast(0)
        scope.launch {
            snackbarHostState.showSnackbar(
                message = "Camera permission denied, you've $cameraTriesLeft tries left."
            )
        }
    }
}
```

---

### 2. Exhausted Tries & Navigating to System Settings

Once the user exhausts all $N$ attempts (`triesLeft <= 0`), any subsequent click on the permission card triggers navigation to the app's system details settings page:

```kotlin
onClick = {
    if (isCameraGranted) {
        onNavigateToFeature(PermissionType.CAMERA)
    } else if (cameraTriesLeft > 0) {
        cameraLauncher.launch(Manifest.permission.CAMERA)
    } else {
        // Tries exhausted -> Redirect to Settings
        openAppSettings(context)
        scope.launch {
            snackbarHostState.showSnackbar(
                message = "Tries exhausted for Camera. Redirecting to Settings."
            )
        }
    }
}
```

The `openAppSettings` helper constructs an `Intent` pointing to `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`:

```kotlin
private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    )
    context.startActivity(intent)
}
```

---

### 3. Automatic Re-checking on App Resume (`ON_RESUME`)

When a user enables a permission in system Settings and returns to the application, the app needs to update its state immediately. We achieve this using Compose's `LifecycleEventEffect`:

```kotlin
LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
    val updatedCamera = checkCameraPermission(context)
    val updatedLocation = checkLocationPermission(context)
    val updatedMedia = checkMediaPermission(context)

    if (updatedCamera != isCameraGranted) {
        isCameraGranted = updatedCamera
        if (updatedCamera) cameraTriesLeft = MAX_TRIES
    }
    if (updatedLocation != isLocationGranted) {
        isLocationGranted = updatedLocation
        if (updatedLocation) locationTriesLeft = MAX_TRIES
    }
    if (updatedMedia != isMediaGranted) {
        isMediaGranted = updatedMedia
        if (updatedMedia) mediaTriesLeft = MAX_TRIES
    }
}
```

---

### 4. Media Permission Compatibility (API 34+)

For Android 14+ (API 34+ / API 33+), granular media permissions (`READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_VISUAL_USER_SELECTED`) are declared in `AndroidManifest.xml` and requested dynamically:

```kotlin
val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
    arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
    )
} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO
    )
} else {
    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}
```

---

## 🛠 Tech Stack
- **Language**: Kotlin 2.2
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: Single Activity, State-driven Composable Navigation
- **Target SDK**: API 34+ (Android 14)
