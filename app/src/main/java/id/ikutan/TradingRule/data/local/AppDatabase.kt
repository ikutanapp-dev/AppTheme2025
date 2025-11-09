package id.ikutan.TradingRule.data.local
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import id.ikutan.TradingRule.data.local.dao.UserDao
import id.ikutan.TradingRule.data.model.User
import kotlin.jvm.Volatile

// Anotasi @Database untuk mendefinisikan tabel dan versi database
@Database(entities = [User::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    // 1. DEKLARASI MEMBER: Fungsi abstrak untuk mendapatkan DAO
    // Ini adalah "member declaration" yang valid.
    abstract fun userDao(): UserDao

    // 2. DEKLARASI MEMBER: Companion objectuntuk menampung member statis
    // Ini juga "member declaration" yang valid.
    companion object {

        // 3. DEKLARASI MEMBER: Properti/variabel di dalam companion object
        // Ini adalah "member declaration" yang valid di dalam companion object.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // 4. DEKLARasi MEMBER: Fungsi di dalam companion object
        // Ini juga "member declaration" yang valid.
        fun getDatabase(context: Context): AppDatabase {
            // Logika seperti 'return', 'if', danpemanggilan fungsi harus ada DI DALAM fungsi lain.
            // Kode di bawah ini TIDAK akan menyebabkan error karena berada di dalam fungsi getDatabase().
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,"app_database" // Nama file database
                )
//                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    // ERROR: JANGAN LETAKKAN KODE SEPERTI INI DI SINI// val builder = Room.databaseBuilder(...) // <-- INI AKAN MENYEBABKAN ERROR "Expecting member declaration"
    // println("Membuat database") // <-- INI JUGA AKAN MENYEBABKAN ERROR

}