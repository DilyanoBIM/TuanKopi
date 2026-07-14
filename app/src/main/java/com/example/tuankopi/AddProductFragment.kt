package com.example.tuankopi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.tuankopi.databinding.FragmentAddProductBinding
import com.google.firebase.firestore.FirebaseFirestore

class AddProductFragment : Fragment() {

    private var _binding: FragmentAddProductBinding? = null
    private val binding get() = _binding!!
    private lateinit var mFirestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddProductBinding.inflate(inflater, container, false)
        mFirestore = FirebaseFirestore.getInstance()

        binding.btnSimpanProduk.setOnClickListener {
            val idProduk = binding.etIdProduk.text.toString().trim().uppercase()
            val namaProduk = binding.etNamaProduk.text.toString().trim()
            val hargaStr = binding.etHargaProduk.text.toString().trim()

            if (idProduk.isEmpty()) { binding.etIdProduk.error = "ID wajib diisi"; return@setOnClickListener }
            if (namaProduk.isEmpty()) { binding.etNamaProduk.error = "Nama wajib diisi"; return@setOnClickListener }
            if (hargaStr.isEmpty()) { binding.etHargaProduk.error = "Harga wajib diisi"; return@setOnClickListener }

            val hargaJual = hargaStr.toLong()
            simpanProdukKeFirestore(idProduk, namaProduk, hargaJual)
        }

        return binding.root
    }

    private fun simpanProdukKeFirestore(id: String, nama: String, harga: Long) {
        val dataProduk = hashMapOf(
            "id_produk" to id,
            "nama_produk" to nama,
            "harga_jual" to harga,
            "foto_url" to "",
            "status_accessible" to true,
            "status_tersedia" to true
        )

        mFirestore.collection("products").document(id).set(dataProduk)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Produk $nama Berhasil Ditambahkan!", Toast.LENGTH_SHORT).show()
                requireActivity().supportFragmentManager.popBackStack() // Kembali ke halaman sebelumnya
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Gagal: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}