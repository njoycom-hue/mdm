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
    version = 2,
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
                )
                    // 이 DB는 업로드 대기 큐일 뿐 사용자 데이터가 아니라서(업로드 실패분은
                    // 다음 수집 주기에 다시 채워짐) 스키마 변경 시 마이그레이션 대신 재생성해도 무방하다.
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
