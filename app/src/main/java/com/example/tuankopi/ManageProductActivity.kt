package com.example.tuankopi

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tuankopi.databinding.ActivityManageProductBinding
import com.google.firebase.firestore.FirebaseFirestore

class ManageProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageProductBinding
    private lateinit var mFirestore: FirebaseFirestore
    private lateinit var adapterProduct: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.customToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Katalog Produk Master"

        ViewCompat.setOnApplyWindowInsetsListener(binding.customToolbar) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, insets.top, 0, 0)
            windowInsets
        }

        mFirestore = FirebaseFirestore.getInstance()

        setupRecyclerView()

        binding.fabAddProduct.setOnClickListener {
            startActivity(Intent(this, AddProductActivity::class.java))
        }

        muatDataProdukDariFirestore()
    }

    private fun setupRecyclerView() {
        adapterProduct = ProductAdapter(
            listProduct = emptyList(),
            onStatusChanged = { idProduk, statusBaru ->
                ubahStatusTersediaProduk(idProduk, statusBaru)
            },
            onItemClick = { product ->
                val intent = Intent(this, DetailProductActivity::class.java).apply {
                    putExtra("PROD_ID", product.id_produk)
                    putExtra("PROD_NAMA", product.nama_produk)
                    putExtra("PROD_HARGA", product.harga_jual)
                    putExtra("PROD_FOTO", product.foto_url)
                }
                startActivity(intent)
            }
        )
        binding.rvProduct.layoutManager = LinearLayoutManager(this)
        binding.rvProduct.adapter = adapterProduct
    }

    private fun muatDataProdukDariFirestore() {
        mFirestore.collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Gagal memuat data: ${error.message}", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, pesan, Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal memperbarui status: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}