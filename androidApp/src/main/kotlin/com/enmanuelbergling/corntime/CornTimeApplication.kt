package com.enmanuelbergling.corntime

import android.app.Application
import com.enmanuelbergling.corntime.shared.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class CornTimeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidLogger()
            androidContext(this@CornTimeApplication)
        }
    }
}
