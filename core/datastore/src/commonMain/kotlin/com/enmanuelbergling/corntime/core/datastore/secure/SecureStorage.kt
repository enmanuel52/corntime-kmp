package com.enmanuelbergling.corntime.core.datastore.secure

/**
 * Small key/value store for secrets. Android encrypts with an Android Keystore AES key;
 * iOS stores items in the Keychain.
 */
interface SecureStorage {
    suspend fun read(key: String): String?
    suspend fun write(key: String, value: String)
    suspend fun delete(key: String)
}
