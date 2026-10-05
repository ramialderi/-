package com.example.prayers.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [PrayerLogEntity::class, TasbihItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PrayerDatabase : RoomDatabase() {

    abstract fun prayerDao(): PrayerDao

    companion object {
        @Volatile
        private var INSTANCE: PrayerDatabase? = null

        fun getDatabase(context: Context): PrayerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PrayerDatabase::class.java,
                    "ela_salaty.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Prepopulate default tasbih items
                        CoroutineScope(Dispatchers.IO).launch {
                            val dao = getDatabase(context).prayerDao()
                            dao.insertTasbihItem(TasbihItemEntity(title = "سُبْحَانَ اللَّهِ", count = 0, target = 33))
                            dao.insertTasbihItem(TasbihItemEntity(title = "الْحَمْدُ لِلَّهِ", count = 0, target = 33))
                            dao.insertTasbihItem(TasbihItemEntity(title = "اللَّهُ أَكْبَرُ", count = 0, target = 33))
                            dao.insertTasbihItem(TasbihItemEntity(title = "لَا إِلَهَ إِلَّا اللَّهُ", count = 0, target = 100))
                            dao.insertTasbihItem(TasbihItemEntity(title = "أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ", count = 0, target = 100))
                            dao.insertTasbihItem(TasbihItemEntity(title = "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ", count = 0, target = 100))
                            dao.insertTasbihItem(TasbihItemEntity(title = "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ", count = 0, target = 100))
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
