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
import androidx.fragment.app.Fragment
import com.example.tuankopi.AktivitasRider.RiderAktivitasFragment
import com.example.tuankopi.databinding.ActivityRiderDashboardBinding
import com.google.firebase.auth.FirebaseAuth

class RiderDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRiderDashboardBinding
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRiderDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = FirebaseAuth.getInstance()

        // 1. Setup Toolbar Custom untuk menggantikan Action Bar bawaan
        setSupportActionBar(binding.riderToolbar)
        supportActionBar?.title = "Tuan Kopi - Rider"

        // 2. SOLUSI AMAN: Berikan padding atas dinamis HANYA pada Toolbar agar tidak terpotong Status Bar/Notch
        ViewCompat.setOnApplyWindowInsetsListener(binding.riderToolbar) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Mengatur padding atas Toolbar sesuai tinggi Status Bar perangkat
            view.setPadding(0, insets.top, 0, 0)
            windowInsets
        }

        // Setel Fragment pertama kali muncul (RiderDashboardFragment)
        if (savedInstanceState == null) {
            gantiRiderFragment(RiderDashboardFragment())
        }

        // Logika Klik Menu Bottom Navigation Bar Rider
        binding.riderBottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_rider_dashboard -> {
                    gantiRiderFragment(RiderDashboardFragment())
                    true
                }
                R.id.nav_rider_stok -> {
                    gantiRiderFragment(RiderStokFragment())
                    true
                }
                R.id.nav_rider_kasir -> {
                    gantiRiderFragment(RiderKasirFragment())
                    true
                }
                R.id.nav_rider_aktivitas -> {
                    gantiRiderFragment(RiderAktivitasFragment())
                    true
                }
                else -> false
            }
        }
    }

    // Fungsi utilitas untuk mengganti fragment secara modular
    fun gantiRiderFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.rider_fragment_container, fragment)
            .commit()
    }

    // ────────────────────────────────────────────────────────────────────────
    // LOGIKA OPERASI LOGOUT MELALUI STANDAR OPTION MENU TOOLBAR RIDER
    // ────────────────────────────────────────────────────────────────────────

    // Menginflasi menu_rider_top ke dalam Toolbar melalui standar Lifecycle Activity
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_rider_top, menu)
        return true
    }

    // Menangani aksi klik pada item menu logout di Toolbar secara stabil
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_rider_logout -> {
                tampilkanDialogKonfirmasiLogout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun tampilkanDialogKonfirmasiLogout() {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Keluar")
            .setMessage("Apakah Anda yakin ingin keluar dari akun Rider?")
            .setPositiveButton("Logout") { _, _ ->
                // 1. Putus sesi Firebase Auth
                mAuth.signOut()

                Toast.makeText(this, "Berhasil keluar dari akun Rider", Toast.LENGTH_SHORT).show()

                // 2. Bersihkan backstack dan tendang balik ke LoginActivity
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }
}