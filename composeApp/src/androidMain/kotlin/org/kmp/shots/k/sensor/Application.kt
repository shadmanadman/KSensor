package org.kmp.shots.k.sensor

import android.app.Application

class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContext.setUp(applicationContext)
    }
}