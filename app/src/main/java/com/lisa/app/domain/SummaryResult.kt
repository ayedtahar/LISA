package com.lisa.app.domain

/**
 * Résultat présenté à l'utilisateur : le résumé rapide du contenu, et en quoi
 * il touche à ses objectifs / centres d'intérêt déclarés dans son profil.
 */
data class SummaryResult(
    val shortSummary: String,
    val personalRelevance: String,
    val matchedInterests: List<String>,
)
