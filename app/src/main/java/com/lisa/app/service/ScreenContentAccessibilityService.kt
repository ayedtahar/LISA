package com.lisa.app.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.lisa.app.domain.CapturedContent
import com.lisa.app.domain.SourceType

/**
 * Observe la fenêtre au premier plan pour en extraire le texte lisible, afin que la bulle
 * flottante puisse proposer un résumé sans que l'utilisateur ait à partager le contenu.
 * Ne fait aucune capture d'écran (pas de screenshot) : il ne lit que l'arbre d'accessibilité,
 * comme le font les lecteurs d'écran.
 */
class ScreenContentAccessibilityService : AccessibilityService() {

    private val maxNodesToVisit = 400
    private val maxTextLength = 6000

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return
        }

        val packageName = event.packageName?.toString() ?: return
        if (packageName == applicationContext.packageName) {
            // Ignore notre propre bulle/overlay : ce n'est pas du contenu à résumer.
            return
        }

        val root = rootInActiveWindow ?: return
        val texts = mutableListOf<String>()
        collectText(root, texts, IntArray(1))

        val fullText = texts.joinToString(separator = " ").trim().take(maxTextLength)
        if (fullText.isBlank()) return

        CapturedScreenHolder.update(
            CapturedContent(
                sourceApp = packageName,
                sourceType = guessSourceType(packageName),
                title = texts.firstOrNull { it.length in 3..80 },
                text = fullText,
                capturedAtMillis = System.currentTimeMillis(),
            )
        )
    }

    private fun collectText(node: AccessibilityNodeInfo, out: MutableList<String>, visited: IntArray) {
        if (visited[0] >= maxNodesToVisit) return
        visited[0]++

        val text = node.text?.toString()?.trim()
        if (!text.isNullOrEmpty()) {
            out.add(text)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectText(child, out, visited)
            if (visited[0] >= maxNodesToVisit) break
        }
    }

    private fun guessSourceType(packageName: String): SourceType {
        val browsers = listOf("chrome", "firefox", "brave", "opera", "edge", "samsungbrowser", "duckduckgo", "vivaldi")
        val emailClients = listOf("gmail", "outlook", "android.email", "yahoo.mobile.client", "k9mail", "aqua.mail")
        val documentApps = listOf("docs", "sheets", "slides", "drive", "adobe.reader", "office.word", "office.excel", "office.powerpoint", "wps.moffice")

        return when {
            browsers.any { packageName.contains(it, ignoreCase = true) } -> SourceType.WEB_PAGE
            emailClients.any { packageName.contains(it, ignoreCase = true) } -> SourceType.EMAIL
            documentApps.any { packageName.contains(it, ignoreCase = true) } -> SourceType.DOCUMENT
            else -> SourceType.OTHER
        }
    }

    override fun onInterrupt() {
        // Rien à nettoyer : pas d'état retenu au-delà de CapturedScreenHolder.
    }
}
