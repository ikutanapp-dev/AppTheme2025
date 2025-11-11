package id.ikutan.TradingRule.ui.home.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import id.ikutan.TradingRule.data.local.dao.HistoryDao

class ViewModelFactory(private val historyDao: HistoryDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(historyDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
