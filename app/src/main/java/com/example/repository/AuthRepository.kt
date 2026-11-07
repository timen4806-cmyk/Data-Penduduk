package com.example.repository

import com.example.data.dao.UserDao
import com.example.data.model.User
import com.example.security.CryptoManager
import kotlinx.coroutines.flow.Flow

sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class RequiresTwoFactor(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthRepository(private val userDao: UserDao) {

    suspend fun hasAdmin(): Boolean {
        return userDao.getUserCount() > 0
    }

    fun hasAdminFlow(): Flow<Int> = userDao.getUserCountFlow()

    suspend fun getAdminUser(): User? {
        return userDao.getFirstUser()
    }

    suspend fun createAdmin(username: String, password: String): Boolean {
        val trimmedUsername = username.trim()
        if (trimmedUsername.isBlank() || password.length < 8) return false

        val (saltHex, passwordHashHex) = CryptoManager.hashPassword(password)
        val user = User(
            username = trimmedUsername,
            salt = saltHex,
            passwordHash = passwordHashHex,
            isTwoFactorEnabled = false,
            isBiometricEnabled = false
        )
        return try {
            userDao.insertUser(user) > 0
        } catch (e: Exception) {
            false
        }
    }

    suspend fun authenticate(username: String, password: String): AuthResult {
        val user = userDao.getUserByUsername(username.trim())
            ?: return AuthResult.Error("Username atau password salah.")

        val isPasswordValid = CryptoManager.verifyPassword(
            password = password,
            saltHex = user.salt,
            storedHashHex = user.passwordHash
        )

        if (!isPasswordValid) {
            return AuthResult.Error("Username atau password salah.")
        }

        return if (user.isTwoFactorEnabled && !user.twoFactorPin.isNullOrBlank()) {
            AuthResult.RequiresTwoFactor(user)
        } else {
            AuthResult.Success(user)
        }
    }

    suspend fun verifyTwoFactorPin(user: User, pin: String): Boolean {
        if (!user.isTwoFactorEnabled || user.twoFactorPin.isNullOrBlank()) return true
        return user.twoFactorPin.trim() == pin.trim()
    }

    suspend fun updateTwoFactorSettings(user: User, enabled: Boolean, pin: String?): Boolean {
        val updated = user.copy(
            isTwoFactorEnabled = enabled,
            twoFactorPin = if (enabled) pin?.trim() else null
        )
        userDao.updateUser(updated)
        return true
    }

    suspend fun updateBiometricSettings(user: User, enabled: Boolean): Boolean {
        val updated = user.copy(isBiometricEnabled = enabled)
        userDao.updateUser(updated)
        return true
    }
}
