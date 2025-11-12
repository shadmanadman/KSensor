package org.kmp.shots.k.sensor

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController {
    KState.init(IosStateControllerFactory.init())
}