package com.enmanuelbergling.corntime.core.datastore.secure

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import org.koin.core.annotation.Single
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/** Generic-password Keychain items scoped to the app's service name. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Single
internal class KeychainSecureStorage : SecureStorage {

    override suspend fun read(key: String): String? = memScoped {
        val result = alloc<CFTypeRefVar>()
        val status = withQuery(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to SERVICE.retained(),
            kSecAttrAccount to key.retained(),
            kSecReturnData to kCFBooleanTrue,
            kSecMatchLimit to kSecMatchLimitOne,
        ) { query -> SecItemCopyMatching(query, result.ptr) }
        if (status != errSecSuccess) return@memScoped null
        val data = CFBridgingRelease(result.value) as? NSData ?: return@memScoped null
        NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
    }

    override suspend fun write(key: String, value: String) {
        delete(key)
        @Suppress("CAST_NEVER_SUCCEEDS")
        val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return
        val status = withQuery(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to SERVICE.retained(),
            kSecAttrAccount to key.retained(),
            kSecValueData to data.retained(),
            kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
        ) { query -> SecItemAdd(query, null) }
        check(status == errSecSuccess) { "Keychain write failed with OSStatus $status" }
    }

    override suspend fun delete(key: String) {
        withQuery(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to SERVICE.retained(),
            kSecAttrAccount to key.retained(),
        ) { query -> SecItemDelete(query) }
    }

    /** A Kotlin/ObjC object handed to CoreFoundation; released together with the query. */
    private class Retained(val ref: CFTypeRef?)

    private fun Any.retained() = Retained(CFBridgingRetain(this))

    /**
     * Builds a CFDictionary query. Values are either CF constants (borrowed) or [Retained]
     * Kotlin objects, which are released once [block] returns.
     */
    private inline fun <T> withQuery(vararg entries: Pair<CFStringRef?, Any?>, block: (CFDictionaryRef?) -> T): T {
        val dictionary = CFDictionaryCreateMutable(
            null,
            entries.size.toLong(),
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr,
        )
        val owned = mutableListOf<CFTypeRef?>()
        entries.forEach { (key, value) ->
            val ref: CFTypeRef? = when (value) {
                is Retained -> value.ref.also { owned += it }
                else -> value as CFTypeRef?
            }
            CFDictionaryAddValue(dictionary, key, ref)
        }
        return try {
            block(dictionary)
        } finally {
            owned.forEach { CFRelease(it) }
            CFRelease(dictionary)
        }
    }

    private companion object {
        const val SERVICE = "com.enmanuelbergling.corntime"
    }
}
