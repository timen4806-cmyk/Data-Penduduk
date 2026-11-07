package com.example.security

import java.security.SecureRandom

object TwoFactorManager {
    /**
     * Generates a 6-digit verification code.
     */
    fun generateOtpCode(): String {
        val random = SecureRandom()
        val code = 100000 + random.nextInt(900000)
        return code.toString()
    }

    /**
     * Validates whether candidate 2FA PIN matches the configured secret/PIN.
     */
    fun verifyPin(candidate: String, storedPin: String?): Boolean {
        if (storedPin.isNullOrBlank()) return false
        return candidate.trim() == storedPin.trim()
    }

    /**
     * Validates 6-digit numeric format.
     */
    fun isValidFormat(code: String): Boolean {
        return code.length == 6 && code.all { it.isDigit() }
    }
}
