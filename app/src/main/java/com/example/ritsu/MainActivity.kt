package com.example.ritsu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ritsu.ui.theme.RitsuTheme
import com.example.ritsu.ui.components.HeaderComponent
import com.example.ritsu.ui.components.NavigationComponent
import com.example.ritsu.ui.components.SplashScreenContent
import com.example.ritsu.ui.navigation.RitsuNavGraph
import com.example.ritsu.ui.navigation.Screen
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavGraph.Companion.findStartDestination
import kotlinx.coroutines.delay

import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.example.ritsu.data.ThemeConfig
import com.example.ritsu.data.ThemeRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val themeRepository = remember { ThemeRepository(context) }
            val themeConfigState = themeRepository.themeConfig.collectAsState(initial = null)

            val themeConfig = themeConfigState.value
            if (themeConfig != null) {
                RitsuTheme(themeConfig = themeConfig) {
                    RitsuApp(themeRepository)
                }
            }
        }
    }
}

@Composable
fun RitsuApp(themeRepository: ThemeRepository) {
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(2000)
        showSplash = false
    }

    Crossfade(
        targetState = showSplash,
        animationSpec = tween(durationMillis = 400),
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
fun MainContent(themeRepository: ThemeRepository) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val currentScreen = when {
        navBackStackEntry?.destination?.route?.startsWith(Screen.Data.name) == true -> Screen.Data
        navBackStackEntry?.destination?.route?.startsWith(Screen.Options.name) == true -> Screen.Options
        navBackStackEntry?.destination?.route?.startsWith(Screen.ManageConfigs.name) == true -> Screen.ManageConfigs
        navBackStackEntry?.destination?.route?.startsWith(Screen.Debug.name) == true -> Screen.Debug
        navBackStackEntry?.destination?.route?.startsWith(Screen.BoxEditor.name) == true -> Screen.BoxEditor
        navBackStackEntry?.destination?.route?.startsWith(Screen.Theme.name) == true -> Screen.Theme
        navBackStackEntry?.destination?.route?.startsWith(Screen.ChartDetails.name) == true -> Screen.ChartDetails
        navBackStackEntry?.destination?.route?.startsWith(Screen.GameDetails.name) == true -> Screen.GameDetails
        navBackStackEntry?.destination?.route?.startsWith(Screen.TopGames.name) == true -> Screen.TopGames
        navBackStackEntry?.destination?.route?.startsWith(Screen.TopCharts.name) == true -> Screen.TopCharts
        navBackStackEntry?.destination?.route?.startsWith(Screen.TopScores.name) == true -> Screen.TopScores
        navBackStackEntry?.destination?.route?.startsWith(Screen.GraphDetail.name) == true -> Screen.GraphDetail
        navBackStackEntry?.destination?.route?.startsWith(Screen.Help.name) == true -> Screen.Help
        else -> Screen.Score
    }

    val subtitle = when (currentScreen) {
        Screen.TopGames, Screen.TopCharts -> navBackStackEntry?.arguments?.getString("option")
        Screen.TopScores -> {
            val opt = navBackStackEntry?.arguments?.getString("option")
            val sort = navBackStackEntry?.arguments?.getString("sortMode")
            if (opt != null && sort != null) "$opt ($sort)" else opt
        }
        else -> null
    }

    var scoreScrollToTopSignal by remember { mutableStateOf(0L) }
    var showDataExportDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (currentScreen != Screen.Help && currentScreen != Screen.BoxEditorHelp) {
                HeaderComponent(
                    currentScreen = currentScreen,
                    subtitle = subtitle,
                    onActionClick = {
                        when (currentScreen) {
                            Screen.Score, Screen.Data -> navController.navigate(Screen.Options.name)
                            else -> navController.popBackStack()
                        }
                    },
                    onShareClick = {
                        showDataExportDialog = true
                    }
                )
            }
        },
        bottomBar = {
            if ((currentScreen == Screen.Score || currentScreen == Screen.Data) && currentScreen != Screen.Help && currentScreen != Screen.BoxEditorHelp) {
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
        RitsuNavGraph(
            navController = navController,
            innerPadding = innerPadding,
            themeRepository = themeRepository,
            scoreScrollToTopSignal = scoreScrollToTopSignal,
            showDataExportDialog = showDataExportDialog,
            onDismissDataExport = { showDataExportDialog = false }
        )
    }
}
