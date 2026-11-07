package com.example.repository

import com.example.data.dao.PendudukDao
import com.example.data.model.Penduduk
import com.example.security.CryptoManager
import kotlinx.coroutines.flow.Flow

sealed class SaveResult {
    data class Success(val residentId: Long) : SaveResult()
    data class Error(val message: String) : SaveResult()
}

class PendudukRepository(private val pendudukDao: PendudukDao) {

    fun getAllResidentsFlow(): Flow<List<Penduduk>> = pendudukDao.getAllResidentsFlow()

    fun searchResidentsFlow(query: String): Flow<List<Penduduk>> {
        return if (query.isBlank()) {
            pendudukDao.getAllResidentsFlow()
        } else {
            pendudukDao.searchResidentsFlow(query.trim())
        }
    }

    suspend fun getAllResidentsList(): List<Penduduk> = pendudukDao.getAllResidentsList()

    suspend fun getResidentById(id: Long): Penduduk? = pendudukDao.getResidentById(id)

    suspend fun getResidentCount(): Int = pendudukDao.getCount()

    fun getCountFlow(): Flow<Int> = pendudukDao.getCountFlow()

    suspend fun saveResident(penduduk: Penduduk): SaveResult {
        // Validation: 16-digit numeric NIK
        val trimmedNik = penduduk.nik.trim()
        if (trimmedNik.length != 16 || !trimmedNik.all { it.isDigit() }) {
            return SaveResult.Error("NIK harus terdiri dari 16 digit angka.")
        }

        // Validation: Nama wajib diisi
        val trimmedNama = penduduk.nama.trim()
        if (trimmedNama.isBlank()) {
            return SaveResult.Error("Nama lengkap wajib diisi.")
        }

        // Duplicate NIK check
        val existingWithNik = pendudukDao.getResidentByNik(trimmedNik)
        if (existingWithNik != null && existingWithNik.id != penduduk.id) {
            return SaveResult.Error("NIK sudah terdaftar pada data penduduk lain.")
        }

        // Generate encrypted snapshot of sensitive details
        val sensitiveDataString = "${trimmedNik}|${trimmedNama}|${penduduk.alamat}|${penduduk.agama}|${penduduk.statusPerkawinan}"
        val encryptedPayload = CryptoManager.encrypt(sensitiveDataString)

        val prepared = penduduk.copy(
            nik = trimmedNik,
            nama = trimmedNama,
            kewarganegaraan = if (penduduk.kewarganegaraan.isBlank()) "WNI" else penduduk.kewarganegaraan.trim(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = Penduduk.SYNC_STATUS_PENDING,
            encryptedPayload = encryptedPayload
        )

        return try {
            if (prepared.id == 0L) {
                val newId = pendudukDao.insertResident(prepared)
                SaveResult.Success(newId)
            } else {
                pendudukDao.updateResident(prepared)
                SaveResult.Success(prepared.id)
            }
        } catch (e: Exception) {
            SaveResult.Error("Gagal menyimpan data: ${e.localizedMessage}")
        }
    }

    suspend fun deleteResident(penduduk: Penduduk) {
        pendudukDao.deleteResident(penduduk)
    }

    suspend fun deleteResidentById(id: Long) {
        pendudukDao.deleteById(id)
    }

    suspend fun insertBulk(residents: List<Penduduk>) {
        pendudukDao.insertAll(residents)
    }

    suspend fun markAsSynced(ids: List<Long>) {
        pendudukDao.updateSyncStatus(ids, Penduduk.SYNC_STATUS_SYNCED)
    }
}
