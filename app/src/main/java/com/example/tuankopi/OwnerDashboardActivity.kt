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
import com.example.tuankopi.databinding.ActivityOwnerDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

class OwnerDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOwnerDashboardBinding
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOwnerDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = FirebaseAuth.getInstance()
        daftarkanNotifikasiTransaksiOwner()

        setSupportActionBar(binding.ownerToolbar)
        supportActionBar?.title = "Tuan Kopi - Owner"

        ViewCompat.setOnApplyWindowInsetsListener(binding.ownerToolbar) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, insets.top, 0, 0)
            windowInsets
        }

        if (savedInstanceState == null) {
            gantiFragment(HomeFragment())
        }

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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_owner_dashboard, menu)
        return true
    }

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
            .setPositiveButton("Logout") { _, _ ->
                mAuth.signOut()

                Toast.makeText(this, "Berhasil keluar dari sistem", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }
    private fun daftarkanNotifikasiTransaksiOwner() {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("owners")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    android.util.Log.d("FCM_SETUP", "Sukses mendaftarkan perangkat ke saluran data transaksi Owner.")
                } else {
                    android.util.Log.e("FCM_SETUP", "Gagal mendaftarkan topik.", task.exception)
                }
            }
    }
}