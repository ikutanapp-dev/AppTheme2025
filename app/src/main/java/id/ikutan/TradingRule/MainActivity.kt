package id.ikutan.TradingRule

import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
                onUpdateHistory = { historyViewModel.updateHistory(it) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    histories: List<History>,
    onUpdateHistory: (History) -> Unit
) {
    val context = LocalContext.current
    var showImpactDialog by remember { mutableStateOf(false) }
    var showParameterDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var selectedHistory by remember { mutableStateOf<History?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Trading History") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFADD8E6) // Light Blue
                ),
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
                            onClick = {
                                showParameterDialog = true
                                showMenu = false
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { context.startActivity(Intent(context, CalculationActivity::class.java)) }) {
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

    if (showImpactDialog) {
        ImpactSettingsDialog(onDismissRequest = { showImpactDialog = false })
    }

    if (showParameterDialog) {
        ParameterSettingsDialog(onDismissRequest = { showParameterDialog = false })
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
fun ParameterSettingsDialog(onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("parameter_setting", Context.MODE_PRIVATE) }

    var pair by remember { mutableStateOf(sharedPreferences.getString("pair", "btcusdt") ?: "btcusdt") }
    var leverage by remember { mutableStateOf(sharedPreferences.getString("leverage", "1") ?: "1") }
    var rule by remember { mutableStateOf(sharedPreferences.getString("rule", "dippy of dippy") ?: "dippy of dippy") }
    var dragdown by remember { mutableStateOf(sharedPreferences.getString("dragdown", "5.0") ?: "5.0") }
    var status by remember { mutableStateOf(sharedPreferences.getString("status", "waiting") ?: "waiting") }
    var result by remember { mutableStateOf(sharedPreferences.getString("result", "-") ?: "-") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = "Parameter Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextField(value = pair, onValueChange = { pair = it }, label = { Text("Default Pair") })
                TextField(value = leverage, onValueChange = { leverage = it }, label = { Text("Default Leverage") })
                TextField(value = rule, onValueChange = { rule = it }, label = { Text("Default Rule") })
                TextField(value = dragdown, onValueChange = { dragdown = it }, label = { Text("Default Dragdown") })
                TextField(value = status, onValueChange = { status = it }, label = { Text("Default Status") })
                TextField(value = result, onValueChange = { result = it }, label = { Text("Default Result") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    with(sharedPreferences.edit()) {
                        putString("pair", pair)
                        putString("leverage", leverage)
                        putString("rule", rule)
                        putString("dragdown", dragdown)
                        putString("status", status)
                        putString("result", result)
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
