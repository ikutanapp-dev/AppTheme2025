package id.ikutan.TradingRule.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.ikutan.TradingRule.data.local.dao.HistoryDao
import id.ikutan.TradingRule.data.model.History
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(private val historyDao: HistoryDao) : ViewModel() {

    val histories = historyDao.getAllHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun insertHistory(history: History) {
        viewModelScope.launch {
            historyDao.insertHistory(history)
        }
    }
}
