package com.playgroundfinder.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.playgroundfinder.app.ui.auth.AuthViewModel
import com.playgroundfinder.app.ui.auth.LoginScreen
import com.playgroundfinder.app.ui.auth.RegisterScreen
import com.playgroundfinder.app.ui.map.MapScreen
import com.playgroundfinder.app.ui.profile.ProfileScreen
import com.playgroundfinder.app.ui.subscription.SubscriptionScreen

object Routes {
    const val LOGIN        = "login"
    const val REGISTER     = "register"
    const val MAP          = "map"
    const val SUBSCRIPTION = "subscription"
    const val PROFILE      = "profile"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val startDestination = if (authViewModel.isLoggedIn) Routes.MAP else Routes.LOGIN

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.MAP) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER)
                }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Routes.MAP) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(Routes.MAP) {
            MapScreen(
                onNavigateToSubscription = {
                    navController.navigate(Routes.SUBSCRIPTION)
                },
                onNavigateToProfile = {
                    navController.navigate(Routes.PROFILE)
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.MAP) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.SUBSCRIPTION) {
            SubscriptionScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
