package com.example.notificationlog.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        NotificationEntity::class,
        AppRuleEntity::class,
        KeywordFilterEntity::class,
        SettingEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun appRuleDao(): AppRuleDao
    abstract fun keywordFilterDao(): KeywordFilterDao
    abstract fun settingDao(): SettingDao

    companion object {
        private const val DB_NAME = "notification_log.db"

        /** 최초 생성 시 넣어 둘 기본 필터 (이름 to 키워드) */
        private val DEFAULT_FILTERS = listOf(
            "배송" to listOf("배송", "도착", "택배", "출발"),
            "결제" to listOf("결제", "승인", "카드", "이체"),
            "예약" to listOf("예약", "예매", "일정"),
            "인증" to listOf("인증번호", "OTP", "보안코드"),
        )

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // DB 가 처음 만들어질 때 한 번만 기본 필터를 넣는다.
                        DEFAULT_FILTERS.forEachIndexed { index, (name, keywords) ->
                            db.execSQL(
                                "INSERT INTO keyword_filters (name, keywords, targetPackages, enabled, createdAt) " +
                                    "VALUES (?, ?, '', 1, ?)",
                                arrayOf<Any?>(name, keywords.joinToString("\n"), index.toLong()),
                            )
                        }
                    }
                })
                .build()
    }
}
