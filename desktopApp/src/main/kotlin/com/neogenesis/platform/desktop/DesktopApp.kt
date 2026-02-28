package com.neogenesis.platform.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.neogenesis.platform.desktop.ui.RootScreen

object DesktopApp {
    @JvmStatic
    fun main(args: Array<String>) = application {
        Window(onCloseRequest = ::exitApplication, title = "NeoGenesis Platform") {
            MaterialTheme {
                Surface { RootScreen() }
            }
        }
    }
}
