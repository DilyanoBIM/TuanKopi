package com.example.tuankopi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.tuankopi.databinding.FragmentAddRiderBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

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

            tambahRiderKeSistem(nama, noHp, email, password)
        }

        return binding.root
    }

    private fun tambahRiderKeSistem(nama: String, noHp: String, email: String, password: String) {
        setLoadingState(true)

        val mainApp = FirebaseApp.getInstance()
        val options = mainApp.options
        val secondaryApp = try {
            FirebaseApp.getInstance("SecondaryApp")
        } catch (e: IllegalStateException) {
            FirebaseApp.initializeApp(requireContext(), options, "SecondaryApp")
        }

        val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)

        secondaryAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val riderUid = authResult.user?.uid
                if (riderUid != null) {
                    simpanProfilRiderKeFirestore(riderUid, nama, noHp, email)
                }

                secondaryAuth.signOut()
            }
            .addOnFailureListener { exception ->

                setLoadingState(false)
                Toast.makeText(requireContext(), "Gagal Auth: ${exception.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun simpanProfilRiderKeFirestore(uid: String, nama: String, noHp: String, email: String) {
        val dataRider = hashMapOf(
            "uid" to uid, "nama" to nama, "email" to email, "no_hp" to noHp,
            "role" to "rider", "status_akun" to true,
            "created_at" to FieldValue.serverTimestamp()
        )
        mFirestore.collection("users").document(uid).set(dataRider)
            .addOnSuccessListener {
                if (_binding == null) return@addOnSuccessListener
                setLoadingState(false)
                Toast.makeText(requireContext(), "Akun Rider $nama Berhasil Terdaftar!", Toast.LENGTH_SHORT).show()
                requireActivity().supportFragmentManager.popBackStack()
            }
            .addOnFailureListener { exception ->
                if (_binding == null) return@addOnFailureListener
                setLoadingState(false)
                Toast.makeText(requireContext(), "Gagal Firestore: ${exception.localizedMessage}", Toast.LENGTH_LONG).show()
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