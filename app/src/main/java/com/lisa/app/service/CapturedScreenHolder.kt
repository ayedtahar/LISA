package com.lisa.app.service

import com.lisa.app.domain.CapturedContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Point de partage entre le service d'accessibilité (qui lit l'écran en continu) et
 * la bulle flottante (qui, au moment du tap, a juste besoin du dernier contenu connu).
 */
object CapturedScreenHolder {

    private val _latest = MutableStateFlow<CapturedContent?>(null)
    val latest: StateFlow<CapturedContent?> = _latest.asStateFlow()

    fun update(content: CapturedContent) {
        _latest.value = content
    }

    fun snapshot(): CapturedContent? = _latest.value
}
