package com.j4m1eb.wprimerace

import android.app.Application
import com.j4m1eb.wprimerace.settings.WPrimeRaceSettings
import io.hammerhead.karooext.KarooSystemService
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule = module {
    single { KarooSystemService(androidContext()) }
    singleOf(::WPrimeRaceSettings)
}

class WPrimeRaceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@WPrimeRaceApplication)
            modules(appModule)
        }
    }
}
