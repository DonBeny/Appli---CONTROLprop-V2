package org.orgaprop.controlprop.utils.prefs

import android.content.ContentValues
import android.content.Context
import androidx.room.Database
import androidx.room.OnConflictStrategy
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import org.orgaprop.controlprop.utils.prefs.models.Contact
import org.orgaprop.controlprop.utils.prefs.dao.ContactDao
import org.orgaprop.controlprop.utils.prefs.models.Pref
import org.orgaprop.controlprop.utils.prefs.dao.PrefDao
import org.orgaprop.controlprop.utils.prefs.models.Storage
import org.orgaprop.controlprop.utils.prefs.dao.StorageDao
import org.orgaprop.controlprop.utils.prefs.migrations.DatabaseMigrations

@Database(
    entities = [Pref::class, Contact::class, Storage::class],
    version = 3,
    exportSchema = false
)
abstract class PrefDatabase : RoomDatabase() {

    companion object {
        private const val TAG = "PrefDatabase"

        // Utilisation des constantes de configPrefDatabase
        private const val PREF_TABLE_NAME = configPrefDatabase.PREF_TABLE_NAME
        private const val PREF_COL_ID_NAME = configPrefDatabase.PREF_COL_ID_NAME
        private const val PREF_COL_PARAM_NAME = configPrefDatabase.PREF_COL_PARAM_NAME
        private const val PREF_COL_VALUE_NAME = configPrefDatabase.PREF_COL_VALUE_NAME

        private const val PREF_ROW_ID_MBR = configPrefDatabase.PREF_ROW_ID_MBR
        private const val PREF_ROW_ID_MBR_NUM = configPrefDatabase.PREF_ROW_ID_MBR_NUM
        private const val PREF_ROW_ADR_MAC = configPrefDatabase.PREF_ROW_ADR_MAC
        private const val PREF_ROW_ADR_MAC_NUM = configPrefDatabase.PREF_ROW_ADR_MAC_NUM
        private const val PREF_ROW_AGENCY = configPrefDatabase.PREF_ROW_AGENCY
        private const val PREF_ROW_AGENCY_NUM = configPrefDatabase.PREF_ROW_AGENCY_NUM
        private const val PREF_ROW_GROUP = configPrefDatabase.PREF_ROW_GROUP
        private const val PREF_ROW_GROUP_NUM = configPrefDatabase.PREF_ROW_GROUP_NUM
        private const val PREF_ROW_RESIDENCE = configPrefDatabase.PREF_ROW_RESIDENCE
        private const val PREF_ROW_RESIDENCE_NUM = configPrefDatabase.PREF_ROW_RESIDENCE_NUM

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
                    .addMigrations(
                        DatabaseMigrations.MIGRATION_1_2,
                        DatabaseMigrations.MIGRATION_2_3
                    )
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
