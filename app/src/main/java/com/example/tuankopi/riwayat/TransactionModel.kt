package com.example.tuankopi.riwayat

import com.google.firebase.Timestamp

data class TransactionModel(
    val order_id: String = "",
    val id_rider: String = "",
    val nama_rider: String = "",
    val waktu_transaksi: Timestamp? = null,
    val metode_pembayaran: String = "",
    val total_harga: Long = 0L,
    val status_pembayaran: String = "",
    val items: List<TransactionItem> = listOf()
)

data class TransactionItem(
    val id_produk: String = "",
    val nama_produk: String = "",
    val qty: Int = 0,
    val harga_satuan: Long = 0L
)