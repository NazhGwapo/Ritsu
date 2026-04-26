package com.example.ritsu.ui.screens.debug

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ritsu.data.GenericScore
import com.example.ritsu.ui.cards.ScoreCard

@Composable
fun ScoreCardEditorDialog(onDismiss: () -> Unit) {
    var songTitle by remember { mutableStateOf("Song Name") }
    var gameName by remember { mutableStateOf("Game") }
    var difficultyName by remember { mutableStateOf("Difficulty") }
    var difficultyVal by remember { mutableStateOf("22.0") }
    var totalScore by remember { mutableStateOf("1234567") }
    var accuracy by remember { mutableStateOf("98.76") }
    var playRank by remember { mutableStateOf("RANK") }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val context = LocalContext.current
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                selectedBitmap = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                @Suppress("DEPRECATION")
                selectedBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ScoreCard Preview", 
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                
                ScoreCard(
                    score = GenericScore(
                        configId = 0,
                        songTitle = songTitle,
                        difficultyName = difficultyName,
                        difficultyVal = difficultyVal,
                        difficultySortValue = GenericScore.parseDifficulty(difficultyVal),
                        totalScore = totalScore.toLongOrNull() ?: 0L,
                        maxCombo = 0,
                        accuracy = accuracy.toDoubleOrNull() ?: 0.0,
                        playRank = playRank,
                        playTimestamp = System.currentTimeMillis(),
                        importTimestamp = System.currentTimeMillis()
                    ),
                    gameName = gameName,
                    imageBitmap = selectedBitmap
                )

                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextField(value = songTitle, onValueChange = { songTitle = it }, label = { Text("Song Title") }, modifier = Modifier.fillMaxWidth())
                    TextField(value = gameName, onValueChange = { gameName = it }, label = { Text("Game Name") }, modifier = Modifier.fillMaxWidth())
                    TextField(value = difficultyName, onValueChange = { difficultyName = it }, label = { Text("Difficulty Name") }, modifier = Modifier.fillMaxWidth())
                    TextField(value = difficultyVal, onValueChange = { difficultyVal = it }, label = { Text("Difficulty Value") }, modifier = Modifier.fillMaxWidth())
                    TextField(value = totalScore, onValueChange = { totalScore = it }, label = { Text("Total Score") }, modifier = Modifier.fillMaxWidth())
                    TextField(value = accuracy, onValueChange = { accuracy = it }, label = { Text("Accuracy") }, modifier = Modifier.fillMaxWidth())
                    TextField(value = playRank, onValueChange = { playRank = it }, label = { Text("Play Rank") }, modifier = Modifier.fillMaxWidth())
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = { pickerLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                        Text("Select Card Image")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
