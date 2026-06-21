package com.pause.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pause.app.ui.AppViewModel
import com.pause.app.ui.navigation.PauseNavHost
import com.pause.app.ui.theme.PauseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: AppViewModel = hiltViewModel()
            // Null until the persisted onboarding flag has actually been read; hold the splash.
            val startOnHome by viewModel.startOnHome.collectAsStateWithLifecycle()
            splash.setKeepOnScreenCondition { startOnHome == null }

            PauseTheme {
                startOnHome?.let { home ->
                    PauseNavHost(viewModel = viewModel, startOnHome = home)
                }
            }
        }
    }
}
