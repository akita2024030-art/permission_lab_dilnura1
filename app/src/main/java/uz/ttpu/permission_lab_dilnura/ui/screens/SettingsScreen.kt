package uz.ttpu.permission_lab_dilnura.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.launch
import uz.ttpu.permission_lab_dilnura.data.PermissionType

private const val MAX_TRIES = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToFeature: (PermissionType) -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Permission states and remaining tries counter
    var isCameraGranted by remember { mutableStateOf(checkCameraPermission(context)) }
    var cameraTriesLeft by remember { mutableIntStateOf(MAX_TRIES) }

    var isLocationGranted by remember { mutableStateOf(checkLocationPermission(context)) }
    var locationTriesLeft by remember { mutableIntStateOf(MAX_TRIES) }

    var isMediaGranted by remember { mutableStateOf(checkMediaPermission(context)) }
    var mediaTriesLeft by remember { mutableIntStateOf(MAX_TRIES) }

    // Re-check permission states automatically when returning from Settings (ON_RESUME)
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

    // Permission launchers
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

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResult ->
        val granted = permissionsResult.values.any { it }
        isLocationGranted = granted
        if (granted) {
            locationTriesLeft = MAX_TRIES
            onNavigateToFeature(PermissionType.GEOLOCATION)
        } else {
            locationTriesLeft = (locationTriesLeft - 1).coerceAtLeast(0)
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Geolocation permission is denied, you've $locationTriesLeft tries left."
                )
            }
        }
    }

    val mediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResult ->
        val granted = permissionsResult.values.any { it }
        isMediaGranted = granted
        if (granted) {
            mediaTriesLeft = MAX_TRIES
            onNavigateToFeature(PermissionType.PHOTOS_VIDEOS)
        } else {
            mediaTriesLeft = (mediaTriesLeft - 1).coerceAtLeast(0)
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Photos/Videos permission is denied, you've $mediaTriesLeft tries left."
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permission Settings") },
                actions = {
                    IconButton(onClick = { openAppSettings(context) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "App Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Manage App Permissions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = "Select an item below to request or test permissions. If denied, remaining tries will decrease before directing to Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Permission Items List
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Camera Item
                PermissionCardItem(
                    title = "Camera",
                    description = "Capture photos and test camera feature",
                    icon = Icons.Default.CameraAlt,
                    isGranted = isCameraGranted,
                    triesLeft = cameraTriesLeft,
                    onClick = {
                        if (isCameraGranted) {
                            onNavigateToFeature(PermissionType.CAMERA)
                        } else if (cameraTriesLeft > 0) {
                            cameraLauncher.launch(Manifest.permission.CAMERA)
                        } else {
                            openAppSettings(context)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Tries exhausted for Camera. Redirecting to Settings."
                                )
                            }
                        }
                    }
                )

                // 2. Geolocation Item
                PermissionCardItem(
                    title = "Geolocation",
                    description = "Access device GPS coordinates (lat & long)",
                    icon = Icons.Default.LocationOn,
                    isGranted = isLocationGranted,
                    triesLeft = locationTriesLeft,
                    onClick = {
                        if (isLocationGranted) {
                            onNavigateToFeature(PermissionType.GEOLOCATION)
                        } else if (locationTriesLeft > 0) {
                            locationLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        } else {
                            openAppSettings(context)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Tries exhausted for Geolocation. Redirecting to Settings."
                                )
                            }
                        }
                    }
                )

                // 3. Photos / Videos Item
                PermissionCardItem(
                    title = "Photos & Videos",
                    description = "Access photo and video gallery files (API 34+)",
                    icon = Icons.Default.PermMedia,
                    isGranted = isMediaGranted,
                    triesLeft = mediaTriesLeft,
                    onClick = {
                        if (isMediaGranted) {
                            onNavigateToFeature(PermissionType.PHOTOS_VIDEOS)
                        } else if (mediaTriesLeft > 0) {
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
                            mediaLauncher.launch(permissionsToRequest)
                        } else {
                            openAppSettings(context)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Tries exhausted for Media. Redirecting to Settings."
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PermissionCardItem(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    triesLeft: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Status Chip
                val chipText = when {
                    isGranted -> "Granted"
                    triesLeft > 0 -> "Denied ($triesLeft ${if (triesLeft == 1) "try" else "tries"} left)"
                    else -> "Exhausted -> Open Settings"
                }

                val chipBgColor = when {
                    isGranted -> Color(0xFF2E7D32)
                    triesLeft > 0 -> Color(0xFFE65100)
                    else -> MaterialTheme.colorScheme.error
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = chipBgColor.copy(alpha = 0.15f),
                    contentColor = chipBgColor
                ) {
                    Text(
                        text = chipText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Select",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Helper methods for permission checks
private fun checkCameraPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}

private fun checkLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

private fun checkMediaPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val images = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_IMAGES
        ) == PackageManager.PERMISSION_GRANTED
        val videos = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_VIDEO
        ) == PackageManager.PERMISSION_GRANTED
        images || videos
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    )
    context.startActivity(intent)
}
