package org.orgaprop.controlprop.utils.prefs.repository

import android.database.Cursor
import kotlinx.coroutines.flow.Flow
import org.orgaprop.controlprop.utils.prefs.dao.StorageDao
import org.orgaprop.controlprop.utils.prefs.models.Storage
import org.orgaprop.controlprop.utils.types.Result

class StorageRepository(private val storageDao: StorageDao) {

    // Récupérer tous les enregistrements de stockage (Flow)
    fun getAllStorage(): Flow<List<Storage>> {
        return storageDao.getAllStorage()
    }

    // Récupérer un enregistrement de stockage par son ID (Flow)
    fun getStorage(storageId: Int): Flow<Storage> {
        return storageDao.getStorage(storageId)
    }

    // Récupérer un enregistrement de stockage par son resid (Flow)
    fun getStorageRsd(storageRsd: Int): Flow<Storage> {
        return storageDao.getStorageRsd(storageRsd)
    }

    // Récupérer tous les enregistrements de stockage (Cursor)
    suspend fun getAllStorageWithCursor(): Cursor {
        return storageDao.getAllStorageWithCursor()
    }

    // Récupérer un enregistrement de stockage par son ID (Cursor)
    suspend fun getStorageWithCursor(storageId: Int): Cursor {
        return storageDao.getStorageWithCursor(storageId)
    }

    // Récupérer un enregistrement de stockage par son resid (Cursor)
    suspend fun getStorageRsdWithCursor(storageRsd: Int): Cursor {
        return storageDao.getStorageRsdWithCursor(storageRsd)
    }

    // Insérer un enregistrement de stockage
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

    // Mettre à jour un enregistrement de stockage
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

    // Supprimer un enregistrement de stockage par son ID
    suspend fun deleteStorageById(storageId: Long): Result<Int> {
        return try {
            Result.Success(storageDao.deleteStorageById(storageId))
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    // Supprimer un enregistrement de stockage par son resid
    suspend fun deleteStorageByRsd(storageRsd: Long): Result<Int> {
        return try {
            Result.Success(storageDao.deleteStorageByRsd(storageRsd))
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    // Supprimer tous les enregistrements de stockage
    suspend fun deleteAllStorage(): Result<Int> {
        return try {
            Result.Success(storageDao.deleteAllStorage())
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

}
