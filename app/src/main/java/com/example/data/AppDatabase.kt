package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [EqProfile::class, DeviceMapping::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eqDao(): EqDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE eq_profiles ADD COLUMN automatedGainControlEnabled INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE eq_profiles ADD COLUMN autoAttenuationEnabled INTEGER NOT NULL DEFAULT 1")
                } catch(e: Exception) {
                    e.printStackTrace()
                }
                try {
                    db.execSQL("ALTER TABLE eq_profiles ADD COLUMN manualAttenuationDb REAL NOT NULL DEFAULT 0.0")
                } catch(e: Exception) {
                    e.printStackTrace()
                }
                try {
                    db.execSQL("ALTER TABLE eq_profiles ADD COLUMN channelBalance REAL NOT NULL DEFAULT 0.0")
                } catch(e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE eq_profiles ADD COLUMN masterNormalizationEnabled INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE eq_profiles ADD COLUMN reverbIntensity REAL NOT NULL DEFAULT 0.0")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vivad_sound_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
