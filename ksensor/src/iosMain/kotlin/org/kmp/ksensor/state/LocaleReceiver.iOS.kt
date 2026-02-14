package org.kmp.ksensor.state

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCurrentLocaleDidChangeNotification
import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleIdentifier
import platform.Foundation.NSLocaleLanguageDirectionRightToLeft
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.characterDirectionForLanguage
import platform.Foundation.countryCode
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.localeIdentifier
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
internal class LocaleReceiver(
    val onLocaleChanged: (StateData.LocaleInfo) -> Unit
) {
    private var observer: NSObject? = null

    fun registerObserver() {
        observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = NSCurrentLocaleDidChangeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue()
        ) { _ ->
            onLocaleChanged(getCurrentLocale())
        } as NSObject?
    }

    fun removeObserver() {
        observer?.let {
            NSNotificationCenter.defaultCenter.removeObserver(it)
            observer = null
        }
    }

    fun getCurrentLocale(): StateData.LocaleInfo {
        val locale = NSLocale.currentLocale
        val languageCode = locale.languageCode

        val direction = NSLocale.characterDirectionForLanguage(languageCode)
        val isRtl = direction == NSLocaleLanguageDirectionRightToLeft

        return StateData.LocaleInfo(
            languageCode = languageCode,
            countryCode = locale.countryCode ?: "",
            fullLocaleString = locale.localeIdentifier,
            displayName = locale.displayNameForKey(
                NSLocaleIdentifier,
                locale.localeIdentifier
            ) ?: locale.localeIdentifier,
            isRTL = isRtl
        )
    }
}
