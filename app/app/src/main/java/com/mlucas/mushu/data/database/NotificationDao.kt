package com.mlucas.mushu.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mlucas.mushu.data.entities.NotificationEntity

@Dao
interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC LIMIT 5")
    fun getLastNotifications(): LiveData<List<NotificationEntity>>

    @Query("DELETE FROM notifications WHERE id IN (SELECT id FROM notifications ORDER BY timestamp ASC LIMIT 1)")
    suspend fun deleteOldestNotification()

    @Query("DELETE FROM notifications WHERE id NOT IN (SELECT id FROM notifications ORDER BY timestamp DESC LIMIT 5)")
    suspend fun removeExcessNotifications()

    @Query("DELETE FROM notifications")
    suspend fun deleteAllNotifications()

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun getCount(): Int
}