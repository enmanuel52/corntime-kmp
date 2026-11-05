package com.enmanuelbergling.corntime.core.datastore

import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.annotation.Single
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@Single
internal class IosDataStorePathProvider : DataStorePathProvider {
    @OptIn(ExperimentalForeignApi::class)
    override fun absolutePath(fileName: String): String {
        val directory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )
        return requireNotNull(directory?.path) { "Documents directory unavailable" } + "/$fileName"
    }
}
