package org.kmp.shots.k.sensor

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContext.setUp(applicationContext)
    }
}