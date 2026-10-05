package com.example.prayers.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerDao {

    @Query("SELECT * FROM prayer_logs WHERE dateString = :dateString LIMIT 1")
    fun getPrayerLogForDate(dateString: String): Flow<PrayerLogEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePrayerLog(log: PrayerLogEntity)

    @Query("SELECT * FROM tasbih_items ORDER BY id ASC")
    fun getAllTasbihItems(): Flow<List<TasbihItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasbihItem(item: TasbihItemEntity): Long

    @Update
    suspend fun updateTasbihItem(item: TasbihItemEntity)

    @Query("DELETE FROM tasbih_items WHERE id = :id")
    suspend fun deleteTasbihItem(id: Int)
}
