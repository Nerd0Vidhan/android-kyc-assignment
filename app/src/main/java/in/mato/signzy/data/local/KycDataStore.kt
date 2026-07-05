package `in`.mato.signzy.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name="kyc_prefs")

class KycDataStore(private val context:Context) {


    fun isVerified(id:Int):Flow<Boolean> {
        val key=booleanPreferencesKey("verified_$id")

        return context.dataStore.data.map{prefs->prefs[key]?:false}
    }
    fun getSelfiePath(id:Int):Flow<String?> {

        val key=stringPreferencesKey("selfie_$id")
        return context.dataStore.data.map{prefs->prefs[key]}

    }

    suspend fun saveKyc(id:Int,selfiePath:String) {

        val verId=booleanPreferencesKey("verified_$id")
        val selfie=stringPreferencesKey("selfie_$id")
        context.dataStore.edit{prefs->
            prefs[verId]=true
            prefs[selfie]=selfiePath
        }
    }

    fun isDarkTheme():Flow<Boolean> {

        val key=booleanPreferencesKey("dark_theme")
        return context.dataStore.data.map{prefs->prefs[key]?:false}
    }

    suspend fun saveTheme(isDark:Boolean) {
        val key=booleanPreferencesKey("dark_theme")
        context.dataStore.edit{prefs-> prefs[key]=isDark }
    }
}
