package com.example.ritsu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import com.example.ritsu.ui.theme.RitsuTheme
import com.example.ritsu.ui.screens.DataScreen
import com.example.ritsu.ui.screens.ScoreScreen
import com.example.ritsu.ui.screens.OptionsScreen
import com.example.ritsu.ui.screens.ManageConfigsScreen
import com.example.ritsu.ui.screens.DebugScreen
import com.example.ritsu.ui.screens.BoxEditorScreen
import com.example.ritsu.ui.components.HeaderComponent
import com.example.ritsu.ui.components.NavigationComponent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavGraph.Companion.findStartDestination
import kotlinx.coroutines.delay

import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.example.ritsu.data.ThemeConfig
import com.example.ritsu.data.ThemeRepository
import com.example.ritsu.ui.screens.ThemeScreen

enum class Screen {
    Score,
    Data,
    Options,
    ManageConfigs,
    Debug,
    BoxEditor,
    Theme
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val themeRepository = remember { ThemeRepository(context) }
            val themeConfig by themeRepository.themeConfig.collectAsState(initial = ThemeConfig())

            RitsuTheme(themeConfig = themeConfig) {
                RitsuApp(themeRepository)
            }
        }
    }
}

@Composable
fun RitsuApp(themeRepository: ThemeRepository) {
    // This variable keeps track of which screen to show
    var showSplash by remember { mutableStateOf(true) }

    // LaunchedEffect runs when this component is first "launched"
    LaunchedEffect(Unit) {
        delay(2000) // Wait for 2 seconds
        showSplash = false // Switch to the main screen
    }

    // Crossfade creates the smooth fade transition between screens
    Crossfade(
        targetState = showSplash,
        animationSpec = tween(durationMillis = 400), // Faster transition: 0.4 seconds
        label = "SplashTransition"
    ) { isSplash ->
        if (isSplash) {
            SplashScreenContent()
        } else {
            MainContent(themeRepository)
        }
    }
}

@Composable
fun SplashScreenContent() {
    // A simple full-screen surface with the primary theme color
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.primary
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Outline Text
            Text(
                text = "律",
                style = MaterialTheme.typography.displayLarge.copy(
                    drawStyle = Stroke(
                        miter = 10f,
                        width = 6f, // This creates the 3px-out / 3px-in outline
                        join = StrokeJoin.Round
                    )
                ),
                color = MaterialTheme.colorScheme.secondary // Contrasting outline color
            )
            // Fill Text
            Text(
                text = "律",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun MainContent(themeRepository: ThemeRepository) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val currentScreen = when (navBackStackEntry?.destination?.route) {
        Screen.Data.name -> Screen.Data
        Screen.Options.name -> Screen.Options
        Screen.ManageConfigs.name -> Screen.ManageConfigs
        Screen.Debug.name -> Screen.Debug
        Screen.BoxEditor.name -> Screen.BoxEditor
        Screen.Theme.name -> Screen.Theme
        else -> Screen.Score
    }

    var scoreScrollToTopSignal by remember { mutableStateOf(0L) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            HeaderComponent(
                currentScreen = currentScreen,
                onActionClick = {
                    when (currentScreen) {
                        Screen.Score, Screen.Data -> navController.navigate(Screen.Options.name)
                        else -> navController.popBackStack()
                    }
                }
            )
        },
        bottomBar = {
            if (currentScreen == Screen.Score || currentScreen == Screen.Data) {
                NavigationComponent(
                    currentScreen = currentScreen,
                    onScreenSelected = { screen ->
                        if (screen == Screen.Score && currentScreen == Screen.Score) {
                            scoreScrollToTopSignal = System.currentTimeMillis()
                        } else {
                            navController.navigate(screen.name) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Score.name,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(200)) },
            exitTransition = { fadeOut(animationSpec = tween(200)) },
            popEnterTransition = { fadeIn(animationSpec = tween(200)) },
            popExitTransition = { fadeOut(animationSpec = tween(200)) }
        ) {
            composable(Screen.Score.name) { ScoreScreen(scrollToTopSignal = scoreScrollToTopSignal) }
            composable(Screen.Data.name) { DataScreen() }
            composable(Screen.Options.name) {
                OptionsScreen(
                    onManageConfigsClick = { navController.navigate(Screen.ManageConfigs.name) },
                    onDebugClick = { navController.navigate(Screen.Debug.name) },
                    onThemeClick = { navController.navigate(Screen.Theme.name) }
                )
            }
            composable(Screen.ManageConfigs.name) {
                ManageConfigsScreen(
                    onEditConfig = { navController.navigate(Screen.BoxEditor.name) }
                )
            }
            composable(Screen.Debug.name) { DebugScreen() }
            composable(Screen.BoxEditor.name) { BoxEditorScreen() }
            composable(Screen.Theme.name) { ThemeScreen(themeRepository) }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RitsuTheme {
        Greeting("Android")
    }
}
