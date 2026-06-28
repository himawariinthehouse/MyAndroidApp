package com.example.myandroidapp.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface GaodeKeyDao {
    @Upsert
    suspend fun insertOrUpdate(entity: GaodeKeyEntity)

    @Query("SELECT * FROM gaode_key WHERE id = 1 LIMIT 1")
    suspend fun getGaodeKey(): GaodeKeyEntity?
}
