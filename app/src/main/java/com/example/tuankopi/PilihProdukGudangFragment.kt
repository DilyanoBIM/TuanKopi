package com.example.tuankopi

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tuankopi.databinding.FragmentPilihProdukGudangBinding
import com.example.tuankopi.databinding.ItemPilihProdukGudangBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

class PilihProdukGudangFragment : Fragment() {

    private var _binding: FragmentPilihProdukGudangBinding? = null
    private val binding get() = _binding!!
    private lateinit var mFirestore: FirebaseFirestore
    private var listProduct = ArrayList<Product>()
    private lateinit var mAdapter: PilihAdapter
    private var tanggalTarget = ""

    private val keranjangJumlahLokal = HashMap<String, Long>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPilihProdukGudangBinding.inflate(inflater, container, false)
        mFirestore = FirebaseFirestore.getInstance()

        tanggalTarget = arguments?.getString("TARGET_TANGGAL") ?: ""

        setupRecyclerView()
        muatKatalogProductAktif()

        binding.btnSimpanBatchGudang.setOnClickListener {
            eksekusiSimpanKolektifGudang()
        }

        return binding.root
    }

    private fun setupRecyclerView() {
        mAdapter = PilihAdapter(listProduct) { idProd, qty ->
            if (qty > 0) {
                keranjangJumlahLokal[idProd] = qty
            } else {
                keranjangJumlahLokal.remove(idProd)
            }
        }
        binding.rvPilihProduk.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPilihProduk.adapter = mAdapter
    }

    private fun muatKatalogProductAktif() {
        mFirestore.collection("products")
            .whereEqualTo("status_tersedia", true)
            .get()
            .addOnSuccessListener { snapshots ->
                if (snapshots != null) {
                    listProduct.clear()
                    listProduct.addAll(snapshots.toObjects(Product::class.java))
                    mAdapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Gagal memuat katalog: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun eksekusiSimpanKolektifGudang() {
        if (keranjangJumlahLokal.isEmpty()) {
            Toast.makeText(requireContext(), "Mohon isi jumlah jatah produksi minimal pada satu produk!", Toast.LENGTH_SHORT).show()
            return
        }

        val cleanedTgl = tanggalTarget.replace("-", "")
        val refDoc = mFirestore.collection("stok_gudang").document(cleanedTgl)

        mFirestore.runTransaction { transaction ->
            val snapshot = transaction.get(refDoc)

            val rawDetailGudang = snapshot.get("detail_gudang") as? Map<*, *>
            val detailGudangTerbarui = HashMap<String, Any>()

            if (rawDetailGudang != null) {
                for ((k, v) in rawDetailGudang) {
                    detailGudangTerbarui[k.toString()] = v!!
                }
            }

            for ((idProd, qtyInput) in keranjangJumlahLokal) {
                val produkMaster = listProduct.find { it.id_produk == idProd } ?: continue
                val dataKopiLama = rawDetailGudang?.get(idProd) as? Map<*, *>

                val lamaAlokasi = dataKopiLama?.get("stok_dialokasikan") as? Long ?: 0L
                val awalBaru: Long
                val tambahanBaru: Long
                val totalBaru: Long
                val sisaBaru: Long

                if (dataKopiLama == null || (dataKopiLama["stok_total"] as? Long ?: 0L) == 0L) {
                    awalBaru = qtyInput
                    tambahanBaru = 0L
                    totalBaru = qtyInput
                    sisaBaru = qtyInput
                } else {
                    val lamaAwal = dataKopiLama["stok_masuk_awal"] as? Long ?: 0L
                    val lamaTambahan = dataKopiLama["stok_tambahan"] as? Long ?: 0L
                    val lamaSisa = dataKopiLama["sisa_gudang"] as? Long ?: 0L

                    awalBaru = lamaAwal
                    tambahanBaru = lamaTambahan + qtyInput
                    totalBaru = awalBaru + tambahanBaru
                    sisaBaru = lamaSisa + qtyInput
                }

                detailGudangTerbarui[idProd] = hashMapOf(
                    "id_produk" to idProd,
                    "nama_produk" to produkMaster.nama_produk,
                    "harga_jual" to produkMaster.harga_jual,
                    "stok_masuk_awal" to awalBaru,
                    "stok_tambahan" to tambahanBaru,
                    "stok_dialokasikan" to lamaAlokasi,
                    "sisa_gudang" to sisaBaru,
                    "stok_total" to totalBaru
                )
            }

            transaction.update(refDoc, "detail_gudang", detailGudangTerbarui)
            transaction.update(refDoc, "last_updated", Timestamp.now())

            null
        }.addOnSuccessListener {
            if (context != null) {
                Toast.makeText(requireContext(), "Batch logistik harian berhasil disinkronkan!", Toast.LENGTH_SHORT).show()
                requireActivity().supportFragmentManager.popBackStack()
            }
        }.addOnFailureListener { e ->
            if (context != null) {
                Toast.makeText(requireContext(), "Gagal mengalkulasi transaksi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    inner class PilihAdapter(
        private val data: List<Product>,
        val onQtyChanged: (String, Long) -> Unit
    ) : RecyclerView.Adapter<PilihAdapter.ViewHolder>() {

        inner class ViewHolder(val b: ItemPilihProdukGudangBinding) : RecyclerView.ViewHolder(b.root) {
            var textWatcher: TextWatcher? = null
        }

        override fun onCreateViewHolder(p: ViewGroup, vt: Int): ViewHolder {
            return ViewHolder(ItemPilihProdukGudangBinding.inflate(LayoutInflater.from(p.context), p, false))
        }

        override fun onBindViewHolder(vh: ViewHolder, pos: Int) {
            val item = data[pos]
            vh.b.tvNamaProdukBawaan.text = item.nama_produk

            val formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("in", "ID"))
            vh.b.tvHargaProdukBawaan.text = formatter.format(item.harga_jual).replace(",00", "")

            vh.b.etJumlahMasukLokal.removeTextChangedListener(vh.textWatcher)

            val jumlahLokal = keranjangJumlahLokal[item.id_produk]
            vh.b.etJumlahMasukLokal.setText(jumlahLokal?.toString() ?: "")

            vh.textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val txt = s.toString().trim()
                    val qty = if (txt.isEmpty()) 0L else txt.toLong()
                    onQtyChanged(item.id_produk, qty)
                }
            }
            vh.b.etJumlahMasukLokal.addTextChangedListener(vh.textWatcher)
        }

        override fun getItemCount(): Int = data.size
    }
}