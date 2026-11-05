package com.enmanuelbergling.corntime.core.data.di

import com.enmanuelbergling.corntime.core.data.network.createTmdbHttpClient
import io.ktor.client.HttpClient
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("com.enmanuelbergling.corntime.core.data")
class CoreDataModule {
    @Single
    fun tmdbHttpClient(): HttpClient = createTmdbHttpClient()
}
