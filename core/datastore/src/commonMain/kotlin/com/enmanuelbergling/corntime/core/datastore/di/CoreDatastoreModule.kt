package com.enmanuelbergling.corntime.core.datastore.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.enmanuelbergling.corntime.core.datastore.DataStorePathProvider
import com.enmanuelbergling.corntime.core.datastore.createPreferencesDataStore
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("com.enmanuelbergling.corntime.core.datastore")
class CoreDatastoreModule {
    @Single
    fun preferencesDataStore(pathProvider: DataStorePathProvider): DataStore<Preferences> =
        createPreferencesDataStore(pathProvider)
}
