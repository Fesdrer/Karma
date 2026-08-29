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
    version = 24,
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
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN dotColor INTEGER NOT NULL DEFAULT -65536")  // 0xFFFF0000 纯红
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN dailyMustDoDeedNames TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN dailyMustDoDeedPenalties TEXT NOT NULL DEFAULT '[]'")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN dailyMustDoLastDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN dailyMustDoDoneSet TEXT NOT NULL DEFAULT '[]'")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN dailyMustDoDeeds TEXT NOT NULL DEFAULT '[]'")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN timerStatus TEXT NOT NULL DEFAULT 'IDLE'")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN timerStartElapsed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN timerResumeElapsed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN timerAccumulatedMs INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN timerSelectedScore REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN timerSelectedEvent TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN bets TEXT NOT NULL DEFAULT '[]'")
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 占卜每日次数限制
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN divinationLimitLow INTEGER NOT NULL DEFAULT 2")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN divinationLimitHigh INTEGER NOT NULL DEFAULT 3")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN divinationLimitBoundary INTEGER NOT NULL DEFAULT 5")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN divinationDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN divinationCount INTEGER NOT NULL DEFAULT 0")
                // 负数阶位
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN negativeRankNames TEXT NOT NULL DEFAULT '[\"微愆\",\"过失\",\"迷途\",\"堕落\",\"沉沦\",\"罪业\",\"空亡\",\"深渊\",\"无间\"]'")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN negativeRankThresholds TEXT NOT NULL DEFAULT '[-10.0,-20.0,-30.0,-40.0,-50.0,-60.0,-70.0,-80.0]'")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN negativeRankColors TEXT NOT NULL DEFAULT '[4283453520,4282795590,4282137660,4281479730,4280821800,4280163870,4279505940,4278848010,4278190080]'")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN negativeRankDivinationLimits TEXT NOT NULL DEFAULT '[0,0,0,0,0,0,0,0,0]'")
                // 每阶占卜次数（正阶）
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN rankDivinationLimits TEXT NOT NULL DEFAULT '[0,0,2,2,2,3,3,3,3]'")
            }
        }

        /**
         * v16 库存在两种历史状态（旧 APK 7 列版 / 完整 10 列版），
         * 统一补齐三个"每阶占卜/负阶颜色"列（缺哪列补哪列）。
         */
        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val columns = mutableSetOf<String>()
                db.query("PRAGMA table_info(karma_settings)").use { cursor ->
                    while (cursor.moveToNext()) {
                        columns.add(cursor.getString(1))
                    }
                }
                if ("negativeRankColors" !in columns) {
                    db.execSQL("ALTER TABLE karma_settings ADD COLUMN negativeRankColors TEXT NOT NULL DEFAULT '[4283453520,4282795590,4282137660,4281479730,4280821800,4280163870,4279505940,4278848010,4278190080]'")
                }
                if ("negativeRankDivinationLimits" !in columns) {
                    db.execSQL("ALTER TABLE karma_settings ADD COLUMN negativeRankDivinationLimits TEXT NOT NULL DEFAULT '[0,0,0,0,0,0,0,0,0]'")
                }
                if ("rankDivinationLimits" !in columns) {
                    db.execSQL("ALTER TABLE karma_settings ADD COLUMN rankDivinationLimits TEXT NOT NULL DEFAULT '[0,0,2,2,2,3,3,3,3]'")
                }
            }
        }

        /**
         * v18：负阶默认颜色从"全纯黑"更新为渐变（-1 级 (80,80,80) → -9 级 (0,0,0)）。
         * 只更新仍为默认纯黑列表的行（用户手动改过色的不受影响）。
         */
        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "UPDATE karma_settings SET negativeRankColors = " +
                        "'[4283453520,4282795590,4282137660,4281479730,4280821800,4280163870,4279505940,4278848010,4278190080]' " +
                        "WHERE negativeRankColors = '[4278190080,4278190080,4278190080,4278190080,4278190080,4278190080,4278190080,4278190080,4278190080]'"
                )
            }
        }

        /**
         * v19：事件默认分数（v3.13 新增）。善业/恶业/善果各一列，默认空列表，
         * 读取时经 withNormalizedEventScores() 与名称列表对齐（缺的按分类补 ±1）。
         */
        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN goodDeedDefaultScores TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN badDeedDefaultScores TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN goodResultDefaultScores TEXT NOT NULL DEFAULT '[]'")
            }
        }

        /**
         * v20：乘法功能（主页面左栏乘法按钮）。默认 1/3、2/3、1/4、1/2、3/4、1.5、2、2.5、3。
         */
        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN multiplierPresets TEXT NOT NULL DEFAULT '[0.33333334,0.6666667,0.25,0.5,0.75,1.5,2.0,2.5,3.0]'")
            }
        }

        /**
         * v21：闹钟功能。新表 alarms（照手机闹钟设计：时间、重复周几、事件名、铃声、震动、贪睡、开关）。
         */
        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS alarms (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "enabled INTEGER NOT NULL DEFAULT 1, " +
                        "hour INTEGER NOT NULL DEFAULT 7, " +
                        "minute INTEGER NOT NULL DEFAULT 30, " +
                        "repeatDays TEXT NOT NULL DEFAULT '[]', " +
                        "eventName TEXT NOT NULL DEFAULT '', " +
                        "ringtoneUri TEXT NOT NULL DEFAULT '', " +
                        "vibrate INTEGER NOT NULL DEFAULT 1, " +
                        "snoozeMinutes INTEGER NOT NULL DEFAULT 10)"
                )
            }
        }

        /**
         * v22：移除闹钟功能（v21 已建 alarms 表，这里删除；数据库版本只能升不能降）。
         */
        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS alarms")
            }
        }

        /**
         * v23：阶位自证功能。设置项（开关+特效颜色）+ 自证进行中状态字段。
         * 颜色默认值按 0xRRGGBB 计算：金 0xFFFFD700=4294956800、白 0xFFFFFFFF=4294967295、
         * 暗红 0xFF8B0000=4287299584、成功绿 0xFF69f0ae=4285132974、失败红 0xFFFF5252=4294922834。
         */
        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN proofEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN proofLineColor INTEGER NOT NULL DEFAULT 4294956800")      // 0xFFFFD700 金
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN proofGlowColor INTEGER NOT NULL DEFAULT 4294967295")      // 0xFFFFFFFF 白
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN proofCountdownBg INTEGER NOT NULL DEFAULT 4287299584")    // 0xFF8B0000 暗红
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN proofCountdownText INTEGER NOT NULL DEFAULT 4294967295")   // 0xFFFFFFFF 白
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN proofSuccessColor INTEGER NOT NULL DEFAULT 4285132974")    // 0xFF69f0ae 绿
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN proofFailColor INTEGER NOT NULL DEFAULT 4294922834")       // 0xFFFF5252 红
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN proofActive INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN proofStartTime INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN proofDurationMs INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN proofStartRankLevel INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN proofTargetLevel INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN proofGuardLevel INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN proofReward REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE karma_settings ADD COLUMN proofPenalty REAL NOT NULL DEFAULT 0.0")
            }
        }

        /**
         * v24：修正 v23 迁移中算错的默认颜色值（金线/倒计时背景/成功/失败色）。
         * 只更新仍等于错误默认值的行（用户手动改过的不受影响）。
         */
        private val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE karma_settings SET proofLineColor = 4294956800 WHERE proofLineColor = 4294955007")
                db.execSQL("UPDATE karma_settings SET proofCountdownBg = 4287299584 WHERE proofCountdownBg = 4278190080")
                db.execSQL("UPDATE karma_settings SET proofSuccessColor = 4285132974 WHERE proofSuccessColor = 4278229358")
                db.execSQL("UPDATE karma_settings SET proofFailColor = 4294922834 WHERE proofFailColor = 4294931026")
            }
        }

        fun getInstance(context: Context): KarmaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KarmaDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
