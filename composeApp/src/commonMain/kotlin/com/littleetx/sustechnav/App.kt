package com.littleetx.sustechnav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.compose.AppTheme
import com.littleetx.sustechnav.screens.MainScreen
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.ui.tooling.preview.Preview

sealed class NavDestination
@Serializable
object MainDestination : NavDestination()

@OptIn(ExperimentalResourceApi::class)
@Composable
@Preview
fun App() {
    val navController = rememberNavController()
    AppTheme {
        NavHost(navController, startDestination = MainDestination) {
            composable<MainDestination> {
                MainScreen()
            }
        }
    }
}