package org.orgaprop.controlprop.utils.prefs.models

import android.content.ContentValues
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import org.orgaprop.controlprop.utils.prefs.ConfigPrefDatabase

@Entity(tableName = ConfigPrefDatabase.PREF_TABLE_NAME)
data class Pref(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ConfigPrefDatabase.PREF_COL_ID_NAME)
    var id: Long = 0,

    @ColumnInfo(name = ConfigPrefDatabase.PREF_COL_PARAM_NAME)
    var param: String,

    @ColumnInfo(name = ConfigPrefDatabase.PREF_COL_VALUE_NAME)
    var value: String
) {
    @Ignore
    constructor() : this(0, "", "")

    fun isValid(): Boolean {
        return param.isNotBlank() && value.isNotBlank()
    }

    companion object {
        fun fromContentValues(values: ContentValues): Pref {
            val pref = Pref()

            if (values.containsKey("id")) {
                pref.id = values.getAsLong("id")
            }
            if (values.containsKey("param")) {
                pref.param = values.getAsString("param") ?: ""
            }
            if (values.containsKey("value")) {
                pref.value = values.getAsString("value") ?: ""
            }

            return pref
        }
    }

}
