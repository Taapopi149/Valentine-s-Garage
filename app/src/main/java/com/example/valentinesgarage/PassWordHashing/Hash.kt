package com.example.valentinesgarage.PassWordHashing

import java.security.MessageDigest

object PasswordUtils {

    fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(password: String, hashed: String): Boolean {
        // for simple hashing, just hash the input and compare
        return hashPassword(password) == hashed
    }
}