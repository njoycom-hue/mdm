package com.safecircle.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.safecircle.app.data.local.entities.PendingCallEvent
import com.safecircle.app.data.local.entities.PendingKeywordAlert
import com.safecircle.app.data.local.entities.PendingUsageEvent

@Database(
    entities = [PendingUsageEvent::class, PendingKeywordAlert::class, PendingCallEvent::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingEventDao(): PendingEventDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "safecircle.db"
                ).build().also { instance = it }
            }
    }
}
