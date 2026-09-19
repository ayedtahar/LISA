package com.lisa.app.domain

import com.lisa.app.data.UserProfile

/**
 * Implémentation locale et déterministe, sans appel réseau : elle raccourcit le texte capté
 * et relie les mots-clés du profil au contenu par simple correspondance textuelle.
 * Sert de bouchon pendant que l'architecture/UI se construit ; à remplacer par un vrai
 * moteur (LLM) derrière la même interface [SummarizationEngine].
 */
class MockSummarizationEngine : SummarizationEngine {

    override suspend fun summarize(content: CapturedContent, profile: UserProfile): SummaryResult {
        val summary = buildShortSummary(content)
        val matched = matchProfileKeywords(content.text, profile)
        val relevance = buildRelevance(matched, profile)
        return SummaryResult(
            shortSummary = summary,
            personalRelevance = relevance,
            matchedInterests = matched,
        )
    }

    private fun buildShortSummary(content: CapturedContent): String {
        val cleaned = content.text.replace(Regex("\\s+"), " ").trim()
        if (cleaned.isEmpty()) return "Aucun texte lisible n'a été détecté sur cet écran."
        val maxLength = 220
        if (cleaned.length <= maxLength) return cleaned
        val cut = cleaned.substring(0, maxLength)
        val lastSpace = cut.lastIndexOf(' ').takeIf { it > 0 } ?: maxLength
        return cut.substring(0, lastSpace).trimEnd() + "…"
    }

    private fun matchProfileKeywords(text: String, profile: UserProfile): List<String> {
        val lowerText = text.lowercase()
        return (profile.interests + profile.goals)
            .distinct()
            .filter { keyword -> keyword.isNotBlank() && lowerText.contains(keyword.lowercase()) }
    }

    private fun buildRelevance(matched: List<String>, profile: UserProfile): String {
        if (profile.isEmpty) {
            return "Renseigne tes objectifs et centres d'intérêt dans ton profil pour que LISA t'explique en quoi ce contenu te concerne."
        }
        if (matched.isEmpty()) {
            return "Ce contenu ne semble pas lié directement à tes objectifs ou centres d'intérêt actuels."
        }
        return "Ce contenu touche à " + matched.joinToString(", ") + " : ça peut valoir le coup de creuser."
    }
}
