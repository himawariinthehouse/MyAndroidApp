package com.example.myandroidapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gaode_key")
data class GaodeKeyEntity(
    @PrimaryKey
    val id: Int = 1,
    val key: String
)
