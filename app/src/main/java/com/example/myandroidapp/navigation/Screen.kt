package com.example.myandroidapp.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Transport : Screen("transport")
    object Time : Screen("time")
    object Health : Screen("health")
}
