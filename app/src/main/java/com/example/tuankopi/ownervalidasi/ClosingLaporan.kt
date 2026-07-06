package com.example.tuankopi.ownervalidasi

import com.google.firebase.Timestamp

data class ClosingLaporan(
    val id_closing: String = "",
    val tanggal: String = "",
    val id_rider: String = "",
    val nama_rider: String = "",
    val waktu_closing_rider: Timestamp? = null,
    val status_validasi: String = "",
    val total_tunai_sistem: Long = 0L,
    val total_qris_sistem: Long = 0L,
    val total_omset_sistem: Long = 0L,
    val uang_tunai_fisik: Long = 0L,
    val nominal_selisih: Long = 0L,
    val catatan_owner: String = ""
)