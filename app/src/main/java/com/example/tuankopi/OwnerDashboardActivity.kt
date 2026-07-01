package com.example.tuankopi

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.tuankopi.databinding.ActivityOwnerDashboardBinding
import com.google.firebase.auth.FirebaseAuth

class OwnerDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOwnerDashboardBinding
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOwnerDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = FirebaseAuth.getInstance()

        // Atur judul action bar utama dashboard
        supportActionBar?.title = "Tuan Kopi - Owner"

        // Setel Fragment yang pertama kali muncul saat aplikasi dibuka (Dashboard/Home)
        if (savedInstanceState == null) {
            gantiFragment(HomeFragment())
        }

        // Logika Klik Penukaran Menu Bottom Navigation Bar
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    gantiFragment(HomeFragment())
                    true
                }
                R.id.navigation_data -> {
                    gantiFragment(DataMasterFragment())
                    true
                }
                R.id.navigation_logistik -> {
                    gantiFragment(LogistikFragment())
                    true
                }
                R.id.navigation_audit -> {
                    gantiFragment(AuditProfitFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun gantiFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    // ────────────────────────────────────────────────────────────────────────
    // LOGIKA OPERASI LOGOUT (OPTIONS MENU)
    // ────────────────────────────────────────────────────────────────────────

    // Inflate item menu logout ke Toolbar atas
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_owner_dashboard, menu)
        return true
    }

    // Handle aksi klik pada item menu logout
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                tampilkanDialogKonfirmasiLogout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun tampilkanDialogKonfirmasiLogout() {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Keluar")
            .setMessage("Apakah Anda yakin ingin keluar dari akun Owner?")
            .setPositiveButton("Logout") { _, _ -> // Mengubah 'dialog, _' menjadi '_, _'
                // 1. Putus sesi Firebase Authentication secara total
                mAuth.signOut()

                Toast.makeText(this, "Berhasil keluar dari sistem", Toast.LENGTH_SHORT).show()

                // 2. Tendang balik ke halaman LoginActivity
                val intent = Intent(this, LoginActivity::class.java)

                // Bersihkan tumpukan activity (Backstack) agar user tidak bisa klik tombol back untuk kembali
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }
}