package com.android.appmanager.feature.documentviewer.presentation

import com.android.core.logger.client.api.ActionLogger
import com.android.core.logger.client.api.UiInteractionAction
import com.android.core.logger.client.api.UiInteractionEvent

private const val SCREEN_NAME = "document_viewer"

/** A click for the action logger. [text] is the element's label, never the user's file names. */
internal fun logClick(elementId: String, elementType: String, text: String) {
    ActionLogger.logUi(
        event = UiInteractionEvent(
            action = UiInteractionAction.Click,
            screen = SCREEN_NAME,
            elementId = elementId,
            elementType = elementType,
            text = text
        )
    )
}
