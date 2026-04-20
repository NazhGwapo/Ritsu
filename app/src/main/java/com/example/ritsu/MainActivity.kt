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
import com.example.ritsu.ui.DataScreen
import com.example.ritsu.ui.ScoreScreen
import com.example.ritsu.ui.OptionsScreen
import com.example.ritsu.ui.ManageConfigsScreen
import com.example.ritsu.ui.HeaderComponent
import com.example.ritsu.ui.NavigationComponent
import kotlinx.coroutines.delay

enum class Screen {
    Score,
    Data,
    Options,
    ManageConfigs
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RitsuTheme {
                RitsuApp()
            }
        }
    }
}

@Composable
fun RitsuApp() {
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
            MainContent()
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
fun MainContent() {
    var currentScreen by remember { mutableStateOf(Screen.Score) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            HeaderComponent(
                currentScreen = currentScreen,
                onActionClick = {
                    when (currentScreen) {
                        Screen.ManageConfigs -> currentScreen = Screen.Options
                        Screen.Options -> currentScreen = Screen.Score
                        else -> currentScreen = Screen.Options
                    }
                }
            )
        },
        bottomBar = {
            NavigationComponent(
                currentScreen = currentScreen,
                onScreenSelected = { currentScreen = it }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                Screen.Score -> ScoreScreen()
                Screen.Data -> DataScreen()
                Screen.Options -> OptionsScreen(
                    onManageConfigsClick = { currentScreen = Screen.ManageConfigs }
                )
                Screen.ManageConfigs -> ManageConfigsScreen()
            }
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
