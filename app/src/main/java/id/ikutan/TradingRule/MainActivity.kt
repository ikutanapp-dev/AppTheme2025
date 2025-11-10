package id.ikutan.TradingRule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.ikutan.TradingRule.data.local.AppDatabase
import id.ikutan.TradingRule.data.model.History
import id.ikutan.TradingRule.ui.history.HistoryViewModel
import id.ikutan.TradingRule.ui.history.ViewModelFactory

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
                onAddHistory = { historyViewModel.insertHistory(it) }
            )
        }
    }
}

@Composable
fun HistoryScreen(histories: List<History>, onAddHistory: (History) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
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
                HistoryItem(history = history)
            }
        }
    }

    if (showDialog) {
        AddHistoryDialog(
            onDismissRequest = { showDialog = false },
            onConfirmation = {
                onAddHistory(it)
                showDialog = false
            }
        )
    }
}

@Composable
fun AddHistoryDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: (History) -> Unit,
) {
    var pair by remember { mutableStateOf("btcusdt") }
    var leverage by remember { mutableStateOf("") }
    var rule by remember { mutableStateOf("") }
    var dragdown by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("waiting") }
    var result by remember { mutableStateOf("") }

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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(
                onClick = {
                    val newHistory = History(
                        pair = pair,
                        leverage = leverage.toIntOrNull() ?: 0,
                        rule = rule,
                        dragdown = dragdown.toFloatOrNull() ?: 0f,
                        status = status,
                        result = result.toFloatOrNull() ?: 0f
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
fun HistoryItem(history: History) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Pair: ${history.pair}")
            Text(text = "Timestamp: ${history.timestamp}")
            Text(text = "Leverage: ${history.leverage}")
            Text(text = "Rule: ${history.rule}")
            Text(text = "Dragdown: ${history.dragdown}")
            Text(text = "Status: ${history.status}")
            Text(text = "Result: ${history.result}")
        }
    }
}
