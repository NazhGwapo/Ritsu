package com.example.ritsu.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ritsu.data.GenericScore
import com.example.ritsu.ui.cards.ScoreCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 5

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Guide") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    Text(
                        "${currentStep + 1} / $totalSteps",
                        modifier = Modifier.padding(end = 16.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = { if (currentStep > 0) currentStep-- },
                        enabled = currentStep > 0
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Back")
                    }

                    if (currentStep < totalSteps - 1) {
                        Button(onClick = { currentStep++ }) {
                            Text("Next")
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    } else {
                        Button(onClick = onBack) {
                            Text("Finish")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    fadeIn().togetherWith(fadeOut())
                },
                label = "StepTransition"
            ) { step ->
                when (step) {
                    0 -> TutorialStepWelcome()
                    1 -> TutorialStepManageConfig()
                    2 -> TutorialStepBoxEditor()
                    3 -> TutorialStepImporting()
                    4 -> TutorialStepScores()
                }
            }
        }
    }
}

@Composable
fun TutorialStepWelcome() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Welcome to Ritsu!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Ritsu is an OCR-powered rhythm game score tracker. Automatically extract and save your play records from screenshots.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TutorialStepManageConfig() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "1. Define Your Game",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Start by creating a 'Configuration' for your game. This tells Ritsu which areas of the screen contain the song title, score, and rank.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        // Improved Mockup of ManageConfigsScreen
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 1.dp
        ) {
            Column {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("BanG Dream!", style = MaterialTheme.typography.titleSmall)
                        Text("12 fields defined", style = MaterialTheme.typography.bodySmall)
                    }
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("D4DJ Groovy Mix", style = MaterialTheme.typography.titleSmall)
                        Text("10 fields defined", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add New Game Config", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun TutorialStepBoxEditor() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "2. Map the Screen",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Upload a reference screenshot and draw 'Boxes' over the data you want to track. Ritsu will scan these areas in all future uploads.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Mockup of BoxEditor with a "Game" screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1A1A1A))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
        ) {
            // Fake Game UI
            Column(modifier = Modifier.padding(16.dp)) {
                Box(modifier = Modifier.fillMaxWidth().height(40.dp).background(Color.DarkGray.copy(alpha = 0.5f), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                    Text("SONG TITLE AREA", color = Color.Gray, fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.height(60.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Box(modifier = Modifier.size(80.dp).background(Color.DarkGray.copy(alpha = 0.5f), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                        Text("RANK", color = Color.Gray, fontSize = 10.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Box(modifier = Modifier.width(120.dp).height(30.dp).background(Color.DarkGray.copy(alpha = 0.5f), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                            Text("SCORE", color = Color.Gray, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.width(80.dp).height(20.dp).background(Color.DarkGray.copy(alpha = 0.5f), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                            Text("ACCURACY", color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                }
            }

            // Highlighting Boxes (Simulating Editor)
            Box(
                modifier = Modifier
                    .padding(14.dp)
                    .fillMaxWidth()
                    .height(44.dp)
                    .border(2.dp, Color.Cyan, RoundedCornerShape(2.dp))
                    .background(Color.Cyan.copy(alpha = 0.1f))
            ) {
                Text("Song Title", color = Color.Cyan, fontSize = 8.sp, modifier = Modifier.padding(2.dp))
            }
            
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(top = 105.dp, end = 14.dp)
                    .width(124.dp)
                    .height(34.dp)
                    .border(2.dp, Color.Yellow, RoundedCornerShape(2.dp))
                    .background(Color.Yellow.copy(alpha = 0.1f))
            ) {
                Text("Total Score", color = Color.Yellow, fontSize = 8.sp, modifier = Modifier.padding(2.dp))
            }
        }
    }
}

@Composable
fun TutorialStepImporting() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "3. Fast Import",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "To track your progress, just tap the Camera icon. Select your game config and pick one or more screenshots from your gallery.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Mockup of NavigationBar highlighting the FAB
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 4.dp,
            shape = RoundedCornerShape(24.dp),
            border = borderStroke()
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                
                // Pulsing-like effect for the FAB
                Box(contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(72.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape))
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(28.dp))
                    }
                }

                Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun TutorialStepScores() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "4. Analyze Your Growth",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "View your score history, filter by game, and see charts of your accuracy over time. Ritsu helps you see where you're improving!",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Actual ScoreCard with dummy values
        ScoreCard(
            score = GenericScore(
                id = 0,
                configId = 0,
                songTitle = "KIZUNA MUSIC",
                difficultyName = "EXPERT",
                difficultyVal = "26",
                difficultySortValue = 26.0,
                totalScore = 985000,
                maxCombo = 780,
                accuracy = 99.54,
                playRank = "S",
                playTimestamp = System.currentTimeMillis(),
                importTimestamp = System.currentTimeMillis()
            ),
            gameName = "BanG Dream!",
            displayIconUri = null,
            useRank = true,
            booleanLabels = listOf("Full Combo"),
            onClick = {},
            onEdit = {},
            onDelete = {}
        )
    }
}

@Composable
private fun borderStroke() = androidx.compose.foundation.BorderStroke(
    width = 1.dp,
    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
)
