package com.example.myandroidapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [GaodeKeyEntity::class, PlaceEntity::class, GroupEntity::class, StockCalendarEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun gaodeKeyDao(): GaodeKeyDao
    abstract fun placeDao(): PlaceDao
    abstract fun groupDao(): GroupDao
    abstract fun stockCalendarDao(): StockCalendarDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `stock_calendar` (" +
                            "`type` TEXT NOT NULL, " +
                            "`securityCode` TEXT NOT NULL, " +
                            "`name` TEXT NOT NULL, " +
                            "`issueDate` TEXT NOT NULL, " +
                            "`market` TEXT NOT NULL, " +
                            "PRIMARY KEY(`type`, `securityCode`))"
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
