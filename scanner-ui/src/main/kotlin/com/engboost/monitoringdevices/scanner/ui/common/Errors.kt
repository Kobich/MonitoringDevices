package com.engboost.monitoringdevices.scanner.ui.common

/** Returns the error message, or the class name if the message is blank. */
internal fun Throwable.toDisplayMessage(): String {
    return message?.takeIf { it.isNotBlank() } ?: this::class.java.simpleName
}

/** Returns [message] followed by the description of [cause], if any. */
internal fun errorText(message: String, cause: Throwable?): String {
    return cause?.let { "$message: ${it.toDisplayMessage()}" } ?: message
}
