package com.enmanuelbergling.corntime.core.datastore.secure

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Single
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Values are AES-GCM encrypted with a non-exportable Android Keystore key and stored Base64-encoded
 * in the app preferences file (port of Pop-Corns' Crypto, moved from CBC to authenticated GCM).
 */
@Single
internal class KeystoreSecureStorage(
    private val dataStore: DataStore<Preferences>,
) : SecureStorage {

    override suspend fun read(key: String): String? {
        val stored = dataStore.data.first()[preferenceKey(key)] ?: return null
        return runCatching { decrypt(Base64.decode(stored, Base64.NO_WRAP)).decodeToString() }.getOrNull()
    }

    override suspend fun write(key: String, value: String) {
        val encrypted = Base64.encodeToString(encrypt(value.encodeToByteArray()), Base64.NO_WRAP)
        dataStore.edit { it[preferenceKey(key)] = encrypted }
    }

    override suspend fun delete(key: String) {
        dataStore.edit { it.remove(preferenceKey(key)) }
    }

    private fun preferenceKey(key: String) = stringPreferencesKey("secure_$key")

    private fun encrypt(bytes: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, secretKey()) }
        return cipher.iv + cipher.doFinal(bytes)
    }

    private fun decrypt(bytes: ByteArray): ByteArray {
        val iv = bytes.copyOfRange(0, IV_SIZE)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_SIZE_BITS, iv))
        }
        return cipher.doFinal(bytes.copyOfRange(IV_SIZE, bytes.size))
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
        }.generateKey()
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "corntime_secure_storage"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_SIZE_BITS = 128
    }
}
