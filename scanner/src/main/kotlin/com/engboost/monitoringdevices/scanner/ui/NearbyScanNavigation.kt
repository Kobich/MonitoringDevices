package com.engboost.monitoringdevices.scanner.ui

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.engboost.monitoringdevices.scanner.NearbyScanner

const val NEARBY_SCAN_ROUTE = "nearby_scan"

/** Adds the feature to the host navigation graph. */
fun NavGraphBuilder.nearbyScanScreen(scanner: NearbyScanner) {
    composable(NEARBY_SCAN_ROUTE) {
        NearbyScanScreen(scanner = scanner)
    }
}

fun NavController.navigateToNearbyScan(navOptions: NavOptions? = null) {
    navigate(NEARBY_SCAN_ROUTE, navOptions)
}
