package com.example.myandroidapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface GroupDao {
    @Insert
    suspend fun insert(group: GroupEntity)

    @Query("SELECT * FROM `groups` ORDER BY name")
    suspend fun getAllGroups(): List<GroupEntity>

    @Query("SELECT name FROM `groups` ORDER BY name")
    suspend fun getAllGroupNames(): List<String>
}
