package siell.claud.authenticator.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import siell.claud.authenticator.ui.screens.*
import siell.claud.authenticator.ui.viewmodels.AuthViewModel

@Composable
fun AppNavigation(authViewModel: AuthViewModel = viewModel()) {
    val navController = rememberNavController()
    
    val userEmail by authViewModel.userEmail.collectAsState()
    val isAppLockEnabled by authViewModel.isAppLockEnabled.collectAsState()
    val isUnlocked by authViewModel.isUnlocked.collectAsState()
    
    val startDestination = if (userEmail == null) {
        "login"
    } else if (isAppLockEnabled && !isUnlocked) {
        "lock"
    } else {
        "dashboard"
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = { id, email, name, picUrl, token ->
                    authViewModel.setLoginData(id, email, name, picUrl, token)
                    val destination = if (isAppLockEnabled) "lock" else "dashboard"
                    navController.navigate(destination) {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        
        composable("lock") {
            LockScreen(
                authViewModel = authViewModel,
                onUnlockSuccess = {
                    authViewModel.unlockApp()
                    navController.navigate("dashboard") {
                        popUpTo("lock") { inclusive = true }
                        popUpTo("login") { inclusive = true } // Just in case
                    }
                }
            )
        }
        
        composable("dashboard") {
            DashboardScreen(
                authViewModel = authViewModel,
                onNavigateToAddAccount = { navController.navigate("addAccount") },
                onNavigateToProfile = { navController.navigate("profile") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        
        composable("addAccount") {
            AddAccountScreen(
                onBack = { navController.popBackStack() },
                onSave = { name, issuer, secret ->
                    authViewModel.addAccount(name, issuer, secret)
                    navController.popBackStack()
                }
            )
        }
        
        composable("profile") {
            ProfileScreen(
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable("settings") {
            SettingsScreen(
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
