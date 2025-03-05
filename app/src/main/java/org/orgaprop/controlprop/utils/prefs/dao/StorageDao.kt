package org.orgaprop.controlprop.utils.prefs.dao

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.orgaprop.controlprop.utils.prefs.models.Storage

@Dao
interface StorageDao {

    @Query(DatabaseQueries.SELECT_ALL_STORAGE)
    fun getAllStorage(): Flow<List<Storage>>

    @Query(DatabaseQueries.SELECT_STORAGE_BY_ID)
    fun getStorage(storageId: Int): Flow<Storage>

    @Query(DatabaseQueries.SELECT_STORAGE_BY_RSD)
    fun getStorageRsd(storageRsd: Int): Flow<Storage>

    @Query(DatabaseQueries.SELECT_ALL_STORAGE)
    fun getAllStorageWithCursor(): Cursor

    @Query(DatabaseQueries.SELECT_STORAGE_BY_ID)
    fun getStorageWithCursor(storageId: Int): Cursor

    @Query(DatabaseQueries.SELECT_STORAGE_BY_RSD)
    fun getStorageRsdWithCursor(storageRsd: Int): Cursor

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStorage(storage: Storage): Long

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun updateStorage(storage: Storage): Int

    @Query(DatabaseQueries.DELETE_STORAGE_BY_ID)
    suspend fun deleteStorageById(storageId: Long): Int

    @Query(DatabaseQueries.DELETE_STORAGE_BY_RSD)
    suspend fun deleteStorageByRsd(storageRsd: Long): Int

    @Query(DatabaseQueries.DELETE_ALL_STORAGE)
    suspend fun deleteAllStorage(): Int

}
