package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DecisionList::class, WheelOption::class], version = 1, exportSchema = false)
abstract class WheelDatabase : RoomDatabase() {
    abstract fun wheelDao(): WheelDao

    companion object {
        @Volatile
        private var INSTANCE: WheelDatabase? = null

        fun getDatabase(context: Context): WheelDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WheelDatabase::class.java,
                    "picker_wheel_database"
                )
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
