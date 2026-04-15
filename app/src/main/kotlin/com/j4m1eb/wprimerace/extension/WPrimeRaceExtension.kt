package com.j4m1eb.wprimerace.extension

import com.j4m1eb.wprimerace.BuildConfig
import com.j4m1eb.wprimerace.settings.WPrimeRaceSettings
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.KarooExtension
import org.koin.android.ext.android.inject
import timber.log.Timber

class WPrimeRaceExtension : KarooExtension("karoowprimerace", BuildConfig.VERSION_NAME) {

    private val karooSystem: KarooSystemService by inject()
    private val settings: WPrimeRaceSettings by inject()

    override val types by lazy {
        listOf(
            WPrimeTTDataType(karooSystem, settings, extension),
            WPrimeCritDataType(karooSystem, settings, extension),
            WPrimeCritTimeDataType(karooSystem, settings, extension),
            WPrimeCritUsableDataType(karooSystem, settings, extension),
            WPrimeCritZeroDataType(karooSystem, settings, extension),
        )
    }

    override fun onCreate() {
        super.onCreate()
        karooSystem.connect { connected ->
            Timber.d("Karoo connected: $connected")
        }
    }

    override fun onDestroy() {
        karooSystem.disconnect()
        super.onDestroy()
    }
}
