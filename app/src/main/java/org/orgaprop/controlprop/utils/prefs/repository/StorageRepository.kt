package org.orgaprop.controlprop.utils.prefs.repository

import android.database.Cursor
import kotlinx.coroutines.flow.Flow
import org.orgaprop.controlprop.utils.prefs.dao.StorageDao
import org.orgaprop.controlprop.utils.prefs.models.Storage
import org.orgaprop.controlprop.utils.types.Result

class StorageRepository(private val storageDao: StorageDao) {

    fun getAllStorage(): Flow<List<Storage>> {
        return storageDao.getAllStorage()
    }

    fun getStorage(storageId: Int): Flow<Storage> {
        return storageDao.getStorage(storageId)
    }

    fun getStorageRsd(storageRsd: Int): Flow<Storage> {
        return storageDao.getStorageRsd(storageRsd)
    }

    fun getAllStorageWithCursor(): Cursor {
        return storageDao.getAllStorageWithCursor()
    }

    fun getStorageWithCursor(storageId: Int): Cursor {
        return storageDao.getStorageWithCursor(storageId)
    }

    fun getStorageRsdWithCursor(storageRsd: Int): Cursor {
        return storageDao.getStorageRsdWithCursor(storageRsd)
    }

    suspend fun insertStorage(storage: Storage): Result<Long> {
        return if (storage.isValid()) {
            try {
                Result.Success(storageDao.insertStorage(storage))
            } catch (e: Exception) {
                Result.Failure(e)
            }
        } else {
            Result.Failure(IllegalArgumentException("Invalid Storage data"))
        }
    }

    suspend fun updateStorage(storage: Storage): Result<Int> {
        return if (storage.isValid()) {
            try {
                Result.Success(storageDao.updateStorage(storage))
            } catch (e: Exception) {
                Result.Failure(e)
            }
        } else {
            Result.Failure(IllegalArgumentException("Invalid Storage data"))
        }
    }

    suspend fun deleteStorageById(storageId: Long): Result<Int> {
        return try {
            Result.Success(storageDao.deleteStorageById(storageId))
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    suspend fun deleteStorageByRsd(storageRsd: Long): Result<Int> {
        return try {
            Result.Success(storageDao.deleteStorageByRsd(storageRsd))
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    suspend fun deleteAllStorage(): Result<Int> {
        return try {
            Result.Success(storageDao.deleteAllStorage())
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

}
