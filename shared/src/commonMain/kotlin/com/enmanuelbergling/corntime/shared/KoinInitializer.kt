package com.enmanuelbergling.corntime.shared

import com.enmanuelbergling.corntime.core.data.di.CoreDataModule
import com.enmanuelbergling.corntime.core.datastore.di.CoreDatastoreModule
import com.enmanuelbergling.corntime.core.domain.di.CoreDomainModule
import com.enmanuelbergling.corntime.feature.movies.domain.di.MoviesDomainModule
import com.enmanuelbergling.corntime.feature.movies.presentation.di.MoviesPresentationModule
import org.koin.core.annotation.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.plugin.module.dsl.startKoin

/** Every module of the app. Add a feature's modules here when it is created. */
@KoinApplication(
    modules = [
        CoreDomainModule::class,
        CoreDataModule::class,
        CoreDatastoreModule::class,
        MoviesDomainModule::class,
        MoviesPresentationModule::class,
    ],
)
object CornTimeKoinApp

/**
 * Single DI entry point for both apps. Android calls it from its Application class (providing the
 * Android context in [appDeclaration]), iOS from the SwiftUI `App` initializer.
 */
fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin<CornTimeKoinApp>(appDeclaration)
}

/** Swift-friendly overload (default lambdas are not exported to Swift). */
fun initKoin() = initKoin {}
