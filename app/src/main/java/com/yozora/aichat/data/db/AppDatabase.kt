package com.yozora.aichat.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.ContentValues
import com.yozora.aichat.ui.chat.migrateSelectedApiKeyValue

@Database(
    entities = [
        ChatSessionEntity::class,
        GroupMemberEntity::class,
        MessageEntity::class,
        TtsAudioCacheEntity::class
    ],
    version = 16,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zora.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14,
                        MIGRATION_14_15,
                        MIGRATION_15_16
                    )
                    .build()
                    .also { instance = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tts_audio_cache` (
                        `id` TEXT NOT NULL,
                        `messageId` TEXT NOT NULL,
                        `sessionId` TEXT NOT NULL,
                        `sourceHash` TEXT NOT NULL,
                        `cleanedTextHash` TEXT NOT NULL,
                        `preparedTextHash` TEXT NOT NULL,
                        `provider` TEXT NOT NULL,
                        `voiceId` TEXT NOT NULL,
                        `modelId` TEXT NOT NULL,
                        `language` TEXT NOT NULL,
                        `audioFilePath` TEXT NOT NULL,
                        `characterCount` INTEGER NOT NULL,
                        `durationMs` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    `index_tts_audio_cache_messageId_sourceHash_preparedTextHash_provider_voiceId_modelId`
                    ON `tts_audio_cache` (`messageId`, `sourceHash`, `preparedTextHash`, `provider`, `voiceId`, `modelId`)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_tts_audio_cache_messageId_sourceHash_provider_voiceId_modelId`
                    ON `tts_audio_cache` (`messageId`, `sourceHash`, `provider`, `voiceId`, `modelId`)
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `sessions` ADD COLUMN `memoryEnabled` INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `sessions` ADD COLUMN `projectId` TEXT"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `sessions` ADD COLUMN `title` TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `headerAvatarUri` TEXT")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `headerAvatarScale` REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `headerAvatarOffsetX` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `headerAvatarOffsetY` REAL NOT NULL DEFAULT 0.0")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `sessions` ADD COLUMN `levelSystemEnabled` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE `sessions` ADD COLUMN `levelXp` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `sessions` ADD COLUMN `storyLore` TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `sessions` ADD COLUMN `archivedContext` TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE `sessions` ADD COLUMN `archivedMessageIdsJson` TEXT NOT NULL DEFAULT '[]'"
                )
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `sessions` ADD COLUMN `draft` TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `sessions` ADD COLUMN `isFavorite` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE `sessions` ADD COLUMN `pinnedAtMillis` INTEGER"
                )
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `bubbleGlassOverride` TEXT")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `messages` ADD COLUMN `deliveryStatus` TEXT NOT NULL DEFAULT 'Delivered'"
                )
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `groupResponseMode` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `directorApiKey` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `group_members` ADD COLUMN `apiKey` TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `headerAvatarRotation` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `headerAvatarTransformNormalized` INTEGER NOT NULL DEFAULT 0")
            }
        }

        internal val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                rewriteSelectedKeys(db, "sessions", "id", "directorApiKey")
                rewriteSelectedKeys(db, "group_members", "id", "apiKey")
            }
        }

        private fun rewriteSelectedKeys(
            db: SupportSQLiteDatabase,
            table: String,
            idColumn: String,
            keyColumn: String
        ) {
            db.query("SELECT `$idColumn`, `$keyColumn` FROM `$table`").use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(idColumn)
                val keyIndex = cursor.getColumnIndexOrThrow(keyColumn)
                while (cursor.moveToNext()) {
                    val id = cursor.getString(idIndex)
                    val oldValue = cursor.getString(keyIndex).orEmpty()
                    val migrated = migrateSelectedApiKeyValue(oldValue)
                    if (migrated != oldValue) {
                        db.update(
                            table,
                            android.database.sqlite.SQLiteDatabase.CONFLICT_NONE,
                            ContentValues().apply { put(keyColumn, migrated) },
                            "`$idColumn` = ?",
                            arrayOf(id)
                        )
                    }
                }
            }
        }
    }
}
