package com.pause.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pause.app.ui.AppViewModel
import com.pause.app.ui.home.HomeScreen
import com.pause.app.ui.onboarding.OnboardingScreen

private object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
}

@Composable
fun PauseNavHost(
    viewModel: AppViewModel,
    startOnHome: Boolean,
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = if (startOnHome) Routes.HOME else Routes.ONBOARDING,
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                viewModel = viewModel,
                onFinished = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(viewModel = viewModel)
        }
    }
}
