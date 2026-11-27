package com.rookhq.monarch.sample

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import com.rookhq.monarch.sample.showcase.ShowcaseApp

/**
 * The only activity: hosts the catalogue edge to edge. Started with the boolean extra
 * `showcase=true`, it opens the landing page's showcase screen instead, in the system's theme.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val showcase = intent?.getBooleanExtra(EXTRA_SHOWCASE, false) == true
        setContent {
            if (showcase) {
                val dark = isSystemInDarkTheme()
                SideEffect { applySystemBarStyle(dark) }
                ShowcaseApp(dark = dark)
            } else {
                CatalogueApp(onDarkChanged = ::applySystemBarStyle)
            }
        }
    }

    /** Keeps the status and navigation bar icons readable on the theme chosen in the catalogue. */
    private fun applySystemBarStyle(dark: Boolean) {
        val style = if (dark) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    private companion object {
        /** The intent extra that opens the showcase screen. */
        const val EXTRA_SHOWCASE = "showcase"
    }
}
