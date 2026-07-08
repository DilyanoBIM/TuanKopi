package com.example.tuankopi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tuankopi.databinding.ActivityInputStokGudangBinding
import com.example.tuankopi.databinding.ItemInputProdukGudangBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class InputStokGudangActivitygapakai : AppCompatActivity() {

    private lateinit var binding: ActivityInputStokGudangBinding
    private lateinit var mFirestore: FirebaseFirestore
    private var listProductMaster = ArrayList<Product>()
    private val petaInputJumlah = HashMap<String, Long>()
    private lateinit var mAdapter: InputAdapter
    private var tanggalTarget = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInputStokGudangBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Input Produksi Awal"

        mFirestore = FirebaseFirestore.getInstance()
        tanggalTarget = intent.getStringExtra("TARGET_TANGGAL") ?: ""
        binding.tvTanggalTerpilih.text = "Tanggal Produksi: $tanggalTarget"

        setupRecyclerView()
        muatMasterProductTersedia()

        binding.btnSaveStokGudang.setOnClickListener {
            simpanSeluruhBatchGudang()
        }
    }

    private fun setupRecyclerView() {
        mAdapter = InputAdapter(listProductMaster) { idProd, jml ->
            petaInputJumlah[idProd] = jml
        }
        binding.rvInputProduk.layoutManager = LinearLayoutManager(this)
        binding.rvInputProduk.adapter = mAdapter
    }

    private fun muatMasterProductTersedia() {
        mFirestore.collection("products")
            .whereEqualTo("status_tersedia", true)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null) {
                    listProductMaster.clear()
                    listProductMaster.addAll(snapshot.toObjects(Product::class.java))
                    mAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun simpanSeluruhBatchGudang() {
        val cleanedTanggalId = tanggalTarget.replace("-", "")

        if (petaInputJumlah.isEmpty() || petaInputJumlah.values.all { it == 0L }) {
            Toast.makeText(this, "Mohon isi minimal satu kuantitas produksi kopi!", Toast.LENGTH_SHORT).show()
            return
        }

        val refDocGudang = mFirestore.collection("stok_gudang").document(cleanedTanggalId)
        val updates = HashMap<String, Any>()

        updates["id_gudang"] = "GUD_$cleanedTanggalId"
        updates["tanggal"] = tanggalTarget
        updates["last_updated"] = Timestamp.now()

        for (prod in listProductMaster) {
            val jmlInput = petaInputJumlah[prod.id_produk] ?: 0L
            if (jmlInput > 0L) {
                updates["detail_gudang.${prod.id_produk}.id_produk"] = prod.id_produk
                updates["detail_gudang.${prod.id_produk}.nama_produk"] = prod.nama_produk
                updates["detail_gudang.${prod.id_produk}.harga_jual"] = prod.harga_jual
                updates["detail_gudang.${prod.id_produk}.stok_masuk_awal"] = jmlInput
                updates["detail_gudang.${prod.id_produk}.stok_dialokasikan"] = 0L
                updates["detail_gudang.${prod.id_produk}.sisa_gudang"] = jmlInput
                updates["detail_gudang.${prod.id_produk}.stok_total"] = jmlInput
            }
        }

        refDocGudang.set(updates, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Stok produksi $tanggalTarget berhasil disatukan di gudang!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal mengupdate logistik: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    inner class InputAdapter(
        private val data: List<Product>,
        private val onQtyChanged: (String, Long) -> Unit
    ) : RecyclerView.Adapter<InputAdapter.ViewHolder>() {
        inner class ViewHolder(val b: ItemInputProdukGudangBinding) : RecyclerView.ViewHolder(b.root) {
            var textWatcher: android.text.TextWatcher? = null
        }
        override fun onCreateViewHolder(p: ViewGroup, vt: Int): ViewHolder {
            return ViewHolder(ItemInputProdukGudangBinding.inflate(LayoutInflater.from(p.context), p, false))
        }
        override fun onBindViewHolder(vh: ViewHolder, pos: Int) {
            val item = data[pos]
            vh.b.tvNamaProdukInput.text = item.nama_produk

            vh.b.etJumlahStokMasuk.removeTextChangedListener(vh.textWatcher)

            val kuantitasSore = petaInputJumlah[item.id_produk]
            vh.b.etJumlahStokMasuk.setText(kuantitasSore?.toString() ?: "")

            vh.textWatcher = object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    val txt = s.toString().trim()
                    val qty = if (txt.isEmpty()) 0L else txt.toLong()
                    onQtyChanged(item.id_produk, qty)
                }
            }
            vh.b.etJumlahStokMasuk.addTextChangedListener(vh.textWatcher)
        }
        override fun getItemCount(): Int = data.size
    }
}