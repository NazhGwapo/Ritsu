package com.example.ritsu.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ritsu.data.ThemeRepository
import com.example.ritsu.ui.screens.*

@Composable
fun RitsuNavGraph(
    navController: NavHostController,
    innerPadding: PaddingValues,
    themeRepository: ThemeRepository,
    scoreScrollToTopSignal: Long,
    showDataExportDialog: Boolean = false,
    onDismissDataExport: () -> Unit = {}
) {
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
            DataScreen(
                navController = navController,
                showExportDialog = showDataExportDialog,
                onDismissExport = onDismissDataExport
            )
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
                        popUpTo(Screen.ChartDetails.name + "/{configId}/{songTitle}/{difficultyName}/{difficultyVal}") {
                            inclusive = true
                        }
                    }
                },
                onGameClick = { cid ->
                    navController.navigate(Screen.GameDetails.name + "/$cid")
                },
                onGraphClick = { cid, type, title, dName, dVal, isNormalized ->
                    var route = Screen.GraphDetail.name + "/$cid/$type"
                    val params = mutableListOf<String>()
                    if (title != null) params.add("songTitle=${android.net.Uri.encode(title)}")
                    if (dName != null) params.add("difficultyName=${android.net.Uri.encode(dName)}")
                    if (dVal != null) params.add("difficultyVal=${android.net.Uri.encode(dVal)}")
                    params.add("isNormalized=$isNormalized")
                    if (params.isNotEmpty()) {
                        route += "?" + params.joinToString("&")
                    }
                    navController.navigate(route)
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
                onGraphClick = { cid, type, title, dName, dVal, isNormalized ->
                    var route = Screen.GraphDetail.name + "/$cid/$type"
                    val params = mutableListOf<String>()
                    if (title != null) params.add("songTitle=${android.net.Uri.encode(title)}")
                    if (dName != null) params.add("difficultyName=${android.net.Uri.encode(dName)}")
                    if (dVal != null) params.add("difficultyVal=${android.net.Uri.encode(dVal)}")
                    params.add("isNormalized=$isNormalized")
                    if (params.isNotEmpty()) {
                        route += "?" + params.joinToString("&")
                    }
                    navController.navigate(route)
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.TopGames.name + "/{range}/{option}") { backStackEntry ->
            val range = backStackEntry.arguments?.getString("range") ?: "Month"
            val option = backStackEntry.arguments?.getString("option") ?: ""
            
            TopGamesScreen(
                range = range,
                option = option,
                onGameClick = { game ->
                    navController.navigate(Screen.GameDetails.name + "/${game.configId}")
                }
            )
        }
        composable(Screen.TopCharts.name + "/{range}/{option}") { backStackEntry ->
            val range = backStackEntry.arguments?.getString("range") ?: "Month"
            val option = backStackEntry.arguments?.getString("option") ?: ""
            
            TopChartsScreen(
                range = range,
                option = option,
                navController = navController
            )
        }
        composable(Screen.TopScores.name + "/{range}/{option}/{sortMode}/{gameFilter}") { backStackEntry ->
            val range = backStackEntry.arguments?.getString("range") ?: "Month"
            val option = backStackEntry.arguments?.getString("option") ?: ""
            val sortMode = backStackEntry.arguments?.getString("sortMode") ?: "Accuracy"
            val gameFilter = backStackEntry.arguments?.getString("gameFilter") ?: "All"
            
            TopScoresScreen(
                range = range,
                option = option,
                sortMode = sortMode,
                gameFilter = gameFilter,
                navController = navController
            )
        }
        composable(
            route = Screen.GraphDetail.name + "/{configId}/{graphType}?songTitle={songTitle}&difficultyName={difficultyName}&difficultyVal={difficultyVal}&isNormalized={isNormalized}",
            arguments = listOf(
                navArgument("configId") { type = NavType.LongType },
                navArgument("graphType") { type = NavType.StringType },
                navArgument("songTitle") { 
                    type = NavType.StringType
                    nullable = true 
                },
                navArgument("difficultyName") { 
                    type = NavType.StringType
                    nullable = true 
                },
                navArgument("difficultyVal") { 
                    type = NavType.StringType
                    nullable = true 
                },
                navArgument("isNormalized") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val configId = backStackEntry.arguments?.getLong("configId") ?: 0L
            val graphType = backStackEntry.arguments?.getString("graphType") ?: "Score"
            val songTitle = backStackEntry.arguments?.getString("songTitle")
            val difficultyName = backStackEntry.arguments?.getString("difficultyName")
            val difficultyVal = backStackEntry.arguments?.getString("difficultyVal")
            val isNormalized = backStackEntry.arguments?.getBoolean("isNormalized") ?: false
            
            GraphDetailScreen(
                configId = configId,
                graphType = graphType,
                songTitle = songTitle,
                difficultyName = difficultyName,
                difficultyVal = difficultyVal,
                isNormalized = isNormalized,
                onBack = { navController.popBackStack() },
                onChartDetails = { cid: Long, title: String, dName: String, dVal: String ->
                    val encodedTitle = android.net.Uri.encode(title)
                    val encodedDName = android.net.Uri.encode(dName)
                    val encodedDVal = android.net.Uri.encode(dVal)
                    navController.navigate(Screen.ChartDetails.name + "/$cid/$encodedTitle/$encodedDName/$encodedDVal") {
                        popUpTo(Screen.ChartDetails.name + "/{configId}/{songTitle}/{difficultyName}/{difficultyVal}") {
                            inclusive = true
                        }
                    }
                },
                onGameDetails = { cid: Long ->
                    navController.navigate(Screen.GameDetails.name + "/$cid")
                }
            )
        }
    }
}
