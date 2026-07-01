package com.example.tuankopi

data class Rider(
    val uid: String = "",
    val nama: String = "",
    val email: String = "",
    val no_hp: String = "",
    val role: String = "rider",
    @field:JvmField val status_akun: Boolean = true
)