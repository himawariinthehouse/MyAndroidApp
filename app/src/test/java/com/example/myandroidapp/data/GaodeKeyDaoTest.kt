package com.example.myandroidapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GaodeKeyDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: GaodeKeyDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        dao = database.gaodeKeyDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndReadGaodeKey() = runBlocking {
        val entity = GaodeKeyEntity(id = 1, key = "test-amap-key")

        dao.insertOrUpdate(entity)
        val loaded = dao.getGaodeKey()

        assertEquals("test-amap-key", loaded?.key)
    }
}
