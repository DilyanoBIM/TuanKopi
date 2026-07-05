package com.example.tuankopi

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tuankopi.databinding.ActivityAddProductBinding
import com.google.firebase.firestore.FirebaseFirestore

class AddProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddProductBinding
    private lateinit var mFirestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Di dalam onCreate(), ganti inisialisasi Action bar lama dengan:
        setSupportActionBar(binding.customToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Tambah Produk"

        mFirestore = FirebaseFirestore.getInstance()

        binding.btnSimpanProduk.setOnClickListener {
            val idProduk = binding.etIdProduk.text.toString().trim().uppercase()
            val namaProduk = binding.etNamaProduk.text.toString().trim()
            val hargaStr = binding.etHargaProduk.text.toString().trim()

            if (idProduk.isEmpty()) { binding.etIdProduk.error = "ID wajib diisi"; return@setOnClickListener }
            if (namaProduk.isEmpty()) { binding.etNamaProduk.error = "Nama wajib diisi"; return@setOnClickListener }
            if (hargaStr.isEmpty()) { binding.etHargaProduk.error = "Harga wajib diisi"; return@setOnClickListener }

            val hargaJual = hargaStr.toLong()

            simpanProdukKeFirestore(idProduk, namaProduk, hargaJual)
        }
    }

    private fun simpanProdukKeFirestore(id: String, nama: String, harga: Long) {
        val dataProduk = hashMapOf(
            "id_produk" to id,
            "nama_produk" to nama,
            "harga_jual" to harga,
            "foto_url" to "", // Kosongkan sementara sebelum integrasi cloud storage
            "status_accessible" to true,
            "status_tersedia" to true
        )

        // Simpan menggunakan ID buatan manual sebagai Document ID agar rapi
        mFirestore.collection("products").document(id).set(dataProduk)
            .addOnSuccessListener {
                Toast.makeText(this, "Produk $nama Berhasil Ditambahkan!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}