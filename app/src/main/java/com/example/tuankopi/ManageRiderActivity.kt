package com.example.tuankopi

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tuankopi.databinding.ActivityManageRiderBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ManageRiderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageRiderBinding
    private lateinit var mFirestore: FirebaseFirestore
    private lateinit var mAuth: FirebaseAuth
    private lateinit var adapterRider: RiderAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageRiderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.customToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Kelola Operasional Rider"

        ViewCompat.setOnApplyWindowInsetsListener(binding.customToolbar) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, insets.top, 0, 0)
            windowInsets
        }

        mFirestore = FirebaseFirestore.getInstance()
        mAuth = FirebaseAuth.getInstance()

        setupRecyclerView()

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
                val pesan = if (statusBaru) "Account diaktifkan" else "Account dinonaktifkan"
                Toast.makeText(this, pesan, Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal memperbarui status: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_owner_dashboard, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> { tampilkanDialogKonfirmasiLogout(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun tampilkanDialogKonfirmasiLogout() {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Keluar")
            .setMessage("Apakah Anda yakin ingin keluar dari akun Owner?")
            .setPositiveButton("Logout") { _, _ ->
                mAuth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}