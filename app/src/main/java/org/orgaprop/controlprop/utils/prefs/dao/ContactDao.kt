package org.orgaprop.controlprop.utils.prefs.dao

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.orgaprop.controlprop.utils.prefs.models.Contact

@Dao
interface ContactDao {

    @Query(DatabaseQueries.SELECT_ALL_CONTACTS)
    fun getAllContacts(): Flow<List<Contact>>

    @Query(DatabaseQueries.SELECT_CONTACT_BY_ADDRESS)
    fun getContact(txt: String): Flow<Contact>

    @Query(DatabaseQueries.SELECT_ALL_CONTACTS)
    fun getAllContactsWithCursor(): Cursor

    @Query(DatabaseQueries.SELECT_CONTACT_BY_ADDRESS)
    fun getContactWithCursor(txt: String): Cursor

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact): Long

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun updateContact(contact: Contact): Int

    @Query(DatabaseQueries.DELETE_CONTACT_BY_ID)
    suspend fun deleteContact(addressId: Long): Int

}
