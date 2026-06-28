package com.example.myandroidapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PlaceDao {
    @Insert
    suspend fun insert(place: PlaceEntity)

    @Query("SELECT * FROM places ORDER BY id DESC")
    suspend fun getAllPlaces(): List<PlaceEntity>

    @Query("SELECT DISTINCT groupName FROM places ORDER BY groupName")
    suspend fun getAllGroupNames(): List<String>
}
