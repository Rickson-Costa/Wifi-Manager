package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument

@Composable
fun WifiManagerApp(viewModel: NetworkViewModel, context: Context) {
    val navController = rememberNavController()
    
    val prefs = context.getSharedPreferences("wifi_manager_prefs", Context.MODE_PRIVATE)
    val hasAcceptedLgpd = prefs.getBoolean("lgpd_accepted", false)
    val startDestination = if (hasAcceptedLgpd) "dashboard" else "onboarding"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("onboarding") {
            OnboardingScreen(onAccept = {
                prefs.edit().putBoolean("lgpd_accepted", true).apply()
                navController.navigate("dashboard") {
                    popUpTo("onboarding") { inclusive = true }
                }
            })
        }
        composable("dashboard") {
            DashboardScreen(viewModel = viewModel, onNavigateToDevice = { mac ->
                val encodedMac = Uri.encode(mac)
                navController.navigate("device/$encodedMac")
            })
        }
        composable(
            route = "device/{mac}",
            arguments = listOf(navArgument("mac") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedMac = backStackEntry.arguments?.getString("mac") ?: ""
            val mac = Uri.decode(encodedMac)
            DeviceDetailScreen(
                mac = mac,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

