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
import androidx.compose.material.icons.filled.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxEditorHelpScreen(onBack: () -> Unit) {
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 5

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Box Editor Guide") },
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
                            Text("Start Mapping!")
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
                label = "BoxEditorStepTransition"
            ) { step ->
                when (step) {
                    0 -> BoxStepSelectConfig()
                    1 -> BoxStepImportImage()
                    2 -> BoxStepDrawBoxes()
                    3 -> BoxStepNavigation()
                    4 -> BoxStepDefineFields()
                }
            }
        }
    }
}

@Composable
fun BoxStepSelectConfig() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Spacer(Modifier.height(24.dp))
        Text("Select a Game Configuration", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text(
            "Use the dropdown menu at the top of the editor to pick which game you are currently mapping. Each game needs its own set of boxes.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(32.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("BanG Dream!", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }
    }
}

@Composable
fun BoxStepImportImage() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.height(24.dp))
        Text("Upload a Reference Screenshot", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text(
            "Tap the image icon in the top bar to upload a clear, high-resolution screenshot of a result screen. This serves as the template for your mapping.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun BoxStepDrawBoxes() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Draw and Refine Boxes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text(
            "Draw a box by dragging your finger across the area you want to scan. Tap a label in the bottom bar first to select WHICH field you are mapping, then draw its box.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(32.dp))
        
        // Improved Mockup
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF121212))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
        ) {
            // Background mockup (Game-like)
            Column(modifier = Modifier.padding(24.dp).fillMaxSize()) {
                Box(modifier = Modifier.fillMaxWidth().height(32.dp).background(Color.DarkGray.copy(alpha = 0.5f), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                    Text("GAME SONG TITLE", color = Color.Gray, fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.height(60.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Box(modifier = Modifier.size(60.dp).background(Color.DarkGray.copy(alpha = 0.5f), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                        Text("RANK", color = Color.Gray, fontSize = 10.sp)
                    }
                    Box(modifier = Modifier.width(120.dp).height(40.dp).background(Color.DarkGray.copy(alpha = 0.5f), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                        Text("SCORE AREA", color = Color.Gray, fontSize = 10.sp)
                    }
                }
            }

            // Highlighting the active mapping
            Box(
                modifier = Modifier
                    .offset(x = 22.dp, y = 22.dp)
                    .fillMaxWidth()
                    .padding(end = 44.dp)
                    .height(36.dp)
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                // Resize Handles
                listOf(Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd).forEach { align ->
                    Box(
                        modifier = Modifier
                            .align(align)
                            .offset(
                                x = if (align == Alignment.TopStart || align == Alignment.BottomStart) (-4).dp else 4.dp,
                                y = if (align == Alignment.TopStart || align == Alignment.TopEnd) (-4).dp else 4.dp
                            )
                            .size(10.dp)
                            .background(Color.White, CircleShape)
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
                Text("Mapping: Song Title", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(2.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
            }
        }
    }
}

@Composable
fun BoxStepNavigation() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ZoomIn, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
        }
        Spacer(Modifier.height(24.dp))
        Text("Navigation and Zoom", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text(
            "Use the floating toolbar on the top right to switch to 'View' mode. In this mode, you can pinch to zoom and drag to pan without modifying any boxes.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(32.dp))
        
        Surface(
            modifier = Modifier.width(64.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(8.dp))
                Icon(Icons.Default.PanTool, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(modifier = Modifier.width(24.dp))
                Spacer(Modifier.height(12.dp))
                Icon(Icons.Default.Refresh, contentDescription = null)
            }
        }
    }
}

@Composable
fun BoxStepDefineFields() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Assign Fields to Boxes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text(
            "Once you've placed a box, tap 'Edit Field' at the bottom to define its purpose. Select whether it's the 'Score', 'Combo', 'Rank', or a custom detail like 'Greats'.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(32.dp))
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 4.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("Field: Total Score", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text("Type: Number (Clean)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Don't forget to tap the SAVE icon in the top bar when you are finished mapping!",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.error
        )
    }
}
