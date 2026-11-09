package com.engboost.monitoringdevices.scanner.ui.common

/** @property id increases with every request, so the same permissions can be requested again. */
internal data class PermissionRequestUi(
    val id: Int,
    val permissions: List<String>
)

internal fun nextPermissionRequest(
    previous: PermissionRequestUi?,
    permissions: Set<String>
): PermissionRequestUi {
    return PermissionRequestUi(
        id = (previous?.id ?: 0) + 1,
        permissions = permissions.toList()
    )
}

/** An empty [result] means the dialog was dismissed, so it counts as denied. */
internal fun isAllGranted(result: Map<String, Boolean>): Boolean {
    return result.isNotEmpty() && result.values.all { isGranted -> isGranted }
}
