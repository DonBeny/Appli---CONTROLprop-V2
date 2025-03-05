package org.orgaprop.controlprop.utils.prefs.repository

import android.database.Cursor
import kotlinx.coroutines.flow.Flow
import org.orgaprop.controlprop.utils.prefs.dao.PrefDao
import org.orgaprop.controlprop.utils.prefs.models.Pref
import org.orgaprop.controlprop.utils.types.Result

class PrefRepository(private val prefDao: PrefDao) {

    // Récupérer une préférence par son paramètre (LiveData)
    fun getPrefFromParam(param: String): Flow<Pref> {
        return prefDao.getPrefFromParam(param)
    }

    // Récupérer une préférence par son ID (LiveData)
    fun getPrefFromId(paramId: Long): Flow<Pref> {
        return prefDao.getPrefFromId(paramId)
    }

    // Récupérer une préférence par son paramètre (Cursor)
    suspend fun getPrefFromParamWithCursor(param: String): Cursor {
        return prefDao.getPrefFromParamWithCursor(param)
    }

    // Récupérer une préférence par son ID (Cursor)
    suspend fun getPrefFromIdWithCursor(paramId: Long): Cursor {
        return prefDao.getPrefFromIdWithCursor(paramId)
    }

    // Insérer une préférence
    suspend fun insertPref(pref: Pref): Result<Long> {
        return if (pref.isValid()) {
            try {
                Result.Success(prefDao.insertPref(pref))
            } catch (e: Exception) {
                Result.Failure(e)
            }
        } else {
            Result.Failure(IllegalArgumentException("Invalid Pref data"))
        }
    }

    // Mettre à jour une préférence
    suspend fun updatePref(pref: Pref): Result<Int> {
        return if (pref.isValid()) {
            try {
                Result.Success(prefDao.updatePref(pref))
            } catch (e: Exception) {
                Result.Failure(e)
            }
        } else {
            Result.Failure(IllegalArgumentException("Invalid Pref data"))
        }
    }

    // Supprimer une préférence par son paramètre
    suspend fun deletePref(param: String): Result<Int> {
        return try {
            Result.Success(prefDao.deletePref(param))
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    // Supprimer une préférence par son ID
    suspend fun deletePref(paramId: Long): Result<Int> {
        return try {
            Result.Success(prefDao.deletePref(paramId))
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

}
