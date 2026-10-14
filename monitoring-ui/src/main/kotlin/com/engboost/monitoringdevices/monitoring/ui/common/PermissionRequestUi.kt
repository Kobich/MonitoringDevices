package com.engboost.monitoringdevices.monitoring.ui.common

/**
 * Runtime permissions to request from the user.
 *
 * @property id increases with every new request, so equal permission sets are requested again.
 */
internal data class PermissionRequestUi(
    val id: Int,
    val permissions: List<String>
)

/** Returns a request for [permissions] that follows [previous]. */
internal fun nextPermissionRequest(
    previous: PermissionRequestUi?,
    permissions: Set<String>
): PermissionRequestUi {
    return PermissionRequestUi(
        id = (previous?.id ?: 0) + 1,
        permissions = permissions.toList()
    )
}

/** Returns `true` if [result] is not empty and every permission in it is granted. */
internal fun isAllGranted(result: Map<String, Boolean>): Boolean {
    return result.isNotEmpty() && result.values.all { isGranted -> isGranted }
}
