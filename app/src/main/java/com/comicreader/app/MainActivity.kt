package com.comicreader.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import com.comicreader.app.data.Comic
import com.comicreader.app.ui.screens.LibraryScreen
import com.comicreader.app.ui.screens.ReaderScreen
import com.comicreader.app.ui.theme.ComicReaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            ComicReaderTheme {
                var currentComic by remember { mutableStateOf<Comic?>(null) }

                if (currentComic != null) {
                    ReaderScreen(
                        comic = currentComic!!,
                        onBack = { currentComic = null }
                    )
                } else {
                    LibraryScreen(
                        onOpenComic = { comic ->
                            currentComic = comic
                        }
                    )
                }
            }
        }
    }
}
