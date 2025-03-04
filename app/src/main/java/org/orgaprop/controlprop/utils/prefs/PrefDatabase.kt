package org.orgaprop.controlprop.databases

import android.content.ContentValues
import android.content.Context

import androidx.room.Database
import androidx.room.OnConflictStrategy
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import org.orgaprop.controlprop.utils.prefs.models.Pref
import org.orgaprop.controlprop.utils.prefs.models.Contact
import org.orgaprop.controlprop.utils.prefs.models.Storage

@Database(
    entities = [Pref::class, Contact::class, Storage::class],
    version = 3,
    exportSchema = false
)
abstract class PrefDatabase : RoomDatabase() {

    companion object {
        private const val TAG = "PrefDatabase"

        // Noms des tables et colonnes
        const val PREF_TABLE_NAME = "Pref"
        const val PREF_COL_ID_NAME = "id"
        const val PREF_COL_ID_NUM = 0
        const val PREF_COL_PARAM_NAME = "param"
        const val PREF_COL_PARAM_NUM = 1
        const val PREF_COL_VALUE_NAME = "value"
        const val PREF_COL_VALUE_NUM = 2

        const val PREF_ROW_ID_MBR = "id_mbr"
        const val PREF_ROW_ID_MBR_NUM = "1"
        const val PREF_ROW_ADR_MAC = "adr_mac"
        const val PREF_ROW_ADR_MAC_NUM = "2"
        const val PREF_ROW_AGENCY = "agc"
        const val PREF_ROW_AGENCY_NUM = "3"
        const val PREF_ROW_GROUP = "grp"
        const val PREF_ROW_GROUP_NUM = "4"
        const val PREF_ROW_RESIDENCE = "rsd"
        const val PREF_ROW_RESIDENCE_NUM = "5"

        // Migrations
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS $PREF_TABLE_NAME (
                        $PREF_COL_ID_NAME INTEGER PRIMARY KEY NOT NULL,
                        $PREF_COL_PARAM_NAME TEXT UNIQUE,
                        $PREF_COL_VALUE_NAME TEXT
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS Storage (
                        id INTEGER PRIMARY KEY NOT NULL,
                        resid INTEGER UNIQUE NOT NULL,
                        date INTEGER NOT NULL,
                        config TEXT NOT NULL,
                        typeCtrl TEXT NOT NULL,
                        ctrl_type TEXT NOT NULL,
                        ctrl_ctrl TEXT NOT NULL,
                        ctrl_sig1 TEXT NOT NULL,
                        ctrl_sig2 TEXT NOT NULL,
                        ctrl_sig TEXT NOT NULL,
                        plan_end INTEGER NOT NULL,
                        plan_content TEXT NOT NULL,
                        plan_validate INTEGER NOT NULL,
                        send_dest TEXT NOT NULL,
                        send_idPlan INTEGER NOT NULL,
                        send_dateCtrl INTEGER NOT NULL,
                        send_typeCtrl TEXT NOT NULL,
                        send_src TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        // Singleton
        @Volatile
        private var INSTANCE: PrefDatabase? = null

        fun getInstance(context: Context): PrefDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PrefDatabase::class.java,
                    "pref2.db"
                )
                    .addCallback(prepopulateDatabase())
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // Pré-remplissage de la base de données
        private fun prepopulateDatabase(): Callback {
            return object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)

                    val contentValues = ContentValues().apply {
                        put(PREF_COL_ID_NAME, PREF_ROW_ID_MBR_NUM.toLong())
                        put(PREF_COL_PARAM_NAME, PREF_ROW_ID_MBR)
                        put(PREF_COL_VALUE_NAME, "new")
                    }
                    db.insert(PREF_TABLE_NAME, OnConflictStrategy.IGNORE, contentValues)

                    contentValues.clear()
                    contentValues.put(PREF_COL_ID_NAME, PREF_ROW_ADR_MAC_NUM.toLong())
                    contentValues.put(PREF_COL_PARAM_NAME, PREF_ROW_ADR_MAC)
                    contentValues.put(PREF_COL_VALUE_NAME, "new")
                    db.insert(PREF_TABLE_NAME, OnConflictStrategy.IGNORE, contentValues)

                    contentValues.clear()
                    contentValues.put(PREF_COL_ID_NAME, PREF_ROW_AGENCY_NUM.toLong())
                    contentValues.put(PREF_COL_PARAM_NAME, PREF_ROW_AGENCY)
                    contentValues.put(PREF_COL_VALUE_NAME, "")
                    db.insert(PREF_TABLE_NAME, OnConflictStrategy.IGNORE, contentValues)

                    contentValues.clear()
                    contentValues.put(PREF_COL_ID_NAME, PREF_ROW_GROUP_NUM.toLong())
                    contentValues.put(PREF_COL_PARAM_NAME, PREF_ROW_GROUP)
                    contentValues.put(PREF_COL_VALUE_NAME, "")
                    db.insert(PREF_TABLE_NAME, OnConflictStrategy.IGNORE, contentValues)

                    contentValues.clear()
                    contentValues.put(PREF_COL_ID_NAME, PREF_ROW_RESIDENCE_NUM.toLong())
                    contentValues.put(PREF_COL_PARAM_NAME, PREF_ROW_RESIDENCE)
                    contentValues.put(PREF_COL_VALUE_NAME, "")
                    db.insert(PREF_TABLE_NAME, OnConflictStrategy.IGNORE, contentValues)
                }
            }
        }
    }

    // DAO
    abstract fun mPrefDao(): PrefDao
    abstract fun mContactDao(): ContactDao
    abstract fun mStorageDao(): StorageDao

}
