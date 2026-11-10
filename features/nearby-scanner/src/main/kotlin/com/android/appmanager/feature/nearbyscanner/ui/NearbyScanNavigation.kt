package com.android.appmanager.feature.nearbyscanner.ui

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.android.appmanager.feature.nearbyscanner.NearbyScanner

const val NEARBY_SCAN_ROUTE = "nearby_scan"

fun NavGraphBuilder.nearbyScanScreen(scanner: NearbyScanner) {
    composable(NEARBY_SCAN_ROUTE) {
        NearbyScanScreen(scanner = scanner)
    }
}

fun NavController.navigateToNearbyScan(navOptions: NavOptions? = null) {
    navigate(NEARBY_SCAN_ROUTE, navOptions)
}
