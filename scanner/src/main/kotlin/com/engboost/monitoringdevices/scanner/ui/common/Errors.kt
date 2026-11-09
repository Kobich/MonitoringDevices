package com.engboost.monitoringdevices.scanner.ui.common

/** Falls back to the class name when the message is blank. */
internal fun Throwable.toDisplayMessage(): String {
    return message?.takeIf { it.isNotBlank() } ?: this::class.java.simpleName
}

internal fun errorText(message: String, cause: Throwable?): String {
    return cause?.let { "$message: ${it.toDisplayMessage()}" } ?: message
}
