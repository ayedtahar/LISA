package com.lisa.app.data

/**
 * Préférences renseignées par l'utilisateur, utilisées pour juger la pertinence
 * d'un résumé par rapport à sa vie (ses objectifs et ses centres d'intérêt).
 */
data class UserProfile(
    val goals: List<String> = emptyList(),
    val interests: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = goals.isEmpty() && interests.isEmpty()

    companion object {
        val EMPTY = UserProfile()
    }
}
