package com.enmanuelbergling.corntime.core.datastore

import android.content.Context
import org.koin.core.annotation.Single

@Single
internal class AndroidDataStorePathProvider(private val context: Context) : DataStorePathProvider {
    override fun absolutePath(fileName: String): String =
        context.filesDir.resolve(fileName).absolutePath
}
