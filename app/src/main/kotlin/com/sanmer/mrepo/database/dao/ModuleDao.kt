package com.sanmer.mrepo.database.dao

import androidx.room.*
import com.sanmer.mrepo.database.entity.LocalModuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModuleDao {
    @Query("SELECT * FROM local_module")
    fun getLocalAll(): List<LocalModuleEntity>

    @Query("SELECT * FROM local_module")
    fun getLocalAllAsFlow(): Flow<List<LocalModuleEntity>>

    @Query("SELECT COUNT(id) FROM local_module")
    fun getLocalCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocal(value: LocalModuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocal(list: List<LocalModuleEntity>)

    @Query("DELETE FROM local_module")
    suspend fun deleteLocalAll()
}