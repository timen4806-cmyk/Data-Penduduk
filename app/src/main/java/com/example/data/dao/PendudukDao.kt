package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Penduduk
import kotlinx.coroutines.flow.Flow

@Dao
interface PendudukDao {
    @Query("SELECT * FROM penduduk ORDER BY nama COLLATE NOCASE ASC")
    fun getAllResidentsFlow(): Flow<List<Penduduk>>

    @Query("SELECT * FROM penduduk ORDER BY nama COLLATE NOCASE ASC")
    suspend fun getAllResidentsList(): List<Penduduk>

    @Query("""
        SELECT * FROM penduduk 
        WHERE nik LIKE '%' || :query || '%' 
           OR nama LIKE '%' || :query || '%' 
           OR alamat LIKE '%' || :query || '%'
        ORDER BY nama COLLATE NOCASE ASC
    """)
    fun searchResidentsFlow(query: String): Flow<List<Penduduk>>

    @Query("SELECT * FROM penduduk WHERE id = :id LIMIT 1")
    suspend fun getResidentById(id: Long): Penduduk?

    @Query("SELECT * FROM penduduk WHERE nik = :nik LIMIT 1")
    suspend fun getResidentByNik(nik: String): Penduduk?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertResident(penduduk: Penduduk): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(residents: List<Penduduk>)

    @Update
    suspend fun updateResident(penduduk: Penduduk)

    @Delete
    suspend fun deleteResident(penduduk: Penduduk)

    @Query("DELETE FROM penduduk WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM penduduk")
    fun getCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM penduduk")
    suspend fun getCount(): Int

    @Query("SELECT * FROM penduduk WHERE syncStatus = :status")
    suspend fun getResidentsBySyncStatus(status: String): List<Penduduk>

    @Query("UPDATE penduduk SET syncStatus = :status WHERE id IN (:ids)")
    suspend fun updateSyncStatus(ids: List<Long>, status: String)
}
