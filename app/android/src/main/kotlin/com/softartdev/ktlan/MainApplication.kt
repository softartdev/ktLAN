package com.softartdev.ktlan

import android.app.Application
import com.softartdev.ktlan.util.CommonAppLauncher
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.koinConfiguration

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        CommonAppLauncher.launch(
            debug = BuildConfig.DEBUG,
            koinConfig = koinConfiguration { androidContext(this@MainApplication) }
        )
    }
}
