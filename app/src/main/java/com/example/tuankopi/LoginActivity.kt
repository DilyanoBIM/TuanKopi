package com.example.tuankopi

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tuankopi.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mFirestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = FirebaseAuth.getInstance()
        mFirestore = FirebaseFirestore.getInstance()

        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty()) {
                binding.etEmail.error = "Email tidak boleh kosong"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.etPassword.error = "Password tidak boleh kosong"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                jalankanProsesLogin(email, password)
            }
        }
    }

    private suspend fun jalankanProsesLogin(email: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSignIn.visibility = View.GONE

        try {
            // .await() secara asinkron menunggu respons jaringan tanpa memblokir Main Thread
            val authResult = mAuth.signInWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid

            if (uid == null) {
                resetKomponenUI("Gagal mengidentifikasi User ID")
                return
            }

            val document = mFirestore.collection("users").document(uid).get().await()

            if (document != null && document.exists()) {
                // Verifikasi status akun langsung di sini (tidak perlu Dispatchers.Default)
                val statusAkun = document.getBoolean("status_akun") ?: false
                if (!statusAkun) {
                    mAuth.signOut()
                    resetKomponenUI("Akun Anda dinonaktifkan oleh Owner.")
                    return // Sekarang return ini benar-benar menghentikan fungsi jalankanProsesLogin
                }

                val namaUser = document.getString("nama") ?: "User"
                val role = document.getString("role")

                Toast.makeText(this@LoginActivity, "Selamat Datang, $namaUser!", Toast.LENGTH_SHORT).show()

                when (role) {
                    "owner" -> {
                        startActivity(Intent(this@LoginActivity, OwnerDashboardActivity::class.java))
                        finish()
                    }
                    "rider" -> {
                        startActivity(Intent(this@LoginActivity, RiderDashboardActivity::class.java))
                        finish()
                    }
                    else -> {
                        mAuth.signOut()
                        resetKomponenUI("Hak akses (Role) tidak dikenali sistem.")
                    }
                }
            } else {
                mAuth.signOut()
                resetKomponenUI("Profil akun belum terdaftar di database Firestore.")
            }

        } catch (e: Exception) {
            resetKomponenUI("Terjadi kesalahan: ${e.localizedMessage}")
        }
    }

    private fun resetKomponenUI(pesanError: String) {
        binding.progressBar.visibility = View.GONE
        binding.btnSignIn.visibility = View.VISIBLE
        Toast.makeText(this, pesanError, Toast.LENGTH_LONG).show()
    }
}