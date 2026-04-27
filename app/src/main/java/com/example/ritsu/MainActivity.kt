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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.FilterChip
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
import com.example.ritsu.ui.screens.ChartDetailsScreen
import com.example.ritsu.ui.screens.GameDetailsScreen
import com.example.ritsu.ui.components.HeaderComponent
import com.example.ritsu.ui.components.NavigationComponent
import com.example.ritsu.ui.screens.TopGamesScreen
import com.example.ritsu.ui.screens.TopChartsScreen
import com.example.ritsu.ui.screens.TopScoresScreen
import com.example.ritsu.ui.cards.TopGameItem
import com.example.ritsu.ui.cards.TopScoreItem
import com.example.ritsu.data.FullScoreRecord
import com.example.ritsu.data.RitsuDatabase
import com.example.ritsu.ui.components.ScoreDetailsDialog
import kotlinx.serialization.json.Json
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavType
import androidx.navigation.navArgument
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
    Theme,
    ChartDetails,
    GameDetails,
    TopGames,
    TopCharts,
    TopScores
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            HeaderComponent(
                currentScreen = currentScreen,
                subtitle = subtitle,
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
            composable(Screen.Score.name) { 
                ScoreScreen(
                    navController = navController,
                    scrollToTopSignal = scoreScrollToTopSignal
                ) 
            }
            composable(Screen.Data.name) { 
                DataScreen(navController = navController) 
            }
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
            composable(
                route = Screen.ChartDetails.name + "/{configId}/{songTitle}/{difficultyName}/{difficultyVal}",
                arguments = listOf(
                    navArgument("configId") { type = NavType.LongType },
                    navArgument("songTitle") { type = NavType.StringType },
                    navArgument("difficultyName") { type = NavType.StringType },
                    navArgument("difficultyVal") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val configId = backStackEntry.arguments?.getLong("configId") ?: 0L
                val songTitle = backStackEntry.arguments?.getString("songTitle") ?: ""
                val difficultyName = backStackEntry.arguments?.getString("difficultyName") ?: ""
                val difficultyVal = backStackEntry.arguments?.getString("difficultyVal") ?: ""
                ChartDetailsScreen(
                    configId = configId,
                    songTitle = songTitle,
                    difficultyName = difficultyName,
                    difficultyVal = difficultyVal,
                    onDifficultyClick = { cid, title, dName, dVal ->
                        val encodedTitle = android.net.Uri.encode(title)
                        val encodedDName = android.net.Uri.encode(dName)
                        val encodedDVal = android.net.Uri.encode(dVal)
                        navController.navigate(Screen.ChartDetails.name + "/$cid/$encodedTitle/$encodedDName/$encodedDVal") {
                            // Pop up to the chart details screen to "swap" rather than stack indefinitely
                            popUpTo(Screen.ChartDetails.name + "/{configId}/{songTitle}/{difficultyName}/{difficultyVal}") {
                                inclusive = true
                            }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.GameDetails.name + "/{configId}",
                arguments = listOf(navArgument("configId") { type = NavType.LongType })
            ) { backStackEntry ->
                val configId = backStackEntry.arguments?.getLong("configId") ?: 0L
                GameDetailsScreen(
                    configId = configId,
                    onChartClick = { cid, songTitle, difficultyName, difficultyVal ->
                        val encodedTitle = android.net.Uri.encode(songTitle)
                        val encodedDName = android.net.Uri.encode(difficultyName)
                        val encodedDVal = android.net.Uri.encode(difficultyVal)
                        navController.navigate(Screen.ChartDetails.name + "/$cid/$encodedTitle/$encodedDName/$encodedDVal")
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.TopGames.name + "/{range}/{option}") { backStackEntry ->
                val range = backStackEntry.arguments?.getString("range") ?: "Month"
                val option = backStackEntry.arguments?.getString("option") ?: ""
                
                val context = LocalContext.current
                val database = remember { RitsuDatabase.getDatabase(context) }
                val scores by database.scoreDao().getAllScores().collectAsState(initial = emptyList())
                val configs by database.scoreDao().getAllConfigs().collectAsState(initial = emptyList())
                val json = remember { Json { ignoreUnknownKeys = true } }

                val filteredScores = remember(scores, range, option) {
                    val currentScores = scores ?: return@remember emptyList<FullScoreRecord>()
                    val sdfDay = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
                    val sdfMonth = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
                    
                    currentScores.filter { record ->
                        val playDate = java.util.Date(record.genericScore.playTimestamp)
                        val playCalendar = java.util.Calendar.getInstance().apply { time = playDate }
                        
                        when (range) {
                            "Day" -> {
                                when (option) {
                                    "Today" -> isSameDay(playCalendar, java.util.Calendar.getInstance())
                                    "Yesterday" -> {
                                        val yesterday = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
                                        isSameDay(playCalendar, yesterday)
                                    }
                                    else -> sdfDay.format(playDate) == option
                                }
                            }
                            "Week" -> {
                                val weekRange = option.split(" - ")
                                if (weekRange.size == 2) {
                                    try {
                                        val start = sdfDay.parse(weekRange[0])
                                        val end = sdfDay.parse(weekRange[1])
                                        if (start != null && end != null) {
                                            playDate.after(start) && playDate.before(java.util.Date(end.time + 86400000))
                                        } else false
                                    } catch (e: Exception) { false }
                                } else if (option == "This Week") {
                                    val startOfWeek = java.util.Calendar.getInstance().apply { 
                                        set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
                                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                                        set(java.util.Calendar.MINUTE, 0)
                                        set(java.util.Calendar.SECOND, 0)
                                    }
                                    playDate.after(startOfWeek.time)
                                } else false
                            }
                            "Month" -> sdfMonth.format(playDate) == option
                            "Year" -> playCalendar[java.util.Calendar.YEAR].toString() == option
                            "All Time" -> true
                            else -> false
                        }
                    }
                }

                val topGames = remember(filteredScores, configs) {
                    filteredScores.groupBy { it.genericScore.configId }
                        .map { (configId, groupScores) ->
                            val config = configs.find { it.id == configId }
                            val booleanFields = config?.let {
                                try {
                                    json.decodeFromString<com.example.ritsu.data.GameConfigData>(it.configData).allFieldsWithCategory
                                        .filter { (field, _) -> field.type == "boolean" }
                                        .map { (field, _) -> field.key to field.label }
                                } catch (e: Exception) { emptyList<Pair<String, String>>() }
                            } ?: emptyList()

                            val achievementList = booleanFields.mapNotNull { (key, label) ->
                                val count = groupScores.sumOf { record ->
                                    record.details.count { it.key == key && it.value == "true" }
                                }
                                if (count > 0) label to count else null
                            }

                            TopGameItem(
                                rank = 0,
                                gameName = config?.gameName ?: "Unknown",
                                playCount = groupScores.size,
                                configId = configId,
                                achievements = achievementList,
                                displayIconUri = config?.displayIconUri
                            )
                        }
                        .sortedByDescending { it.playCount }
                        .mapIndexed { index, item -> item.copy(rank = index + 1) }
                }

                TopGamesScreen(
                    topGames = topGames,
                    onGameClick = { game ->
                        navController.navigate(Screen.GameDetails.name + "/${game.configId}")
                    }
                )
            }
            composable(Screen.TopCharts.name + "/{range}/{option}") { backStackEntry ->
                val range = backStackEntry.arguments?.getString("range") ?: "Month"
                val option = backStackEntry.arguments?.getString("option") ?: ""
                
                val context = LocalContext.current
                val database = remember { RitsuDatabase.getDatabase(context) }
                val scores by database.scoreDao().getAllScores().collectAsState(initial = emptyList())
                val configs by database.scoreDao().getAllConfigs().collectAsState(initial = emptyList())
                val json = remember { Json { ignoreUnknownKeys = true } }

                val sortMode = "Plays"

                val filteredScores = remember(scores, range, option) {
                    val currentScores = scores ?: return@remember emptyList<FullScoreRecord>()
                    val sdfDay = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
                    val sdfMonth = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
                    
                    currentScores.filter { record ->
                        val playDate = java.util.Date(record.genericScore.playTimestamp)
                        val playCalendar = java.util.Calendar.getInstance().apply { time = playDate }
                        
                        when (range) {
                            "Day" -> {
                                when (option) {
                                    "Today" -> isSameDay(playCalendar, java.util.Calendar.getInstance())
                                    "Yesterday" -> {
                                        val yesterday = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
                                        isSameDay(playCalendar, yesterday)
                                    }
                                    else -> sdfDay.format(playDate) == option
                                }
                            }
                            "Week" -> {
                                val weekRange = option.split(" - ")
                                if (weekRange.size == 2) {
                                    try {
                                        val start = sdfDay.parse(weekRange[0])
                                        val end = sdfDay.parse(weekRange[1])
                                        if (start != null && end != null) {
                                            playDate.after(start) && playDate.before(java.util.Date(end.time + 86400000))
                                        } else false
                                    } catch (e: Exception) { false }
                                } else if (option == "This Week") {
                                    val startOfWeek = java.util.Calendar.getInstance().apply { 
                                        set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
                                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                                        set(java.util.Calendar.MINUTE, 0)
                                        set(java.util.Calendar.SECOND, 0)
                                    }
                                    playDate.after(startOfWeek.time)
                                } else false
                            }
                            "Month" -> sdfMonth.format(playDate) == option
                            "Year" -> playCalendar[java.util.Calendar.YEAR].toString() == option
                            "All Time" -> true
                            else -> false
                        }
                    }
                }

                val topCharts = remember(filteredScores, configs, sortMode) {
                    filteredScores.groupBy { "${it.genericScore.configId}|${it.genericScore.songTitle}|${it.genericScore.difficultyName}|${it.genericScore.difficultyVal}" }
                        .map { (key, groupScores) ->
                            val configId = key.split("|").first().toLong()
                            val config = configs.find { it.id == configId }
                            
                            val bestRecord = if (sortMode == "Plays") {
                                groupScores.maxByOrNull { it.genericScore.playTimestamp }!!
                            } else {
                                groupScores.maxByOrNull { record ->
                                    com.example.ritsu.data.RankingUtils.calculateRankingValue(record, config, sortMode)
                                }!!
                            }

                            com.example.ritsu.data.RankingUtils.mapToTopScoreItem(
                                record = bestRecord,
                                config = config,
                                playCount = groupScores.size,
                                showRank = (sortMode != "Plays")
                            )
                        }
                        .sortedByDescending { item ->
                            if (sortMode == "Plays") {
                                item.playCount.toDouble()
                            } else {
                                val config = configs.find { it.gameName == item.gameName && it.displayIconUri == item.displayIconUri }
                                val originalRecord = filteredScores.find { 
                                    it.genericScore.songTitle == item.songTitle &&
                                    it.genericScore.difficultyName == item.difficultyName &&
                                    it.genericScore.difficultyVal == item.difficultyVal &&
                                    it.genericScore.id == item.scoreId
                                }
                                if (originalRecord != null) {
                                    com.example.ritsu.data.RankingUtils.calculateRankingValue(originalRecord, config, sortMode)
                                } else 0.0
                            }
                        }
                        .mapIndexed { index, item -> item.copy(rank = index + 1) }
                }

                var selectedScoreForDetails by remember { mutableStateOf<FullScoreRecord?>(null) }

                TopChartsScreen(
                    topCharts = topCharts,
                    sortMode = sortMode,
                    onChartClick = { chart ->
                        if (sortMode == "Plays") {
                            val config = configs.find { it.gameName == chart.gameName && it.displayIconUri == chart.displayIconUri }
                            val configId = config?.id ?: 0L
                            val encodedTitle = android.net.Uri.encode(chart.songTitle)
                            val encodedDiffName = android.net.Uri.encode(chart.difficultyName)
                            val encodedDiffVal = android.net.Uri.encode(chart.difficultyVal)
                            navController.navigate(Screen.ChartDetails.name + "/$configId/$encodedTitle/$encodedDiffName/$encodedDiffVal")
                        } else {
                            selectedScoreForDetails = filteredScores.find { it.genericScore.id == chart.scoreId }
                        }
                    }
                )

                if (selectedScoreForDetails != null) {
                    val config = configs.find { it.id == selectedScoreForDetails!!.genericScore.configId }
                    if (config != null) {
                        ScoreDetailsDialog(
                            scoreRecord = selectedScoreForDetails!!,
                            gameConfig = config,
                            scoreDao = database.scoreDao(),
                            onDismiss = { selectedScoreForDetails = null },
                            onChartDetails = {
                                val s = selectedScoreForDetails!!.genericScore
                                val encodedTitle = android.net.Uri.encode(s.songTitle)
                                val encodedDiffName = android.net.Uri.encode(s.difficultyName)
                                val encodedDiffVal = android.net.Uri.encode(s.difficultyVal)
                                selectedScoreForDetails = null
                                navController.navigate(Screen.ChartDetails.name + "/${s.configId}/$encodedTitle/$encodedDiffName/$encodedDiffVal")
                            },
                            onGameDetails = {
                                val s = selectedScoreForDetails!!.genericScore
                                selectedScoreForDetails = null
                                navController.navigate(Screen.GameDetails.name + "/${s.configId}")
                            }
                        )
                    }
                }
            }
            composable(Screen.TopScores.name + "/{range}/{option}/{sortMode}/{gameFilter}") { backStackEntry ->
                val range = backStackEntry.arguments?.getString("range") ?: "Month"
                val option = backStackEntry.arguments?.getString("option") ?: ""
                val sortMode = backStackEntry.arguments?.getString("sortMode") ?: "Accuracy"
                val gameFilter = backStackEntry.arguments?.getString("gameFilter") ?: "All"
                
                val context = LocalContext.current
                val database = remember { RitsuDatabase.getDatabase(context) }
                val scores by database.scoreDao().getAllScores().collectAsState(initial = emptyList())
                val configs by database.scoreDao().getAllConfigs().collectAsState(initial = emptyList())
                val json = remember { Json { ignoreUnknownKeys = true } }

                val filteredScores = remember(scores, range, option) {
                    val currentScores = scores ?: return@remember emptyList<FullScoreRecord>()
                    val sdfDay = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
                    val sdfMonth = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
                    
                    currentScores.filter { record ->
                        val playDate = java.util.Date(record.genericScore.playTimestamp)
                        val playCalendar = java.util.Calendar.getInstance().apply { time = playDate }
                        
                        when (range) {
                            "Day" -> {
                                when (option) {
                                    "Today" -> isSameDay(playCalendar, java.util.Calendar.getInstance())
                                    "Yesterday" -> {
                                        val yesterday = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
                                        isSameDay(playCalendar, yesterday)
                                    }
                                    else -> sdfDay.format(playDate) == option
                                }
                            }
                            "Week" -> {
                                val weekRange = option.split(" - ")
                                if (weekRange.size == 2) {
                                    try {
                                        val start = sdfDay.parse(weekRange[0])
                                        val end = sdfDay.parse(weekRange[1])
                                        if (start != null && end != null) {
                                            playDate.after(start) && playDate.before(java.util.Date(end.time + 86400000))
                                        } else false
                                    } catch (e: Exception) { false }
                                } else if (option == "This Week") {
                                    val startOfWeek = java.util.Calendar.getInstance().apply { 
                                        set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
                                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                                        set(java.util.Calendar.MINUTE, 0)
                                        set(java.util.Calendar.SECOND, 0)
                                    }
                                    playDate.after(startOfWeek.time)
                                } else false
                            }
                            "Month" -> sdfMonth.format(playDate) == option
                            "Year" -> playCalendar[java.util.Calendar.YEAR].toString() == option
                            "All Time" -> true
                            else -> false
                        }
                    }
                }

                val topScores = remember(filteredScores, configs, sortMode, gameFilter) {
                    filteredScores
                        .filter { record ->
                            if (gameFilter == "All") true
                            else {
                                val config = configs.find { it.id == record.genericScore.configId }
                                config?.gameName == gameFilter
                            }
                        }
                        .map { record ->
                            val config = configs.find { it.id == record.genericScore.configId }
                            val rankingValue = com.example.ritsu.data.RankingUtils.calculateRankingValue(record, config, sortMode)
                            Pair(record, rankingValue)
                        }
                        .sortedByDescending { it.second }
                        .mapIndexed { index, (record, _) ->
                            val config = configs.find { it.id == record.genericScore.configId }
                            com.example.ritsu.data.RankingUtils.mapToTopScoreItem(
                                record = record,
                                config = config,
                                rank = index + 1
                            )
                        }
                }

                var selectedScoreForDetails by remember { mutableStateOf<FullScoreRecord?>(null) }

                TopScoresScreen(
                    sortMode = sortMode,
                    topScores = topScores,
                    onScoreClick = { item ->
                        selectedScoreForDetails = filteredScores.find { it.genericScore.id == item.scoreId }
                    }
                )

                if (selectedScoreForDetails != null) {
                    val config = configs.find { it.id == selectedScoreForDetails!!.genericScore.configId }
                    if (config != null) {
                        ScoreDetailsDialog(
                            scoreRecord = selectedScoreForDetails!!,
                            gameConfig = config,
                            scoreDao = database.scoreDao(),
                            onDismiss = { selectedScoreForDetails = null },
                            onChartDetails = {
                                val s = selectedScoreForDetails!!.genericScore
                                val encodedTitle = android.net.Uri.encode(s.songTitle)
                                val encodedDiffName = android.net.Uri.encode(s.difficultyName)
                                val encodedDiffVal = android.net.Uri.encode(s.difficultyVal)
                                selectedScoreForDetails = null
                                navController.navigate(Screen.ChartDetails.name + "/${s.configId}/$encodedTitle/$encodedDiffName/$encodedDiffVal")
                            },
                            onGameDetails = {
                                val s = selectedScoreForDetails!!.genericScore
                                selectedScoreForDetails = null
                                navController.navigate(Screen.GameDetails.name + "/${s.configId}")
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun isSameDay(cal1: java.util.Calendar, cal2: java.util.Calendar): Boolean {
    return cal1[java.util.Calendar.YEAR] == cal2[java.util.Calendar.YEAR] &&
            cal1[java.util.Calendar.DAY_OF_YEAR] == cal2[java.util.Calendar.DAY_OF_YEAR]
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
