package com.engboost.monitoringdevices

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.engboost.monitoringdevices.featuretest.FeatureTestScreen
import com.engboost.monitoringdevices.scanner.NearbyScanner
import com.engboost.monitoringdevices.scanner.ui.navigateToNearbyScan
import com.engboost.monitoringdevices.scanner.ui.nearbyScanScreen
import com.engboost.monitoringdevices.ui.theme.MonitoringDevicesTheme

private const val HOME_ROUTE = "home"
private const val FEATURE_TEST_ROUTE = "feature_test"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scanner = (application as MonitoringDevicesApplication).scanner
        enableEdgeToEdge()
        setContent {
            MonitoringDevicesTheme {
                AppNavHost(scanner = scanner)
            }
        }
    }
}

@Composable
private fun AppNavHost(scanner: NearbyScanner) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = HOME_ROUTE) {
        composable(HOME_ROUTE) {
            HomeScreen(
                onOpenScannerClick = { navController.navigateToNearbyScan() },
                onOpenFeatureTestClick = { navController.navigate(FEATURE_TEST_ROUTE) }
            )
        }
        nearbyScanScreen(scanner = scanner)
        composable(FEATURE_TEST_ROUTE) {
            FeatureTestScreen()
        }
    }
}

/** Stands in for the host app's own screens. */
@Composable
private fun HomeScreen(onOpenScannerClick: () -> Unit, onOpenFeatureTestClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onOpenScannerClick) {
            Text(text = "Open nearby scanner")
        }
        Button(onClick = onOpenFeatureTestClick) {
            Text(text = "Test features")
        }
    }
}
