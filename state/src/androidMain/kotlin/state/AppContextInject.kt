package state

import android.app.Application
import android.content.Context
import androidx.startup.Initializer

internal class AppContextInject : Initializer<AppContext> {
    override fun create(context: Context): AppContext {
        AppContext.setUp(context)
        return AppContext
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

}