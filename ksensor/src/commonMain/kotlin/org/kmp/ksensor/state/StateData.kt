package org.kmp.ksensor.state

enum class PlatformType {
    iOS,
    Android
}

enum class StateType {
    SCREEN,
    APP_VISIBILITY,
    CONNECTIVITY,
    ACTIVE_NETWORK,
    LOCATION,
    VOLUME,
    LOCALE
}


sealed class StateData {
    data class AppVisibilityStatus(
        val isAppVisible: Boolean,
    ) : StateData()

    data class LocationStatus(
        val isLocationOn: Boolean
    ) : StateData()

    data class ScreenStatus(
        val isScreenOn: Boolean,
    ) : StateData()

    data class CurrentActiveNetwork(val activeNetwork: ActiveNetwork) : StateData() {
        enum class ActiveNetwork {
            WIFI,
            CELLULAR,
            NONE
        }
    }

    data class VolumeStatus(val volumePercentage: Int): StateData()
    data class ConnectivityStatus(
        val isConnected: Boolean,
    ) : StateData()

    data class LocaleInfo(
        val languageCode: String,      // ISO 639 (e.g., "en", "ar", "he")
        val countryCode: String,       // ISO 3166 (e.g., "US", "SA", "IL")
        val fullLocaleString: String,  // Complete identifier (e.g., "en_US", "ar_SA")
        val displayName: String,       // Human-readable (e.g., "English (United States)")
        val isRTL: Boolean             // Right-to-left layout (true for Arabic, Hebrew, etc.)
    ) : StateData()
}