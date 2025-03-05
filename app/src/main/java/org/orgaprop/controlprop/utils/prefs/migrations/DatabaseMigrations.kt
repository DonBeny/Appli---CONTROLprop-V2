package org.orgaprop.controlprop.utils.prefs.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.orgaprop.controlprop.utils.prefs.ConfigPrefDatabase

object DatabaseMigrations {

    // Migration de la version 1 à la version 2
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS ${ConfigPrefDatabase.PREF_TABLE_NAME} (
                    ${ConfigPrefDatabase.PREF_COL_ID_NAME} INTEGER PRIMARY KEY NOT NULL,
                    ${ConfigPrefDatabase.PREF_COL_PARAM_NAME} TEXT UNIQUE,
                    ${ConfigPrefDatabase.PREF_COL_VALUE_NAME} TEXT
                )
                """.trimIndent()
            )
        }
    }

    // Migration de la version 2 à la version 3
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS ${ConfigPrefDatabase.STORAGE_TABLE_NAME} (
                    ${ConfigPrefDatabase.STORAGE_COL_ID} INTEGER PRIMARY KEY NOT NULL,
                    ${ConfigPrefDatabase.STORAGE_COL_RESID} INTEGER UNIQUE NOT NULL,
                    ${ConfigPrefDatabase.STORAGE_COL_DATE} INTEGER NOT NULL,
                    ${ConfigPrefDatabase.STORAGE_COL_CONFIG} TEXT NOT NULL,
                    ${ConfigPrefDatabase.STORAGE_COL_TYPE_CTRL} TEXT NOT NULL,
                    ${ConfigPrefDatabase.STORAGE_COL_CTRL_TYPE} TEXT NOT NULL,
                    ${ConfigPrefDatabase.STORAGE_COL_CTRL_CTRL} TEXT NOT NULL,
                    ${ConfigPrefDatabase.STORAGE_COL_CTRL_SIG1} TEXT NOT NULL,
                    ${ConfigPrefDatabase.STORAGE_COL_CTRL_SIG2} TEXT NOT NULL,
                    ${ConfigPrefDatabase.STORAGE_COL_CTRL_SIG} TEXT NOT NULL,
                    ${ConfigPrefDatabase.STORAGE_COL_PLAN_END} INTEGER NOT NULL,
                    ${ConfigPrefDatabase.STORAGE_COL_PLAN_CONTENT} TEXT NOT NULL,
                    ${ConfigPrefDatabase.STORAGE_COL_PLAN_VALIDATE} INTEGER NOT NULL,
                    ${ConfigPrefDatabase.STORAGE_COL_SEND_DEST} TEXT NOT NULL,
                    ${ConfigPrefDatabase.STORAGE_COL_SEND_ID_PLAN} INTEGER NOT NULL,
                    ${ConfigPrefDatabase.STORAGE_COL_SEND_DATE_CTRL} INTEGER NOT NULL,
                    ${ConfigPrefDatabase.STORAGE_COL_SEND_TYPE_CTRL} TEXT NOT NULL,
                    ${ConfigPrefDatabase.STORAGE_COL_SEND_SRC} TEXT NOT NULL
                )
                """.trimIndent()
            )
        }
    }

}
