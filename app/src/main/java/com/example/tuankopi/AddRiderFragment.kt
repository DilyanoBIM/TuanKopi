package com.example.tuankopi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.tuankopi.databinding.FragmentAddRiderBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Suppress("SpellCheckingInspection")
class AddRiderFragment : Fragment() {

    private var _binding: FragmentAddRiderBinding? = null
    private val binding get() = _binding!!
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mFirestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddRiderBinding.inflate(inflater, container, false)
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

            // Gunakan coroutine scope untuk menjalankan fungsi suspend
            lifecycleScope.launch {
                tambahRiderKeSistem(nama, noHp, email, password)
            }
        }

        return binding.root
    }

    private suspend fun tambahRiderKeSistem(nama: String, noHp: String, email: String, password: String) {
        setLoadingState(true)

        try {
            // 1. CEK KEUNIKAN NOMOR HP DI FIRESTORE TERLEBIH DAHULU
            val cekHpSnapshot = withContext(Dispatchers.IO) {
                mFirestore.collection("users").whereEqualTo("no_hp", noHp).get().await()
            }

            if (!cekHpSnapshot.isEmpty) {
                setLoadingState(false)
                binding.etNoHpRider.error = "Nomor HP sudah digunakan"
                Toast.makeText(requireContext(), "Gagal: Nomor HP sudah terdaftar pada akun lain!", Toast.LENGTH_LONG).show()
                return
            }

            // 2. INISIALISASI SECONDARY APP UNTUK AUTH (Agar sesi Owner tidak ter-logout)
            val mainApp = FirebaseApp.getInstance()
            val options = mainApp.options
            val secondaryApp = try {
                FirebaseApp.getInstance("SecondaryApp")
            } catch (e: IllegalStateException) {
                FirebaseApp.initializeApp(requireContext(), options, "SecondaryApp")
            }

            val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)

            // 3. BUAT AKUN DI FIREBASE AUTH
            val authResult = withContext(Dispatchers.IO) {
                secondaryAuth.createUserWithEmailAndPassword(email, password).await()
            }

            val riderUid = authResult.user?.uid
            if (riderUid != null) {
                // 4. SIMPAN PROFIL KE FIRESTORE
                simpanProfilRiderKeFirestore(riderUid, nama, noHp, email)
            }

            // Sign out secondary auth setelah berhasil buat akun
            secondaryAuth.signOut()

        } catch (e: Exception) {
            setLoadingState(false)
            Toast.makeText(requireContext(), "Gagal Mendaftarkan Rider: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun simpanProfilRiderKeFirestore(uid: String, nama: String, noHp: String, email: String) {
        val dataRider = hashMapOf(
            "uid" to uid,
            "nama" to nama,
            "email" to email,
            "no_hp" to noHp,
            "role" to "rider",
            "status_akun" to true,
            "created_at" to FieldValue.serverTimestamp()
        )

        try {
            withContext(Dispatchers.IO) {
                mFirestore.collection("users").document(uid).set(dataRider).await()
            }

            if (_binding != null) {
                setLoadingState(false)
                Toast.makeText(requireContext(), "Akun Rider $nama Berhasil Terdaftar!", Toast.LENGTH_SHORT).show()
                requireActivity().supportFragmentManager.popBackStack()
            }
        } catch (e: Exception) {
            if (_binding != null) {
                setLoadingState(false)
                Toast.makeText(requireContext(), "Gagal Simpan Database: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.progressBarAdd.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSimpanRider.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}