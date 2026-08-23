package com.example.myandroidapp.data

import androidx.room.Entity

@Entity(
    tableName = "stock_calendar",
    primaryKeys = ["type", "securityCode"]
)
data class StockCalendarEntity(
    val type: String,
    val securityCode: String,
    val name: String,
    val issueDate: String,
    val market: String,
    val calendarEventId: Long? = null
)
