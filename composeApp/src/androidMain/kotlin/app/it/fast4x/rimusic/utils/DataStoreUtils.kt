package app.it.fast4x.rimusic.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

object DataStoreUtils {
    
    // Key constants
    const val KEY_USERNAME = "username"
    // Add other keys here as needed
    // const val KEY_SOME_SETTING = "some_setting"
    
    suspend fun saveString(context: Context, key: String, value: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(key)] = value
        }
    }
    
    // Add this blocking version for synchronous saving
    fun saveStringBlocking(context: Context, key: String, value: String) {
        runBlocking {
            context.dataStore.edit { preferences ->
                preferences[stringPreferencesKey(key)] = value
            }
        }
    }
    
    fun getStringFlow(context: Context, key: String, default: String = ""): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[stringPreferencesKey(key)] ?: default
        }
    }
    
    fun getStringBlocking(context: Context, key: String, default: String = ""): String {
        return runBlocking {
            getStringFlow(context, key, default).first()
        }
    }
    
    suspend fun saveBoolean(context: Context, key: String, value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[booleanPreferencesKey(key)] = value
        }
    }
    
    // Add this blocking version for synchronous saving
    fun saveBooleanBlocking(context: Context, key: String, value: Boolean) {
        runBlocking {
            context.dataStore.edit { preferences ->
                preferences[booleanPreferencesKey(key)] = value
            }
        }
    }
    
    fun getBooleanFlow(context: Context, key: String, default: Boolean = false): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[booleanPreferencesKey(key)] ?: default
        }
    }
    
    fun getBooleanBlocking(context: Context, key: String, default: Boolean = false): Boolean {
        return runBlocking {
            getBooleanFlow(context, key, default).first()
        }
    }
}