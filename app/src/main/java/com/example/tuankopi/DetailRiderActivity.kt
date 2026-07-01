package com.example.tuankopi

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.tuankopi.databinding.ActivityDetailRiderBinding

class DetailRiderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailRiderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailRiderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Mengaktifkan Tombol Panah Kembali di Toolbar Atas Android
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Detail Karyawan" // Mengubah judul bar atas

        // Tangkap data dari Intent
        val uid = intent.getStringExtra("RIDER_UID") ?: "-"
        val nama = intent.getStringExtra("RIDER_NAMA") ?: "-"
        val email = intent.getStringExtra("RIDER_EMAIL") ?: "-"
        val noHp = intent.getStringExtra("RIDER_NOHP") ?: "-"

        // Pasang data ke UI
        binding.tvDetailNama.text = "Nama: $nama"
        binding.tvDetailEmail.text = "Email: $email"
        binding.tvDetailNoHp.text = "No HP/WA: $noHp"
        binding.tvDetailUid.text = "UID: $uid"
    }

    // 2. Fungsi Logika: Jika tombol panah kembali di bar atas diklik, tutup halaman ini
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed() // Kembali ke halaman daftar rider
        return true
    }
}