package id.ikutan.TradingRule.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class History(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val pair: String = "btcusdt",
    val timestamp: Long = System.currentTimeMillis(),
    val leverage: Int,
    val rule: String,
    val dragdown: Float,
    val status: String = "waiting",
    val result: Float
)
