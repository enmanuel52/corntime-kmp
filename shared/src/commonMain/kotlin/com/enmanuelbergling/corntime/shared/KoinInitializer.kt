package com.enmanuelbergling.corntime.shared

import com.enmanuelbergling.corntime.core.data.di.CoreDataModule
import com.enmanuelbergling.corntime.core.datastore.di.CoreDatastoreModule
import com.enmanuelbergling.corntime.core.domain.di.CoreDomainModule
import com.enmanuelbergling.corntime.feature.actor.domain.di.ActorDomainModule
import com.enmanuelbergling.corntime.feature.actor.presentation.di.ActorPresentationModule
import com.enmanuelbergling.corntime.feature.auth.domain.di.AuthDomainModule
import com.enmanuelbergling.corntime.feature.auth.presentation.di.AuthPresentationModule
import com.enmanuelbergling.corntime.core.sync.di.CoreSyncModule
import com.enmanuelbergling.corntime.feature.favorites.presentation.di.FavoritesPresentationModule
import com.enmanuelbergling.corntime.feature.movies.domain.di.MoviesDomainModule
import com.enmanuelbergling.corntime.feature.movies.presentation.di.MoviesPresentationModule
import com.enmanuelbergling.corntime.feature.tvshows.domain.di.TvShowsDomainModule
import com.enmanuelbergling.corntime.feature.tvshows.presentation.di.TvShowsPresentationModule
import com.enmanuelbergling.corntime.feature.settings.presentation.di.SettingsPresentationModule
import com.enmanuelbergling.corntime.feature.watchlists.domain.di.WatchlistsDomainModule
import com.enmanuelbergling.corntime.feature.watchlists.presentation.di.WatchlistsPresentationModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.plugin.module.dsl.startKoin

/** Every module of the app. Add a feature's modules here when it is created. */
@KoinApplication(
    modules = [
        CoreDomainModule::class,
        CoreDataModule::class,
        CoreDatastoreModule::class,
        CoreSyncModule::class,
        ActorDomainModule::class,
        ActorPresentationModule::class,
        AuthDomainModule::class,
        AuthPresentationModule::class,
        SettingsPresentationModule::class,
        MoviesDomainModule::class,
        MoviesPresentationModule::class,
        TvShowsDomainModule::class,
        TvShowsPresentationModule::class,
        FavoritesPresentationModule::class,
        WatchlistsDomainModule::class,
        WatchlistsPresentationModule::class,
        SharedModule::class,
    ],
)
object CornTimeKoinApp

/** App-wide bindings that live in this module, such as [AppViewModel]. */
@Module
@ComponentScan("com.enmanuelbergling.corntime.shared")
class SharedModule

/**
 * Single DI entry point for both apps. Android calls it from its Application class (providing the
 * Android context in [appDeclaration]), iOS from the SwiftUI `App` initializer.
 */
fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin<CornTimeKoinApp>(appDeclaration)
}

/** Swift-friendly overload (default lambdas are not exported to Swift). */
fun initKoin() = initKoin {}
