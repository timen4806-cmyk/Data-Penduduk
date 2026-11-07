package com.example

import com.example.data.model.Penduduk
import com.example.security.CryptoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testPasswordHashingAndVerification() {
    val password = "adminPassword123"
    val (salt, hash) = CryptoManager.hashPassword(password)

    assertTrue(salt.isNotBlank())
    assertTrue(hash.isNotBlank())
    assertTrue(CryptoManager.verifyPassword(password, salt, hash))
    assertFalse(CryptoManager.verifyPassword("wrongPassword", salt, hash))
  }

  @Test
  fun testPendudukFullAddress() {
    val penduduk = Penduduk(
      nik = "3201012345678901",
      nama = "Budi Santoso",
      alamat = "Gang Mawar",
      rt = "01",
      rw = "02",
      kelurahan = "Bojong",
      kecamatan = "Cibinong"
    )
    val fullAddress = penduduk.getFullAddress()
    assertEquals("Gang Mawar, RT 01, RW 02, Kelurahan/Desa Bojong, Kecamatan Cibinong", fullAddress)
  }
}
