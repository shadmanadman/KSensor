package org.kmp.shots.k.sensor

import android.content.Context
import androidx.startup.Initializer

internal class AppContextInject : Initializer<AppContext> {
    override fun create(context: Context): AppContext {
        AppContext.setUp(context)
        KState.setController(AndroidStateHandler())
        return AppContext
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

}