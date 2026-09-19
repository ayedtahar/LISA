package com.lisa.app.domain

enum class SourceType {
    WEB_PAGE,
    EMAIL,
    DOCUMENT,
    OTHER,
}

/**
 * Ce que la bulle a réussi à lire à l'écran au moment où l'utilisateur l'a sollicitée.
 */
data class CapturedContent(
    val sourceApp: String,
    val sourceType: SourceType,
    val title: String?,
    val text: String,
    val capturedAtMillis: Long,
) {
    val isUsable: Boolean get() = text.isNotBlank()
}
