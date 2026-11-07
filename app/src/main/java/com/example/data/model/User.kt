package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing administrative user accounts with salt, password hash,
 * and Two-Factor Authentication (2FA) / Biometric flags.
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val salt: String,
    val passwordHash: String,
    val twoFactorPin: String? = null,
    val isTwoFactorEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
