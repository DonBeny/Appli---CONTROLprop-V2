package org.orgaprop.controlprop.utils.prefs.repository

import android.database.Cursor
import kotlinx.coroutines.flow.Flow
import org.orgaprop.controlprop.utils.prefs.dao.ContactDao
import org.orgaprop.controlprop.utils.prefs.models.Contact
import org.orgaprop.controlprop.utils.types.Result

class ContactRepository(private val contactDao: ContactDao) {

    fun getAllContacts(): Flow<List<Contact>> {
        return contactDao.getAllContacts()
    }

    fun getContact(address: String): Flow<Contact> {
        return contactDao.getContact(address)
    }

    fun getAllContactsWithCursor(): Cursor {
        return contactDao.getAllContactsWithCursor()
    }

    fun getContactWithCursor(address: String): Cursor {
        return contactDao.getContactWithCursor(address)
    }

    suspend fun insertContact(contact: Contact): Result<Long> {
        return if (contact.isValid()) {
            try {
                Result.Success(contactDao.insertContact(contact))
            } catch (e: Exception) {
                Result.Failure(e)
            }
        } else {
            Result.Failure(IllegalArgumentException("Invalid Contact data"))
        }
    }

    suspend fun updateContact(contact: Contact): Result<Int> {
        return if (contact.isValid()) {
            try {
                Result.Success(contactDao.updateContact(contact))
            } catch (e: Exception) {
                Result.Failure(e)
            }
        } else {
            Result.Failure(IllegalArgumentException("Invalid Contact data"))
        }
    }

    suspend fun deleteContact(contactId: Long): Result<Int> {
        return try {
            Result.Success(contactDao.deleteContact(contactId))
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

}
