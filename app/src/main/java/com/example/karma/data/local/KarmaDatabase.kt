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
    version = 10,
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

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN goodDeedPresets TEXT NOT NULL DEFAULT '[\"帮助他人\",\"早起早睡\",\"锻炼身体\",\"日行一善\",\"孝敬父母\"]'")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN badDeedPresets TEXT NOT NULL DEFAULT '[\"发脾气\",\"浪费粮食\",\"口出恶言\",\"懒惰拖延\",\"伤害他人\"]'")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN goodResultPresets TEXT NOT NULL DEFAULT '[]'")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN guideLineWidth REAL NOT NULL DEFAULT 6.0")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN guideLineColor INTEGER NOT NULL DEFAULT -10496")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN rankThresholds TEXT NOT NULL DEFAULT '[10.0,30.0,60.0,100.0,150.0,210.0,280.0,360.0]'")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN rankNames TEXT NOT NULL DEFAULT '[\"壹阶\",\"贰阶\",\"叁阶\",\"肆阶\",\"伍阶\",\"陆阶\",\"柒阶\",\"捌阶\",\"玖阶\"]'")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN luckEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN luckT REAL NOT NULL DEFAULT 7.0")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN luckB REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN luckW REAL NOT NULL DEFAULT 100.0")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN themeGradientBaseColor INTEGER NOT NULL DEFAULT 4279507235")  // 0xFF141923
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN themeGradientAccentColor INTEGER NOT NULL DEFAULT 4282132997") // 0xFF3C2A05
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN dotColor INTEGER NOT NULL DEFAULT -44462")  // 0xFFFF5252 红色
            }
        }

        fun getInstance(context: Context): KarmaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KarmaDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
