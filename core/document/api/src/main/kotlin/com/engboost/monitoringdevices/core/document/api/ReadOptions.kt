package com.engboost.monitoringdevices.core.document.api

data class ReadOptions(
    /**
     * For encrypted documents. Without it an encrypted document fails with
     * [DocumentReadException.Reason.PASSWORD_REQUIRED], with a wrong one with
     * [DocumentReadException.Reason.INVALID_PASSWORD].
     */
    val password: String? = null
)
