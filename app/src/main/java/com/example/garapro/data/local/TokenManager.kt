package com.example.garapro.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.garapro.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = Constants.USER_PREFERENCES
)

class TokenManager(private val context: Context) {

    private val ACCESS_TOKEN_KEY = stringPreferencesKey(Constants.ACCESS_TOKEN_KEY)
    private val USER_ROLE_KEY = stringPreferencesKey("user_role")
    private val USER_ID_KEY = stringPreferencesKey("user_id")

    suspend fun saveAccessToken(accessToken: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = accessToken
        }
    }

    fun getAccessToken(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[ACCESS_TOKEN_KEY]
        }
    }

    suspend fun getAccessTokenSync(): String? {
        return context.dataStore.data.first()[ACCESS_TOKEN_KEY]
    }

    suspend fun clearTokens() {
        context.dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY)
        }
    }

    suspend fun saveUserRole(role: String) {
        context.dataStore.edit { it[USER_ROLE_KEY] = role }
    }

    suspend fun getUserRole(): String? {
        return context.dataStore.data.first()[USER_ROLE_KEY]
    }


    suspend fun clearUserRole() {
        context.dataStore.edit { it.remove(USER_ROLE_KEY) }
    }

    suspend fun saveUserId(userId: String) {
        context.dataStore.edit { it[USER_ID_KEY] = userId }
    }

    fun getUserId(): Flow<String?> {
        return context.dataStore.data.map { prefs -> prefs[USER_ID_KEY] }
    }

    suspend fun getUserIdSync(): String? {
        return context.dataStore.data.first()[USER_ID_KEY]
    }

    suspend fun clearUserId() {
        context.dataStore.edit { it.remove(USER_ID_KEY) }
    }

    suspend fun clearAll() {
        context.dataStore.edit {
            it.remove(ACCESS_TOKEN_KEY)
            it.remove(USER_ROLE_KEY)
            it.remove(USER_ID_KEY)
        }
    }

}