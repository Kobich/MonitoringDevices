package com.engboost.monitoringdevices

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.engboost.monitoringdevices.scanner.NearbyScanner
import com.engboost.monitoringdevices.scanner.ui.navigateToNearbyScan
import com.engboost.monitoringdevices.scanner.ui.nearbyScanScreen
import com.engboost.monitoringdevices.ui.theme.MonitoringDevicesTheme

private const val HOME_ROUTE = "home"

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
            HomeScreen(onOpenScannerClick = { navController.navigateToNearbyScan() })
        }
        nearbyScanScreen(scanner = scanner)
    }
}

/** Stands in for the host app's own screens. */
@Composable
private fun HomeScreen(onOpenScannerClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(onClick = onOpenScannerClick) {
            Text(text = "Open nearby scanner")
        }
    }
}
