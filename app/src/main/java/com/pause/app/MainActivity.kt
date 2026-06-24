package com.pause.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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

            // When the user returns from granting a permission in system Settings, (re)start the
            // Usage-Access monitor if it should now be running.
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshMonitorService()
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            PauseTheme {
                startOnHome?.let { home ->
                    PauseNavHost(viewModel = viewModel, startOnHome = home)
                }
            }
        }
    }
}
