package com.example.tuankopi.riwayat

import com.google.firebase.Timestamp

/**
 * Mapping 1:1 ke struktur field koleksi Firestore "transactions".
 * Semua field diberi default agar aman dipakai dengan
 * DocumentSnapshot.toObject(Transaksi::class.java).
 */
data class Transaksi(
    val order_id: String = "",
    val id_rider: String = "",
    val nama_rider: String = "",
    val waktu_transaksi: Timestamp? = null,
    val metode_pembayaran: String = "",   // "TUNAI" atau "QRIS"
    val total_harga: Long = 0L,
    val nominal_diterima: Long = 0L,
    val nominal_kembalian: Long = 0L,
    val status_pembayaran: String = "",   // "PENDING" | "SUCCESS" | "FAILED"
    val items: List<Map<String, Any>> = emptyList()
) {
    /** Ringkasan item untuk ditampilkan di satu baris list, contoh: "Kopi Susu x2, Americano x1" */
    fun ringkasanItem(): String {
        if (items.isEmpty()) return "-"
        return items.joinToString(", ") { item ->
            val nama = item["nama_produk"] as? String ?: "Produk"
            val qtyRaw = item["qty"]
            val qty = when (qtyRaw) {
                is Long -> qtyRaw
                is Number -> qtyRaw.toLong()
                else -> 0L
            }
            "$nama x$qty"
        }
    }
}

/** Item dropdown untuk daftar Rider, diambil dari koleksi "users" where role == "rider" */
data class RiderOption(
    val uid: String,
    val nama: String
)