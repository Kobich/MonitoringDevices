package com.engboost.monitoringdevices.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.android.appmanager.feature.documentviewer.api.DocumentViewerFeatureApi
import com.android.appmanager.feature.filemanager.api.FileManagerFeatureApi
import com.android.appmanager.feature.nearbyscanner.NearbyScanner
import com.android.appmanager.feature.nearbyscanner.ui.navigateToNearbyScan
import com.android.appmanager.feature.nearbyscanner.ui.nearbyScanScreen
import com.android.core.commonui.icons.IconCatalogScreen
import com.engboost.monitoringdevices.featuretest.FeatureTestScreen
import com.engboost.monitoringdevices.home.HomeScreen
import java.io.File

@Composable
internal fun AppNavigation(
    fileManagerFeature: FileManagerFeatureApi,
    documentViewerFeature: DocumentViewerFeatureApi,
    scanner: NearbyScanner,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppDestination.HOME,
        modifier = modifier
    ) {
        composable(AppDestination.HOME) {
            HomeScreen(
                onFileManagerClick = { navController.navigate(AppDestination.FILE_MANAGER) },
                onScannerClick = { navController.navigateToNearbyScan() },
                onFeatureTestClick = { navController.navigate(AppDestination.FEATURE_TEST) },
                onIconsClick = { navController.navigate(AppDestination.ICONS) }
            )
        }
        composable(AppDestination.ICONS) {
            IconCatalogScreen(onBackClick = { navController.popBackStack() })
        }
        nearbyScanScreen(scanner = scanner)
        composable(AppDestination.FEATURE_TEST) {
            FeatureTestScreen()
        }
        composable(AppDestination.FILE_MANAGER) {
            fileManagerFeature.FileManagerScreen(
                modifier = Modifier,
                onBackClick = { navController.popBackStack() },
                onFileOpen = { file -> navController.navigate(AppDestination.fileViewer(file)) }
            )
        }
        composable(
            route = AppDestination.FILE_VIEWER,
            arguments = listOf(navArgument(AppDestination.ARG_PATH) { type = NavType.StringType })
        ) { backStackEntry ->
            val path = backStackEntry.arguments?.getString(AppDestination.ARG_PATH).orEmpty()
            documentViewerFeature.DocumentViewerScreen(
                file = File(path),
                modifier = Modifier,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

private object AppDestination {
    const val HOME = "home"
    const val FILE_MANAGER = "file_manager"
    const val FEATURE_TEST = "feature_test"
    const val ICONS = "icons"
    const val ARG_PATH = "path"
    const val FILE_VIEWER = "file_viewer?$ARG_PATH={$ARG_PATH}"

    fun fileViewer(file: File): String = "file_viewer?$ARG_PATH=${Uri.encode(file.absolutePath)}"
}
