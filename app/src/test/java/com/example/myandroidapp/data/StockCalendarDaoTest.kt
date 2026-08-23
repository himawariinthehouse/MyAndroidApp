package com.example.myandroidapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class StockCalendarDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: StockCalendarDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        dao = database.stockCalendarDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertAndReadStockCalendar() = runBlocking {
        val item = StockCalendarEntity(
            type = "新股",
            securityCode = "301689",
            name = "电科思仪",
            issueDate = "2026-08-28",
            market = "深交所创业板"
        )

        dao.upsertAll(listOf(item))
        var loaded = dao.getAll()
        assertEquals(1, loaded.size)
        assertEquals("电科思仪", loaded[0].name)

        dao.upsertAll(listOf(item.copy(name = "电科思仪更新")))
        loaded = dao.getAll()
        assertEquals(1, loaded.size)
        assertEquals("电科思仪更新", loaded[0].name)
    }
}
