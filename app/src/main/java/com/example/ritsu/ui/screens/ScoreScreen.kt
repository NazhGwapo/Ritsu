package com.example.ritsu.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ritsu.R
import com.example.ritsu.data.FullScoreRecord
import com.example.ritsu.data.GameConfigData
import com.example.ritsu.data.RitsuDatabase
import com.example.ritsu.ui.cards.ScoreCard
import com.example.ritsu.ui.components.ScoreDetailsDialog
import com.example.ritsu.ui.screens.debug.ManualEntryDialog
import com.example.ritsu.ui.utils.ScoreFilterUtils
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

enum class ScoreSortMode {
    PLAY_DATE_DESC, PLAY_DATE_ASC,
    IMPORT_DATE_DESC, IMPORT_DATE_ASC,
    SCORE_DESC, SCORE_ASC,
    ACCURACY_DESC, ACCURACY_ASC
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreScreen(
    scrollToTopSignal: Long = 0L,
) {
    val context = LocalContext.current
    val database = remember { RitsuDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    val json = remember { Json { ignoreUnknownKeys = true } }

    // State
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(ScoreSortMode.PLAY_DATE_DESC) }
    var selectedGames by remember { mutableStateOf(setOf<Long>()) }
    var showFilterDialog by remember { mutableStateOf(false) }

    var selectedScore by remember { mutableStateOf<FullScoreRecord?>(null) }
    var scoreToEdit by remember { mutableStateOf<FullScoreRecord?>(null) }

    val scores by database.scoreDao().getAllScores().collectAsState(initial = null)
    val configs by database.scoreDao().getAllConfigs().collectAsState(initial = null)
    val configMap = remember(configs) { configs?.associateBy { it.id } ?: emptyMap() }

    // Filtering & Sorting Logic
    val processedScores = remember(searchQuery, scores, configMap, sortMode, selectedGames) {
        val queries = ScoreFilterUtils.parseQuery(searchQuery)
        
        scores?.filter { record ->
            val config = configMap[record.genericScore.configId]
            val gameName = config?.gameName ?: "Unknown Game"
            
            // 1. Game Filter
            if (selectedGames.isNotEmpty() && !selectedGames.contains(record.genericScore.configId)) return@filter false
            
            // 2. Search Query
            ScoreFilterUtils.matches(record, gameName, queries)
        }?.sortedWith { a, b ->
            val s1 = a.genericScore
            val s2 = b.genericScore
            when (sortMode) {
                ScoreSortMode.PLAY_DATE_DESC -> s2.playTimestamp.compareTo(s1.playTimestamp)
                ScoreSortMode.PLAY_DATE_ASC -> s1.playTimestamp.compareTo(s2.playTimestamp)
                ScoreSortMode.IMPORT_DATE_DESC -> s2.importTimestamp.compareTo(s1.importTimestamp)
                ScoreSortMode.IMPORT_DATE_ASC -> s1.importTimestamp.compareTo(s2.importTimestamp)
                ScoreSortMode.SCORE_DESC -> s2.totalScore.compareTo(s1.totalScore)
                ScoreSortMode.SCORE_ASC -> s1.totalScore.compareTo(s2.totalScore)
                ScoreSortMode.ACCURACY_DESC -> s2.accuracy.compareTo(s1.accuracy)
                ScoreSortMode.ACCURACY_ASC -> s1.accuracy.compareTo(s2.accuracy)
            }
        }
    }

    // UI Scroll Behavior (Auto-hide search bar)
    val searchBarHeight = 80.dp
    val searchBarHeightPx = with(LocalDensity.current) { searchBarHeight.toPx() }
    var searchBarOffsetHeightPx by remember { mutableFloatStateOf(0f) }

    val listState = rememberLazyListState()

    val nestedScrollConnection = remember(listState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Only allow hiding if we can actually scroll forward (down) 
                // OR if it's already partially hidden and we're scrolling back up
                if (!listState.canScrollForward && searchBarOffsetHeightPx == 0f) return Offset.Zero
                
                val delta = available.y
                val newOffset = searchBarOffsetHeightPx + delta
                searchBarOffsetHeightPx = newOffset.coerceIn(-searchBarHeightPx, 0f)
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > 0) {
            listState.animateScrollToItem(0)
            searchBarOffsetHeightPx = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        // Main List
        if (processedScores == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (processedScores.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.ohnoes),
                        contentDescription = null,
                        modifier = Modifier.size(128.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isEmpty()) "No scores yet" else "No matching scores",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (searchQuery.isNotEmpty() || selectedGames.isNotEmpty()) {
                        TextButton(onClick = { 
                            searchQuery = ""
                            selectedGames = emptySet()
                        }) {
                            Text("Clear filters")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(top = searchBarHeight, bottom = 16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(processedScores) { fullRecord ->
                    val score = fullRecord.genericScore
                    val config = configMap[score.configId]
                    val gameName = config?.gameName ?: "Unknown Game"
                    
                    val useRank = remember(config) {
                        if (config == null) true else {
                            try {
                                json.decodeFromString<GameConfigData>(config.configData).useRankOcr
                            } catch (_: Exception) {
                                true
                            }
                        }
                    }

                    val booleanLabels = remember(fullRecord, config) {
                        if (config == null) emptyList<String>() else {
                            try {
                                val configData = json.decodeFromString<GameConfigData>(config.configData)
                                val booleanFields = configData.allFieldsWithCategory
                                    .filter { it.first.type == "boolean" }
                                    .map { it.first.key to it.first.label }
                                    .toMap()
                                
                                fullRecord.details
                                    .filter { booleanFields.containsKey(it.key) && it.value.lowercase() == "true" }
                                    .map { booleanFields[it.key] ?: "" }
                            } catch (_: Exception) {
                                emptyList<String>()
                            }
                        }
                    }

                    ScoreCard(
                        score = score,
                        gameName = gameName,
                        displayIconUri = config?.displayIconUri,
                        useRank = useRank,
                        booleanLabels = booleanLabels,
                        onClick = { selectedScore = fullRecord },
                        onEdit = { scoreToEdit = fullRecord },
                        onDelete = {
                            scope.launch {
                                database.scoreDao().deleteScoreById(score.id)
                            }
                        }
                    )
                }
            }
        }

        // Search Bar (Floating & Auto-hiding)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(searchBarHeight)
                .offset { IntOffset(x = 0, y = searchBarOffsetHeightPx.roundToInt()) },
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search scores...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
                
                IconButton(
                    onClick = { showFilterDialog = true },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (selectedGames.isNotEmpty()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter")
                }
            }
        }
    }

    // Dialogs & Sheets
    if (showFilterDialog) {
        FilterSortDialog(
            configs = configs ?: emptyList(),
            currentSortMode = sortMode,
            selectedGames = selectedGames,
            onDismiss = { showFilterDialog = false },
            onApply = { newSort, newGames ->
                sortMode = newSort
                selectedGames = newGames
                showFilterDialog = false
            }
        )
    }

    selectedScore?.let { record ->
        val config = configMap[record.genericScore.configId]
        if (config != null) {
            ScoreDetailsDialog(
                scoreRecord = record,
                gameConfig = config,
                scoreDao = database.scoreDao(),
                onDismiss = { selectedScore = null },
                onEdit = {
                    selectedScore = null
                    scoreToEdit = record
                }
            )
        }
    }

    if (scoreToEdit != null) {
        ManualEntryDialog(
            initialRecord = scoreToEdit,
            onDismiss = { scoreToEdit = null }
        )
    }
}

@Composable
fun FilterSortDialog(
    configs: List<com.example.ritsu.data.GameConfig>,
    currentSortMode: ScoreSortMode,
    selectedGames: Set<Long>,
    onDismiss: () -> Unit,
    onApply: (ScoreSortMode, Set<Long>) -> Unit
) {
    var tempSortMode by remember { mutableStateOf(currentSortMode) }
    var tempSelectedGames by remember { mutableStateOf(selectedGames) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter & Sort", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sorting Section
                Text("Sort By", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                
                val sortGroups = listOf(
                    "Play Date" to (ScoreSortMode.PLAY_DATE_DESC to ScoreSortMode.PLAY_DATE_ASC),
                    "Import Date" to (ScoreSortMode.IMPORT_DATE_DESC to ScoreSortMode.IMPORT_DATE_ASC),
                    "Score" to (ScoreSortMode.SCORE_DESC to ScoreSortMode.SCORE_ASC),
                    "Accuracy" to (ScoreSortMode.ACCURACY_DESC to ScoreSortMode.ACCURACY_ASC)
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    sortGroups.forEach { (label, modes) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = tempSortMode == modes.first,
                                    onClick = { tempSortMode = modes.first },
                                    label = { Text("DESC", fontSize = 10.sp) }
                                )
                                FilterChip(
                                    selected = tempSortMode == modes.second,
                                    onClick = { tempSortMode = modes.second },
                                    label = { Text("ASC", fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                // Game Filtering Section
                Text("Filter Games", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                
                // Using a simple Column with small items since FlowRow had issues
                if (configs.isEmpty()) {
                    Text("No games found.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    // Chunk into groups of 2 for a pseudo-grid
                    configs.chunked(2).forEach { rowConfigs ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            rowConfigs.forEach { config ->
                                FilterChip(
                                    selected = tempSelectedGames.contains(config.id),
                                    onClick = {
                                        tempSelectedGames = if (tempSelectedGames.contains(config.id)) {
                                            tempSelectedGames - config.id
                                        } else {
                                            tempSelectedGames + config.id
                                        }
                                    },
                                    label = { 
                                        Text(
                                            config.gameName, 
                                            maxLines = 1, 
                                            overflow = TextOverflow.Ellipsis,
                                            fontSize = 11.sp
                                        ) 
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowConfigs.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(tempSortMode, tempSelectedGames) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
