package ru.ritual.app.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.secureDataStore by preferencesDataStore(name = "secure_api")

data class YandexCredentials(val apiKey: String, val folderId: String)

class SecureApiKeyStore(private val context: Context) {
    private val alias = "ritual_yandex_credentials"
    private val encryptedKey = stringPreferencesKey("encrypted_yandex_credentials")
    private val legacyAlias = "ritual_openai_key"
    private val legacyEncryptedKey = stringPreferencesKey("encrypted_openai_key")

    val hasKey: Flow<Boolean> = context.secureDataStore.data.map { prefs ->
        !prefs[encryptedKey].isNullOrBlank()
    }

    suspend fun save(apiKey: String, folderId: String) {
        require(apiKey.isNotBlank()) { "API-ключ не может быть пустым" }
        require(folderId.isNotBlank()) { "ID каталога не может быть пустым" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val credentials = JSONObject()
            .put("apiKey", apiKey.trim())
            .put("folderId", folderId.trim())
            .toString()
        val encrypted = cipher.doFinal(credentials.toByteArray(Charsets.UTF_8))
        val payload = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        context.secureDataStore.edit {
            it[encryptedKey] = payload
            it.remove(legacyEncryptedKey)
        }
        deleteAlias(legacyAlias)
    }

    suspend fun read(): YandexCredentials? {
        val payload = context.secureDataStore.data.first()[encryptedKey]
        val parts = payload?.split(":", limit = 2) ?: return null
        if (parts.size != 2) return null
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateSecretKey(),
                GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)),
            )
            val json = JSONObject(String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8))
            YandexCredentials(json.getString("apiKey"), json.getString("folderId"))
        }.getOrNull()
    }

    suspend fun delete() {
        context.secureDataStore.edit {
            it.remove(encryptedKey)
            it.remove(legacyEncryptedKey)
        }
        deleteAlias(alias)
        deleteAlias(legacyAlias)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            generateKey()
        }
    }

    private fun deleteAlias(keyAlias: String) {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(keyAlias)) keyStore.deleteEntry(keyAlias)
    }
}
