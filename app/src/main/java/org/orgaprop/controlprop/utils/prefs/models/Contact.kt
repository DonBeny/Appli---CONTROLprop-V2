package org.orgaprop.controlprop.utils.prefs.models

import android.content.ContentValues
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import org.orgaprop.controlprop.utils.prefs.ConfigPrefDatabase

@Entity(tableName = ConfigPrefDatabase.CONTACT_TABLE_NAME)
data class Contact(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ConfigPrefDatabase.CONTACT_COL_ID)
    var id: Int = 0,

    @ColumnInfo(name = ConfigPrefDatabase.CONTACT_COL_ADR)
    var address: String
) {
    @Ignore
    constructor() : this(0, "")

    fun isValid(): Boolean {
        return address.isNotBlank()
    }

    companion object {
        fun fromContentValues(values: ContentValues): Contact {
            val contact = Contact()

            if (values.containsKey("id")) {
                contact.id = values.getAsInteger("id")
            }
            if (values.containsKey("address")) {
                contact.address = values.getAsString("address") ?: ""
            }
            if (values.containsKey("mail")) {
                contact.address = values.getAsString("mail") ?: ""
            }
            if (values.containsKey("courriel")) {
                contact.address = values.getAsString("courriel") ?: ""
            }
            if (values.containsKey("contact")) {
                contact.address = values.getAsString("contact") ?: ""
            }

            return contact
        }
    }
}