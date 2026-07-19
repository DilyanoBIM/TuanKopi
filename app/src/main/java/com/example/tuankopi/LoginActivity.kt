package com.example.tuankopi

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.example.tuankopi.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mFirestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Memaksa aplikasi menggunakan Light Mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = FirebaseAuth.getInstance()
        mFirestore = FirebaseFirestore.getInstance()

        // Auto-login jika sesi masih ada
        if (mAuth.currentUser != null) {
            binding.progressBar.visibility = View.VISIBLE
            binding.btnSignIn.visibility = View.GONE
            lifecycleScope.launch { arahkanKeDashboardSesuaiRole(mAuth.currentUser!!.uid) }
        }

        binding.btnSignIn.setOnClickListener {
            val inputLogin = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (inputLogin.isEmpty()) {
                binding.etEmail.error = "Email atau No. HP tidak boleh kosong"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.etPassword.error = "Password tidak boleh kosong"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                jalankanProsesLogin(inputLogin, password)
            }
        }
    }

    private suspend fun jalankanProsesLogin(inputLogin: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSignIn.visibility = View.GONE

        try {
            var emailUntukLogin = inputLogin

            // DETEKSI INPUT: Jika input TIDAK mengandung '@', asumsikan itu Nomor HP
            if (!inputLogin.contains("@")) {
                val usersRef = mFirestore.collection("users")
                // Cari dokumen user berdasarkan no_hp
                val querySnapshot = withContext(Dispatchers.IO) {
                    usersRef.whereEqualTo("no_hp", inputLogin).get().await()
                }

                if (querySnapshot.isEmpty) {
                    resetKomponenUI("Nomor HP tidak terdaftar di sistem.")
                    return
                }

                // Ambil email dari data profil Firestore yang ditemukan
                val userDoc = querySnapshot.documents[0]
                val emailDariDatabase = userDoc.getString("email")

                if (emailDariDatabase.isNullOrEmpty()) {
                    resetKomponenUI("Akun ditemukan, tapi data email tidak valid.")
                    return
                }

                // Gunakan email yang ditemukan untuk proses autentikasi Firebase
                emailUntukLogin = emailDariDatabase
            }

            // PROSES AUTENTIKASI FIREBASE
            val uid = withContext(Dispatchers.IO) {
                val authResult = mAuth.signInWithEmailAndPassword(emailUntukLogin, password).await()
                authResult.user?.uid
            }

            if (uid == null) {
                resetKomponenUI("Gagal mengidentifikasi User ID")
                return
            }

            arahkanKeDashboardSesuaiRole(uid)

        } catch (e: Exception) {
            // Error handling disederhanakan agar tidak memunculkan pesan error bahasa Inggris bawaan Firebase ke user
            resetKomponenUI("Gagal login: Kredensial salah atau bermasalah.")
        }
    }

    private suspend fun arahkanKeDashboardSesuaiRole(uid: String) {
        try {
            val document = withContext(Dispatchers.IO) {
                mFirestore.collection("users").document(uid).get().await()
            }

            if (document != null && document.exists()) {
                val statusAkun = document.getBoolean("status_akun") ?: false
                if (!statusAkun) {
                    mAuth.signOut()
                    resetKomponenUI("Akun Anda dinonaktifkan oleh Owner.")
                    return
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
                resetKomponenUI("Profil akun belum terdaftar di database.")
            }

        } catch (e: Exception) {
            mAuth.signOut()
            resetKomponenUI("Gagal memuat profil: ${e.localizedMessage}")
        }
    }

    private fun resetKomponenUI(pesanError: String) {
        binding.progressBar.visibility = View.GONE
        binding.btnSignIn.visibility = View.VISIBLE
        Toast.makeText(this, pesanError, Toast.LENGTH_LONG).show()
    }
}