package com.example.tuankopi

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tuankopi.databinding.ActivityManageRiderBinding
import com.google.firebase.firestore.FirebaseFirestore

class ManageRiderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageRiderBinding
    private lateinit var mFirestore: FirebaseFirestore
    private lateinit var adapterRider: RiderAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageRiderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Mengaktifkan Tombol Panah Kembali ke Dashboard Owner
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Kelola Operasional Rider"

        mFirestore = FirebaseFirestore.getInstance()

        setupRecyclerView()

        // Tombol `+` untuk mendaftarkan Rider baru
        binding.fabAddRider.setOnClickListener {
            startActivity(Intent(this, AddRiderActivity::class.java))
        }

        muatDataRiderDariFirestore()
    }

    private fun setupRecyclerView() {
        adapterRider = RiderAdapter(
            listRider = emptyList(),
            onStatusChanged = { uid, statusBaru ->
                ubahStatusAkunRider(uid, statusBaru)
            },
            onItemClick = { rider ->
                val intent = Intent(this, DetailRiderActivity::class.java).apply {
                    putExtra("RIDER_UID", rider.uid)
                    putExtra("RIDER_NAMA", rider.nama)
                    putExtra("RIDER_EMAIL", rider.email)
                    putExtra("RIDER_NOHP", rider.no_hp)
                }
                startActivity(intent)
            }
        )
        binding.rvRider.layoutManager = LinearLayoutManager(this)
        binding.rvRider.adapter = adapterRider
    }

    private fun muatDataRiderDariFirestore() {
        mFirestore.collection("users")
            .whereEqualTo("role", "rider")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Gagal mengambil data: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.toObjects(Rider::class.java)
                    adapterRider.updateData(list)
                }
            }
    }

    private fun ubahStatusAkunRider(uid: String, statusBaru: Boolean) {
        mFirestore.collection("users").document(uid)
            .update("status_akun", statusBaru)
            .addOnSuccessListener {
                val pesan = if (statusBaru) "Akun diaktifkan" else "Akun dinonaktifkan"
                Toast.makeText(this, pesan, Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal memperbarui status: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    // 2. Fungsi Logika: Menangani ketika tombol panah kembali di bar atas diklik
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed() // Menutup halaman ini dan kembali ke OwnerDashboardActivity
        return true
    }
}