package id.ikutan.TradingRule

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.ikutan.TradingRule.data.local.AppDatabase
import id.ikutan.TradingRule.data.model.History
import id.ikutan.TradingRule.ui.home.history.HistoryViewModel
import id.ikutan.TradingRule.ui.home.history.ViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val database by lazy { AppDatabase.getDatabase(this) }
    private val historyViewModel: HistoryViewModel by viewModels {
        ViewModelFactory(database.historyDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val histories by historyViewModel.histories.collectAsState()
            HistoryScreen(
                histories = histories,
                onAddHistory = { historyViewModel.insertHistory(it) },
                onUpdateHistory = { historyViewModel.updateHistory(it) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    histories: List<History>,
    onAddHistory: (History) -> Unit,
    onUpdateHistory: (History) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showImpactDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var selectedHistory by remember { mutableStateOf<History?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Trading History") },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Settings"
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Impact") },
                            onClick = { 
                                showImpactDialog = true
                                showMenu = false 
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Parameter") },
                            onClick = { /* TODO: Handle Parameter click */ }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Text(text = "+")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(histories) { history ->
                HistoryItem(
                    history = history,
                    onLongClick = { selectedHistory = it }
                )
            }
        }
    }

    if (showAddDialog) {
        AddHistoryDialog(
            onDismissRequest = { showAddDialog = false },
            onConfirmation = {
                onAddHistory(it)
                showAddDialog = false
            }
        )
    }

    if (showImpactDialog) {
        ImpactSettingsDialog(onDismissRequest = { showImpactDialog = false })
    }

    selectedHistory?.let {
        UpdateResultDialog(
            history = it,
            onDismissRequest = { selectedHistory = null },
            onConfirmation = { updatedHistory ->
                onUpdateHistory(updatedHistory)
                selectedHistory = null
            }
        )
    }
}

@Composable
fun ImpactSettingsDialog(onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("impact_setting", Context.MODE_PRIVATE) }
    
    var smallImpact by remember { mutableStateOf(sharedPreferences.getString("small_impact", "") ?: "") }
    var mediumImpact by remember { mutableStateOf(sharedPreferences.getString("medium_impact", "") ?: "") }
    var highImpact by remember { mutableStateOf(sharedPreferences.getString("high_impact", "") ?: "") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = "Impact Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = smallImpact,
                    onValueChange = { smallImpact = it },
                    label = { Text("Small Impact") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                TextField(
                    value = mediumImpact,
                    onValueChange = { mediumImpact = it },
                    label = { Text("Medium Impact") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                TextField(
                    value = highImpact,
                    onValueChange = { highImpact = it },
                    label = { Text("High Impact") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    with(sharedPreferences.edit()) {
                        putString("small_impact", smallImpact)
                        putString("medium_impact", mediumImpact)
                        putString("high_impact", highImpact)
                        apply()
                    }
                    onDismissRequest()
                }
            ) {
                Text("Save")
            }
        }
    )
}


@Composable
fun UpdateResultDialog(
    history: History,
    onDismissRequest: () -> Unit,
    onConfirmation: (History) -> Unit
) {
    AlertDialog(
        title = { Text(text = "Update Result") },
        text = { Text(text = "Update the result for ${history.pair}") },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onConfirmation(history.copy(result = "success")) }) {
                    Text("Success")
                }
                Button(onClick = { onConfirmation(history.copy(result = "failed")) }) {
                    Text("Failed")
                }
            }
        }
    )
}

@Composable
fun AddHistoryDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: (History) -> Unit,
) {
    var pair by remember { mutableStateOf("btcusdt") }
    var leverage by remember { mutableStateOf("1") }
    var rule by remember { mutableStateOf("dippy of dippy") }
    var dragdown by remember { mutableStateOf("5.0") }
    var status by remember { mutableStateOf("waiting") }
    var result by remember { mutableStateOf("-") }

    AlertDialog(
        title = { Text(text = "Add New History") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = pair, onValueChange = { pair = it }, label = { Text("Pair") })
                TextField(
                    value = leverage,
                    onValueChange = { leverage = it },
                    label = { Text("Leverage") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                TextField(value = rule, onValueChange = { rule = it }, label = { Text("Rule") })
                TextField(
                    value = dragdown,
                    onValueChange = { dragdown = it },
                    label = { Text("Dragdown") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                TextField(value = status, onValueChange = { status = it }, label = { Text("Status") })
                TextField(
                    value = result,
                    onValueChange = { result = it },
                    label = { Text("Result") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(
                onClick = {
                    val newHistory = History(
                        pair = pair,
                        leverage = leverage.toIntOrNull() ?: 1,
                        rule = rule.ifEmpty { "dippy of dippy" },
                        dragdown = dragdown.toFloatOrNull() ?: 5f,
                        status = status,
                        result = result.ifEmpty { "-" }
                    )
                    onConfirmation(newHistory)
                }
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            Button(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun HistoryItem(history: History, onLongClick: (History) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongClick(history) }
                )
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${history.pair} X${history.leverage}",
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = formatTimestamp(history.timestamp),
                fontStyle = FontStyle.Italic
            )
            Text(
                text = "Rule : ${history.rule} -${history.dragdown}%",
                color = Color.Blue
            )
            Text(
                text = "${history.status} ${history.result}",
                color = when (history.result.lowercase(Locale.ROOT)) {
                    "success" -> Color.Green
                    "failed" -> Color.Red
                    else -> Color.Black
                }
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd-MMM-yyyy, HH.mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
