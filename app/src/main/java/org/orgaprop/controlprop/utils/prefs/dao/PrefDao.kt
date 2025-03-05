package org.orgaprop.controlprop.utils.prefs.dao

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.orgaprop.controlprop.utils.prefs.models.Pref

@Dao
interface PrefDao {

    @Query(DatabaseQueries.SELECT_PREF_BY_PARAM)
    fun getPrefFromParam(param: String): Flow<Pref>

    @Query(DatabaseQueries.SELECT_PREF_BY_ID)
    fun getPrefFromId(paramId: Long): Flow<Pref>

    @Query(DatabaseQueries.SELECT_PREF_BY_PARAM)
    fun getPrefFromParamWithCursor(param: String): Cursor

    @Query(DatabaseQueries.SELECT_PREF_BY_ID)
    fun getPrefFromIdWithCursor(paramId: Long): Cursor

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPref(pref: Pref): Long

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun updatePref(pref: Pref): Int

    @Query(DatabaseQueries.DELETE_PREF_BY_PARAM)
    suspend fun deletePref(param: String): Int

    @Query(DatabaseQueries.DELETE_PREF_BY_ID)
    suspend fun deletePref(paramId: Long): Int

}
