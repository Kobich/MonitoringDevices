package com.android.core.logger.client.api

data class UiInteractionEvent(
    val action: UiInteractionAction,
    val screen: String,
    val elementId: String,
    val elementType: String,
    val text: String? = null
)
