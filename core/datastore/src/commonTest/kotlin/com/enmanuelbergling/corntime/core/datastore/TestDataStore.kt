package com.enmanuelbergling.corntime.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.random.Random

/** A DataStore backed by a unique temporary file, scoped to the test's [scope]. */
fun testPreferencesDataStore(scope: CoroutineScope): DataStore<Preferences> {
    val path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "corntime-test-${Random.nextLong()}.preferences_pb"
    return PreferenceDataStoreFactory.createWithPath(scope = scope, produceFile = { path.toString().toPath() })
}
