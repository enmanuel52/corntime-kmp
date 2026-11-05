package com.enmanuelbergling.corntime.shared

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/**
 * Single DI entry point for both apps. Android calls it from its Application class,
 * iOS from the SwiftUI `App` initializer.
 */
fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
    }
}

/** Swift-friendly overload (default lambdas are not exported to Swift). */
fun initKoin() = initKoin {}
