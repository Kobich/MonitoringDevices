package com.android.appmanager.feature.filemanager.presentation.model

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/**
 * A text of the screen state, kept as resources rather than a ready string: the screen resolves it, so it follows the
 * app language even in a ViewModel that outlived a configuration change.
 */
@Immutable
internal sealed interface UiText {
    /** Not translated: a file name, an error reason. */
    data class Plain(val value: String) : UiText

    data class Resource(@StringRes val id: Int, val args: List<UiText>) : UiText {
        constructor(@StringRes id: Int, vararg args: UiText) : this(id, args.toList())
    }

    /** [count] picks the form and is its `%1$d`. */
    data class Plural(@PluralsRes val id: Int, val count: Int) : UiText

    data class Joined(val parts: List<UiText>, val separator: String) : UiText
}

internal fun UiText.resolve(resources: Resources): String = when (this) {
    is UiText.Plain -> value
    is UiText.Resource -> if (args.isEmpty()) {
        resources.getString(id)
    } else {
        resources.getString(id, *args.map { arg -> arg.resolve(resources) }.toTypedArray())
    }
    is UiText.Plural -> resources.getQuantityString(id, count, count)
    is UiText.Joined -> parts.joinToString(separator) { part -> part.resolve(resources) }
}

@Composable
@ReadOnlyComposable
internal fun UiText.asString(): String {
    // Read so that a new configuration (language) resolves the text again.
    LocalConfiguration.current
    return resolve(LocalContext.current.resources)
}
