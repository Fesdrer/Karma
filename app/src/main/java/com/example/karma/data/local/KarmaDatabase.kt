package com.example.karma.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.karma.data.local.converter.Converters
import com.example.karma.data.local.dao.HistoryEntryDao
import com.example.karma.data.local.dao.KarmaSettingsDao
import com.example.karma.data.local.entity.HistoryEntryEntity
import com.example.karma.data.local.entity.KarmaSettingsEntity

@Database(
    entities = [HistoryEntryEntity::class, KarmaSettingsEntity::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class KarmaDatabase : RoomDatabase() {

    abstract fun historyEntryDao(): HistoryEntryDao
    abstract fun karmaSettingsDao(): KarmaSettingsDao

    companion object {
        private const val DB_NAME = "karma_database"

        @Volatile
        private var INSTANCE: KarmaDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 左：分数区域
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN scoreAxisFontSize REAL NOT NULL DEFAULT 22.0")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN scoreAxisRange REAL NOT NULL DEFAULT 6.0")

                // 中：刻度区域
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN axisLabelColor INTEGER NOT NULL DEFAULT -2130706433")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN axisTickThickness REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN axisLabelFontSize REAL NOT NULL DEFAULT 19.0")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN axisDisplayRange REAL NOT NULL DEFAULT 100.0")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN showNearbyTicks INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN nearbyTickRange REAL NOT NULL DEFAULT 10.0")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN axisQuarterValue REAL NOT NULL DEFAULT 15.0")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN rankColors TEXT NOT NULL DEFAULT '[4278212095,4278220799,4278229503,4278238207,4278246826,4282703172,4292717056,4294923520,4294901760]'")

                // 右：历史记录
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN historyLineThickness REAL NOT NULL DEFAULT 2.0")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN historyDotRadius REAL NOT NULL DEFAULT 3.5")

                // 业力衰减
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN decayEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN decayHour INTEGER NOT NULL DEFAULT 23")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN decayMinute INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN lastDecayDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN rankDecayAmounts TEXT NOT NULL DEFAULT '[2.0,2.0,2.0,2.0,2.0,3.0,3.0,3.0,3.0]'")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN scoreAxisRangeMin REAL NOT NULL DEFAULT -6.0")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN scoreAxisRangeMax REAL NOT NULL DEFAULT 6.0")
            }
        }

        fun getInstance(context: Context): KarmaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KarmaDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
