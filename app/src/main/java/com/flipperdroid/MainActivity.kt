package com.flipperdroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.flipperdroid.ui.screens.*
import com.flipperdroid.ui.theme.FlipperTheme
import com.flipperdroid.util.PermissionManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        PermissionManager.requestAll(this)

        setContent {
            FlipperTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "main_menu"
                ) {
                    composable("main_menu") {
                        MainMenuScreen(
                            onNavigate = { route ->
                                navController.navigate(route)
                            }
                        )
                    }
                    composable("wifi") {
                        WiFiScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("ble") {
                        BLEScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("ir") {
                        IRScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("about") {
                        AboutScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}