package uz.ttpu.permission_lab_dilnura

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import uz.ttpu.permission_lab_dilnura.data.PermissionType
import uz.ttpu.permission_lab_dilnura.ui.screens.CameraScreen
import uz.ttpu.permission_lab_dilnura.ui.screens.LocationScreen
import uz.ttpu.permission_lab_dilnura.ui.screens.MediaScreen
import uz.ttpu.permission_lab_dilnura.ui.screens.SettingsScreen
import uz.ttpu.permission_lab_dilnura.ui.theme.Permission_lab_dilnuraTheme

sealed class Screen {
    object Settings : Screen()
    object Camera : Screen()
    object Location : Screen()
    object Media : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Permission_lab_dilnuraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Settings) }

                    Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                        when (screen) {
                            Screen.Settings -> {
                                SettingsScreen(
                                    onNavigateToFeature = { type ->
                                        currentScreen = when (type) {
                                            PermissionType.CAMERA -> Screen.Camera
                                            PermissionType.GEOLOCATION -> Screen.Location
                                            PermissionType.PHOTOS_VIDEOS -> Screen.Media
                                        }
                                    }
                                )
                            }
                            Screen.Camera -> {
                                CameraScreen(
                                    onBackClick = { currentScreen = Screen.Settings }
                                )
                            }
                            Screen.Location -> {
                                LocationScreen(
                                    onBackClick = { currentScreen = Screen.Settings }
                                )
                            }
                            Screen.Media -> {
                                MediaScreen(
                                    onBackClick = { currentScreen = Screen.Settings }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
