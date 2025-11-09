package id.ikutan.TradingRule.data.model

// model yang digunakan di seluruh aplikasi

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int =0, // Primary key yang akan dibuat otomatis
    val name: String,
    val email: String
)