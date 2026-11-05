package uz.ttpu.permission_lab_dilnura.data

enum class PermissionType {
    CAMERA,
    GEOLOCATION,
    PHOTOS_VIDEOS
}

data class PermissionState(
    val type: PermissionType,
    val title: String,
    val description: String,
    val isGranted: Boolean = false,
    val triesLeft: Int = 2
)
