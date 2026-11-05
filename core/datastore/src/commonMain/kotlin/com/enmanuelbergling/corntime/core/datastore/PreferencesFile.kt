package com.enmanuelbergling.corntime.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

internal const val PREFERENCES_FILE_NAME = "corntime.preferences_pb"

/** Absolute path where a DataStore file lives on each platform. */
interface DataStorePathProvider {
    fun absolutePath(fileName: String): String
}

fun createPreferencesDataStore(pathProvider: DataStorePathProvider): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { pathProvider.absolutePath(PREFERENCES_FILE_NAME).toPath() },
    )
