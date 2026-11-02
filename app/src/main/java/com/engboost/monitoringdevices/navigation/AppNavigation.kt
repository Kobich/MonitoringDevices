package com.engboost.monitoringdevices.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.engboost.monitoringdevices.feature.filemanager.api.FileManagerFeatureApi
import com.engboost.monitoringdevices.home.HomeScreen

@Composable
internal fun AppNavigation(
    fileManagerFeature: FileManagerFeatureApi,
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
                onFileManagerClick = { navController.navigate(AppDestination.FILE_MANAGER) }
            )
        }
        composable(AppDestination.FILE_MANAGER) {
            fileManagerFeature.FileManagerScreen(
                modifier = Modifier,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

private object AppDestination {
    const val HOME = "home"
    const val FILE_MANAGER = "file_manager"
}
