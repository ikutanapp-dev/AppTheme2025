package id.ikutan.TradingRule.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.ikutan.TradingRule.data.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    // Menyisipkan satu user. Jika ada konflik (misal, id sama), data lama akan diganti.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    // Mengambil semua user dan mengembalikannya sebagai Flow
    // Flow akan otomatis mengirim data baru setiap kali ada perubahan ditabel users
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<User>>

    // Mengambil satu user berdasarkan ID
    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserById(userId: Int): Flow<User>}