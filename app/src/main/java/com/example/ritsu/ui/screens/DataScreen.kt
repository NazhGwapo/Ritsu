package com.example.ritsu.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.ritsu.R
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen() {
    val ranges = listOf("Day", "Week", "Month", "Year")
    var selectedRange by remember { mutableStateOf(ranges[0]) }
    var rangeExpanded by remember { mutableStateOf(false) }

    // Using Calendar for maximum compatibility and stability
    val options = remember(selectedRange) {
        val list = mutableListOf<String>()
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

        when (selectedRange) {
            "Day" -> {
                for (i in 0..365) { // Increased range to 1 year
                    val d = Calendar.getInstance()
                    d.add(Calendar.DAY_OF_YEAR, -i)
                    list.add(when(i) {
                        0 -> "Today"
                        1 -> "Yesterday"
                        else -> sdf.format(d.time)
                    })
                }
            }
            "Week" -> {
                for (i in 0..156) { // Increased range to 3 years
                    val d = Calendar.getInstance()
                    d.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    d.add(Calendar.WEEK_OF_YEAR, -i)
                    val end = d.clone() as Calendar
                    end.add(Calendar.DAY_OF_YEAR, 6)
                    list.add(if (i == 0) "This Week" else "${sdf.format(d.time)} - ${sdf.format(end.time)}")
                }
            }
            "Month" -> {
                val monthSdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                for (i in 0..60) { // Increased range to 5 years
                    val d = Calendar.getInstance()
                    d.add(Calendar.MONTH, -i)
                    list.add(monthSdf.format(d.time))
                }
            }
            "Year" -> {
                val year = calendar.get(Calendar.YEAR)
                for (i in 0..20) { // Increased range to 20 years
                    list.add((year - i).toString())
                }
            }
        }
        list
    }

    var selectedOption by remember(selectedRange) { mutableStateOf(options.firstOrNull() ?: "") }
    var optionExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Range Dropdown
            ExposedDropdownMenuBox(
                expanded = rangeExpanded,
                onExpandedChange = { rangeExpanded = !rangeExpanded },
                modifier = Modifier.weight(0.4f)
            ) {
                OutlinedTextField(
                    value = selectedRange,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Range") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(rangeExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                )
                ExposedDropdownMenu(
                    expanded = rangeExpanded,
                    onDismissRequest = { rangeExpanded = false }
                ) {
                    ranges.forEach { range ->
                        DropdownMenuItem(
                            text = { Text(range) },
                            onClick = {
                                selectedRange = range
                                rangeExpanded = false
                            }
                        )
                    }
                }
            }

            // Selection Trigger
            Box(modifier = Modifier.weight(0.6f)) {
                OutlinedTextField(
                    value = selectedOption,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Selection") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(optionExpanded) },
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { optionExpanded = true }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.ohnoes),
                    contentDescription = null,
                    modifier = Modifier.size(128.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "No data for $selectedOption")
            }
        }
    }

    if (optionExpanded) {
        ModalBottomSheet(
            onDismissRequest = { optionExpanded = false }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                item {
                    Text(
                        text = "Select $selectedRange",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                items(options) { option ->
                    ListItem(
                        headlineContent = { Text(option) },
                        modifier = Modifier.clickable {
                            selectedOption = option
                            optionExpanded = false
                        }
                    )
                }
            }
        }
    }
}
