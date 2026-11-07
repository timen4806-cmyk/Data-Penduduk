package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing citizen resident data (Data Penduduk),
 * derived and extended from the reference DataWarga specification.
 */
@Entity(
    tableName = "penduduk",
    indices = [Index(value = ["nik"], unique = true)]
)
data class Penduduk(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nik: String,
    val nama: String,
    val tempatLahir: String = "",
    val tanggalLahir: String = "",
    val jenisKelamin: String = "",
    val alamat: String = "",
    val rt: String = "",
    val rw: String = "",
    val kelurahan: String = "",
    val kecamatan: String = "",
    val agama: String = "",
    val statusPerkawinan: String = "",
    val pekerjaan: String = "",
    val kewarganegaraan: String = "WNI",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = SYNC_STATUS_SYNCED,
    val encryptedPayload: String? = null
) {
    companion object {
        const val SYNC_STATUS_SYNCED = "SYNCED"
        const val SYNC_STATUS_PENDING = "PENDING_SYNC"
        const val SYNC_STATUS_LOCAL_ONLY = "LOCAL_ONLY"

        val GENDER_OPTIONS = listOf("Laki-laki", "Perempuan")
        val AGAMA_OPTIONS = listOf("Islam", "Kristen", "Katolik", "Hindu", "Buddha", "Khonghucu", "Penghayat / Lainnya")
        val STATUS_PERKAWINAN_OPTIONS = listOf("Belum Kawin", "Kawin", "Cerai Hidup", "Cerai Mati")
        val KEWARGANEGARAAN_OPTIONS = listOf("WNI", "WNA")

        fun formatFullAddress(
            alamat: String,
            rt: String,
            rw: String,
            kelurahan: String,
            kecamatan: String
        ): String {
            val parts = mutableListOf<String>()
            if (alamat.isNotBlank()) parts.add(alamat.trim())
            if (rt.isNotBlank()) parts.add("RT ${rt.trim()}")
            if (rw.isNotBlank()) parts.add("RW ${rw.trim()}")
            if (kelurahan.isNotBlank()) parts.add("Kelurahan/Desa ${kelurahan.trim()}")
            if (kecamatan.isNotBlank()) parts.add("Kecamatan ${kecamatan.trim()}")
            return parts.joinToString(", ")
        }
    }

    fun getFullAddress(): String {
        return formatFullAddress(alamat, rt, rw, kelurahan, kecamatan)
    }
}
