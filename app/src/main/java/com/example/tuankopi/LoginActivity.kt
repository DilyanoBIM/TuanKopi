package com.example.tuankopi

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tuankopi.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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

            prosesAutentikasiFirebase(email, password)
        }
    }

    private fun prosesAutentikasiFirebase(email: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSignIn.visibility = View.GONE

        mAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid
                if (uid != null) {
                    cekRoleUserKeFirestore(uid)
                } else {
                    resetKomponenUI("Gagal mengidentifikasi User ID")
                }
            }
            .addOnFailureListener { exception ->
                resetKomponenUI("Autentikasi Gagal: ${exception.localizedMessage}")
            }
    }

    private fun cekRoleUserKeFirestore(uid: String) {
        mFirestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {

                    val statusAkun = document.getBoolean("status_akun") ?: false
                    if (!statusAkun) {
                        mAuth.signOut()
                        resetKomponenUI("Akun Anda dinonaktifkan oleh Owner.")
                        return@addOnSuccessListener
                    }

                    val namaUser = document.getString("nama") ?: "User"
                    val role = document.getString("role")

                    Toast.makeText(this, "Selamat Datang, $namaUser!", Toast.LENGTH_SHORT).show()

                    when (role) {
                        "owner" -> {
                            val intent = Intent(this, OwnerDashboardActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                        "rider" -> {
                            val intent = Intent(this, RiderDashboardActivity::class.java)
                            startActivity(intent)
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
            }
            .addOnFailureListener { exception ->
                resetKomponenUI("Gagal memuat data peran: ${exception.localizedMessage}")
            }
    }

    private fun resetKomponenUI(pesanError: String) {
        binding.progressBar.visibility = View.GONE
        binding.btnSignIn.visibility = View.VISIBLE
        Toast.makeText(this, pesanError, Toast.LENGTH_LONG).show()
    }
}