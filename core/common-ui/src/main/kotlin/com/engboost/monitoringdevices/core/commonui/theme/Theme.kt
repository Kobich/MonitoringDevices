package com.engboost.monitoringdevices.core.commonui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Colors come from the device: Material You palette of the wallpaper / system settings on Android 12+,
 * the Material 3 baseline palette on older versions. Light or dark follows the system setting.
 *
 * Components take colors only from `MaterialTheme.colorScheme`, never hardcoded, so they follow the device too.
 */
@Composable
fun MonitoringDevicesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    /** `false` gives the baseline palette everywhere: stable colors for previews. */
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
