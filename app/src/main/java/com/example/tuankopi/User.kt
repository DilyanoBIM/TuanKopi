package com.example.tuankopi

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val nama: String = "",
    val email: String = "",
    val no_hp: String = "",
    val role: String = "",
    val status_akun: Boolean = true,
    val created_at: Timestamp? = null
)