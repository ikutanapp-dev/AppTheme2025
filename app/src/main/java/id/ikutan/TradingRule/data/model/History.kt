package id.ikutan.TradingRule.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class History(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val pair: String = "btcusdt",
    val timestamp: Long = System.currentTimeMillis(),
    val leverage: Int = 1,
    val rule: String = "dippy of dippy",
    val dragdown: Float = 5f,
    val status: String = "waiting",
    val result: String = "-"
)
