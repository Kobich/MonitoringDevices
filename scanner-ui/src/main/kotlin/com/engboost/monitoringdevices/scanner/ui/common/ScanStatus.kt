package com.engboost.monitoringdevices.scanner.ui.common

/**
 * Status of a scan shown on a screen.
 *
 * [R] is the reason type of the scanner's `Unavailable` event, e.g. `WifiUnavailableReason`.
 */
internal sealed interface ScanStatus<out R> {
    data object Stopped : ScanStatus<Nothing>

    /** Scanning has been started by the user and runs while the screen is visible. */
    data object Scanning : ScanStatus<Nothing>

    data object PermissionsRequired : ScanStatus<Nothing>

    data object PermissionsDenied : ScanStatus<Nothing>

    data class Unavailable<R>(val reason: R) : ScanStatus<R>

    data class Failed(val message: String) : ScanStatus<Nothing>
}

/** Returns the text of the status; [reasonText] describes the reason of [ScanStatus.Unavailable]. */
internal fun <R> ScanStatus<R>.toText(reasonText: (R) -> String): String {
    return when (this) {
        ScanStatus.Stopped -> "Stopped"
        ScanStatus.Scanning -> "Scanning"
        ScanStatus.PermissionsRequired -> "Permissions required"
        ScanStatus.PermissionsDenied -> "Permissions denied"
        is ScanStatus.Unavailable -> reasonText(reason)
        is ScanStatus.Failed -> message
    }
}
