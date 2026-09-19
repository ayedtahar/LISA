package com.lisa.app.domain

import com.lisa.app.data.UserProfile

/**
 * Abstraction du moteur de résumé. L'implémentation par défaut ([MockSummarizationEngine])
 * est un simple placeholder local : elle permet de valider l'architecture et l'UI sans
 * dépendre d'un LLM. Elle est conçue pour être remplacée plus tard par un moteur réel
 * (ex. API Claude) sans changer le reste de l'app.
 */
interface SummarizationEngine {
    suspend fun summarize(content: CapturedContent, profile: UserProfile): SummaryResult
}
