package com.android.core.commonui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colors with a meaning that the Material scheme doesn't have: "ok", "in progress", "problem".
 * Soft tonal pairs, so a status reads at a glance without shouting:
 * [accent] for an icon on the screen background, [container] + [onContainer] for a pill or a card.
 */
@Immutable
class StatusColorRole(
    val accent: Color,
    val container: Color,
    val onContainer: Color
)

@Immutable
class StatusColors(
    val positive: StatusColorRole,
    val pending: StatusColorRole,
    val negative: StatusColorRole
)

internal val LightStatusColors = StatusColors(
    positive = StatusColorRole(accent = Color(0xFF2E7D32), container = Color(0xFFDCEFD8), onContainer = Color(0xFF123D17)),
    pending = StatusColorRole(accent = Color(0xFF9A6700), container = Color(0xFFFAEED3), onContainer = Color(0xFF3F2E00)),
    negative = StatusColorRole(accent = Color(0xFFB3423A), container = Color(0xFFF9E1DE), onContainer = Color(0xFF4F1A16))
)

internal val DarkStatusColors = StatusColors(
    positive = StatusColorRole(accent = Color(0xFF8FD18B), container = Color(0xFF1E3620), onContainer = Color(0xFFCBEBC6)),
    pending = StatusColorRole(accent = Color(0xFFE3BD62), container = Color(0xFF3A2F14), onContainer = Color(0xFFF3E0B0)),
    negative = StatusColorRole(accent = Color(0xFFF0A098), container = Color(0xFF43221F), onContainer = Color(0xFFF7D3CE))
)

internal val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }
