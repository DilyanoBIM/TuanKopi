package com.example.tuankopi

import com.google.firebase.Timestamp

data class StokGudang(
    val id_gudang: String = "",
    val tanggal: String = "",
    val id_produk: String = "",
    val nama_produk: String = "",
    val stok_masuk_awal: Long = 0L,
    val stok_tambahan: Long = 0L, // ◄ Field baru pelacak pasokan siang/sore
    val stok_dialokasikan: Long = 0L,
    val sisa_gudang: Long = 0L,
    val harga_jual: Long = 0L,
    val stok_total: Long = 0L,    // ◄ Hasil penjumlahan awal + tambahan
    val last_updated: Timestamp? = null
)