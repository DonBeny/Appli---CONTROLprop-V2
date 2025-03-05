package org.orgaprop.controlprop.utils.prefs.repository

import android.database.Cursor
import kotlinx.coroutines.flow.Flow
import org.orgaprop.controlprop.utils.prefs.dao.PrefDao
import org.orgaprop.controlprop.utils.prefs.models.Pref
import org.orgaprop.controlprop.utils.types.Result

class PrefRepository(private val prefDao: PrefDao) {

    fun getPrefFromParam(param: String): Flow<Pref> {
        return prefDao.getPrefFromParam(param)
    }

    fun getPrefFromId(paramId: Long): Flow<Pref> {
        return prefDao.getPrefFromId(paramId)
    }

    fun getPrefFromParamWithCursor(param: String): Cursor {
        return prefDao.getPrefFromParamWithCursor(param)
    }

    fun getPrefFromIdWithCursor(paramId: Long): Cursor {
        return prefDao.getPrefFromIdWithCursor(paramId)
    }

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

    suspend fun deletePref(param: String): Result<Int> {
        return try {
            Result.Success(prefDao.deletePref(param))
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    suspend fun deletePref(paramId: Long): Result<Int> {
        return try {
            Result.Success(prefDao.deletePref(paramId))
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

}
