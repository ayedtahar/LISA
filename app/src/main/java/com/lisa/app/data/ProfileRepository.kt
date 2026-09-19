package com.lisa.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.profileDataStore by preferencesDataStore(name = "lisa_profile")

/**
 * Persiste le profil utilisateur (objectifs + centres d'intérêt) localement sur l'appareil.
 */
class ProfileRepository(private val context: Context) {

    private object Keys {
        val GOALS = stringSetPreferencesKey("goals")
        val INTERESTS = stringSetPreferencesKey("interests")
    }

    val profile: Flow<UserProfile> = context.profileDataStore.data.map { prefs ->
        UserProfile(
            goals = prefs[Keys.GOALS]?.toList().orEmpty().sorted(),
            interests = prefs[Keys.INTERESTS]?.toList().orEmpty().sorted(),
        )
    }

    suspend fun save(profile: UserProfile) {
        context.profileDataStore.edit { prefs ->
            prefs[Keys.GOALS] = profile.goals.toSet()
            prefs[Keys.INTERESTS] = profile.interests.toSet()
        }
    }
}
