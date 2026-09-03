package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PhotoCacheEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PhotoDatabase : RoomDatabase() {

    abstract fun photoCacheDao(): PhotoCacheDao

    companion object {
        @Volatile
        private var INSTANCE: PhotoDatabase? = null

        fun getInstance(context: Context): PhotoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PhotoDatabase::class.java,
                    "photo_star_sorter.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
