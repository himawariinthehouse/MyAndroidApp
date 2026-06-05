package com.example.myandroidapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myandroidapp.screens.HomeScreen
import com.example.myandroidapp.screens.TransportScreen
import com.example.myandroidapp.screens.TimeScreen
import com.example.myandroidapp.screens.HealthScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }
        composable(Screen.Transport.route) {
            TransportScreen(navController)
        }
        composable(Screen.Time.route) {
            TimeScreen(navController)
        }
        composable(Screen.Health.route) {
            HealthScreen(navController)
        }
    }
}
