package com.example.tuankopi

data class Product(
    val id_produk: String = "",
    val nama_produk: String = "",
    val harga_jual: Long = 0,
    val foto_url: String = "",
    @field:JvmField val status_tersedia: Boolean = true
)