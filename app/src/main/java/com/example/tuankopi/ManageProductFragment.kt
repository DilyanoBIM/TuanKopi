package com.example.tuankopi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tuankopi.databinding.FragmentManageProductBinding
import com.google.firebase.firestore.FirebaseFirestore

class ManageProductFragment : Fragment() {

    private var _binding: FragmentManageProductBinding? = null
    private val binding get() = _binding!!
    private lateinit var mFirestore: FirebaseFirestore
    private lateinit var adapterProduct: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageProductBinding.inflate(inflater, container, false)
        mFirestore = FirebaseFirestore.getInstance()

        setupRecyclerView()

        binding.fabAddProduct.setOnClickListener {
            (requireActivity() as OwnerDashboardActivity)
                .bukaHalaman(AddProductFragment(), "Tambah Produk")
        }

        muatDataProdukDariFirestore()
        return binding.root
    }

    private fun setupRecyclerView() {
        adapterProduct = ProductAdapter(
            listProduct = emptyList(),
            onStatusChanged = { idProduk, statusBaru ->
                ubahStatusTersediaProduk(idProduk, statusBaru)
            },
            onItemClick = { product ->
                val fragment = DetailProductFragment().apply {
                    arguments = Bundle().apply {
                        putString("PROD_ID", product.id_produk)
                        putString("PROD_NAMA", product.nama_produk)
                        putLong("PROD_HARGA", product.harga_jual)
                        putString("PROD_FOTO", product.foto_url)
                    }
                }
                (requireActivity() as OwnerDashboardActivity)
                    .bukaHalaman(fragment, "Detail Produk")
            }
        )
        binding.rvProduct.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProduct.adapter = adapterProduct
    }

    private fun muatDataProdukDariFirestore() {
        mFirestore.collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(requireContext(), "Gagal memuat data: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.toObjects(Product::class.java)
                    adapterProduct.updateData(list)
                }
            }
    }

    private fun ubahStatusTersediaProduk(idProduk: String, statusBaru: Boolean) {
        mFirestore.collection("products").document(idProduk)
            .update("status_tersedia", statusBaru)
            .addOnSuccessListener {
                val pesan = if (statusBaru) "Menu tersedia" else "Menu dinonaktifkan"
                Toast.makeText(requireContext(), pesan, Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Gagal memperbarui status: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}