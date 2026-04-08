package com.j4m1eb.wprimerace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.j4m1eb.wprimerace.screens.MainScreen
import com.j4m1eb.wprimerace.theme.WPrimeRaceTheme
import timber.log.Timber

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        setContent {
            WPrimeRaceTheme {
                MainScreen()
            }
        }
    }
}
