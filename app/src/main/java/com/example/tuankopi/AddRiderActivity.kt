package com.example.tuankopi

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tuankopi.databinding.ActivityAddRiderBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@Suppress("SpellCheckingInspection")
class AddRiderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddRiderBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mFirestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddRiderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.customToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Tambah Rider Baru"

        ViewCompat.setOnApplyWindowInsetsListener(binding.customToolbar) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, insets.top, 0, 0)
            windowInsets
        }

        mAuth = FirebaseAuth.getInstance()
        mFirestore = FirebaseFirestore.getInstance()

        binding.btnSimpanRider.setOnClickListener {
            val nama = binding.etNamaRider.text.toString().trim()
            val noHp = binding.etNoHpRider.text.toString().trim()
            val email = binding.etEmailRider.text.toString().trim()
            val password = binding.etPasswordRider.text.toString().trim()

            if (nama.isEmpty()) { binding.etNamaRider.error = "Nama wajib diisi"; return@setOnClickListener }
            if (noHp.isEmpty()) { binding.etNoHpRider.error = "No HP wajib diisi"; return@setOnClickListener }
            if (email.isEmpty()) { binding.etEmailRider.error = "Email wajib diisi"; return@setOnClickListener }
            if (password.length < 6) { binding.etPasswordRider.error = "Password minimal 6 karakter"; return@setOnClickListener }

            tambahRiderKeSistem(nama, noHp, email, password)
        }
    }

    private fun tambahRiderKeSistem(nama: String, noHp: String, email: String, password: String) {
        setLoadingState(true)

        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val riderUid = authResult.user?.uid
                if (riderUid != null) {
                    simpanProfilRiderKeFirestore(riderUid, nama, noHp, email)
                }
            }
            .addOnFailureListener { exception ->
                setLoadingState(false)
                Toast.makeText(this, "Gagal Auth: ${exception.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun simpanProfilRiderKeFirestore(uid: String, nama: String, noHp: String, email: String) {
        val dataRider = hashMapOf(
            "uid" to uid,
            "nama" to nama,
            "email" to email,
            "no_hp" to noHp,
            "role" to "rider",
            "status_akun" to true,
            "created_at" to FieldValue.serverTimestamp()
        )

        mFirestore.collection("users").document(uid).set(dataRider)
            .addOnSuccessListener {
                setLoadingState(false)
                Toast.makeText(this, "Akun Rider $nama Berhasil Terdaftar!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { exception ->
                setLoadingState(false)
                Toast.makeText(this, "Gagal Firestore: ${exception.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setLoadingState(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBarAdd.visibility = View.VISIBLE
            binding.btnSimpanRider.visibility = View.GONE
        } else {
            binding.progressBarAdd.visibility = View.GONE
            binding.btnSimpanRider.visibility = View.VISIBLE
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