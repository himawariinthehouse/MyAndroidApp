package com.example.myandroidapp.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface StockCalendarDao {
    @Upsert
    suspend fun upsertAll(items: List<StockCalendarEntity>)

    @Query("SELECT * FROM stock_calendar ORDER BY issueDate ASC")
    suspend fun getAll(): List<StockCalendarEntity>

    @Query("SELECT COUNT(*) FROM stock_calendar")
    suspend fun count(): Int

    @Query("UPDATE stock_calendar SET calendarEventId = :eventId WHERE type = :type AND securityCode = :securityCode")
    suspend fun updateCalendarEventId(type: String, securityCode: String, eventId: Long)
}
