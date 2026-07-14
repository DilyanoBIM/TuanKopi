package com.example.tuankopi

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tuankopi.databinding.FragmentDetailStokGudangBinding
import com.example.tuankopi.databinding.ItemDetailGudangBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class DetailStokGudangFragment : Fragment() {

    private var _binding: FragmentDetailStokGudangBinding? = null
    private val binding get() = _binding!!
    private lateinit var mFirestore: FirebaseFirestore
    private var listStok = ArrayList<StokGudang>()
    private lateinit var mAdapter: DetailAdapter
    private var posisiTerbuka: Int = -1
    private var tanggalLihat = ""
    private var docIdGudang = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailStokGudangBinding.inflate(inflater, container, false)
        mFirestore = FirebaseFirestore.getInstance()

        tanggalLihat = arguments?.getString("LIHAT_TANGGAL") ?: ""

        val tanggalIndo = formatKeTanggalIndo(tanggalLihat)
        binding.tvDetailHeaderTanggal.text = "Rincian Stock Pada:\n$tanggalIndo"

        docIdGudang = tanggalLihat.replace("-", "")

        setupRecyclerView()
        muatDetailStokBerdasarkanMapTanggal()

        binding.fabAddItemKeTanggal.setOnClickListener {
            val fragment = PilihProdukGudangFragment().apply {
                arguments = Bundle().apply { putString("TARGET_TANGGAL", tanggalLihat) }
            }
            (requireActivity() as OwnerDashboardActivity).bukaHalaman(fragment, "Pilih Menu Kopi")
        }

        return binding.root
    }

    private fun setupRecyclerView() {
        mAdapter = DetailAdapter(listStok)
        binding.rvDetailGudang.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDetailGudang.adapter = mAdapter
    }

    private fun muatDetailStokBerdasarkanMapTanggal() {
        if (docIdGudang.isEmpty()) return

        mFirestore.collection("stok_gudang").document(docIdGudang)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (context != null) Toast.makeText(requireContext(), "Gagal memuat: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    Thread {
                        val listSaringanLokal = ArrayList<StokGudang>()
                        val rawMapDetail = snapshot.get("detail_gudang") as? Map<*, *>

                        if (rawMapDetail != null) {
                            for ((_, value) in rawMapDetail) {
                                val dataKopi = value as? Map<*, *> ?: continue

                                val idProd = dataKopi["id_produk"] as? String ?: ""
                                val namaProd = dataKopi["nama_produk"] as? String ?: "Menu Kopi"
                                val harga = dataKopi["harga_jual"] as? Long ?: 0L
                                val awal = dataKopi["stok_masuk_awal"] as? Long ?: 0L
                                val tambahan = dataKopi["stok_tambahan"] as? Long ?: 0L
                                val alokasi = dataKopi["stok_dialokasikan"] as? Long ?: 0L
                                val sisa = dataKopi["sisa_gudang"] as? Long ?: 0L
                                val total = dataKopi["stok_total"] as? Long ?: (awal + tambahan)

                                if (idProd.isNotEmpty()) {
                                    listSaringanLokal.add(
                                        StokGudang(
                                            id_gudang = "GUD_$docIdGudang",
                                            tanggal = tanggalLihat,
                                            id_produk = idProd,
                                            nama_produk = namaProd,
                                            stok_masuk_awal = awal,
                                            stok_tambahan = tambahan,
                                            stok_dialokasikan = alokasi,
                                            sisa_gudang = sisa,
                                            harga_jual = harga,
                                            stok_total = total
                                        )
                                    )
                                }
                            }
                        }

                        listSaringanLokal.sortBy { it.nama_produk }

                        activity?.runOnUiThread {
                            if (_binding != null) {
                                listStok.clear()
                                listStok.addAll(listSaringanLokal)
                                mAdapter.notifyDataSetChanged()
                            }
                        }
                    }.start()
                }
            }
    }

    private fun tampilkanDialogEditStok(item: StokGudang) {
        val dialogBuilder = MaterialAlertDialogBuilder(requireContext())
        dialogBuilder.setTitle("Edit Jumlah Produksi")
        dialogBuilder.setMessage("Ubah jumlah jatah seduh awal untuk ${item.nama_produk}:")

        val input = EditText(requireContext())
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.setText(item.stok_masuk_awal.toString())
        input.setSelection(input.text.length)

        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        input.layoutParams = lp

        val container = LinearLayout(requireContext())
        container.setPadding(dpToPx(requireContext(), 20), dpToPx(requireContext(), 4), dpToPx(requireContext(), 20), dpToPx(requireContext(), 4))
        container.addView(input)
        dialogBuilder.setView(container)

        dialogBuilder.setPositiveButton("Simpan") { dialog, _ ->
            val teksJumlah = input.text.toString().trim()
            if (teksJumlah.isEmpty()) return@setPositiveButton

            val stokAwalBaru = teksJumlah.toLong()
            val stokTotalBaru = stokAwalBaru + item.stok_tambahan
            val sisaGudangBaru = stokTotalBaru - item.stok_dialokasikan

            if (sisaGudangBaru < 0) {
                Toast.makeText(requireContext(), "Jumlah total tidak boleh di bawah jatah Rider yang sudah dibawa keliling!", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            mFirestore.collection("stok_gudang").document(docIdGudang)
                .update(
                    "detail_gudang.${item.id_produk}.stok_masuk_awal", stokAwalBaru,
                    "detail_gudang.${item.id_produk}.stok_total", stokTotalBaru,
                    "detail_gudang.${item.id_produk}.sisa_gudang", sisaGudangBaru,
                    "last_updated", Timestamp.now()
                ).addOnSuccessListener {
                    posisiTerbuka = -1
                    dialog.dismiss()
                }
        }
        dialogBuilder.setNegativeButton("Batal") { dialog, _ -> dialog.cancel() }
        dialogBuilder.show()
    }

    private fun tampilkanDialogHapusStok(item: StokGudang) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hapus Item")
            .setMessage("Hapus ${item.nama_produk} dari logistik tanggal ini?")
            .setPositiveButton("Hapus") { dialog, _ ->
                mFirestore.collection("stok_gudang").document(docIdGudang)
                    .update(
                        "detail_gudang.${item.id_produk}.stok_masuk_awal", 0L,
                        "detail_gudang.${item.id_produk}.sisa_gudang", 0L,
                        "detail_gudang.${item.id_produk}.stok_total", 0L,
                        "last_updated", Timestamp.now()
                    )
                    .addOnSuccessListener {
                        posisiTerbuka = -1
                        dialog.dismiss()
                    }
            }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun formatKeTanggalIndo(tanggal: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = parser.parse(tanggal)
            val formatter = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
            if (date != null) formatter.format(date) else tanggal
        } catch (e: Exception) {
            tanggal
        }
    }

    private fun dpToPx(context: Context, dp: Int): Int = (dp * context.resources.displayMetrics.density).toInt()

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    inner class DetailAdapter(private val data: List<StokGudang>) : RecyclerView.Adapter<DetailAdapter.ViewHolder>() {
        inner class ViewHolder(val b: ItemDetailGudangBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(p: ViewGroup, vt: Int): ViewHolder {
            return ViewHolder(ItemDetailGudangBinding.inflate(LayoutInflater.from(p.context), p, false))
        }

        override fun onBindViewHolder(vh: ViewHolder, pos: Int) {
            val item = data[pos]
            vh.b.tvDetailNamaKopi.text = item.nama_produk
            vh.b.tvMasukAwal.text = "Awal: ${item.stok_masuk_awal} Cup"
            vh.b.tvTambahanStok.text = "Tambahan: ${item.stok_tambahan} Cup"
            vh.b.tvStokTotalKumulatif.text = "Total: ${item.stok_total} Cup"
            vh.b.tvDialokasikan.text = "Diambil: ${item.stok_dialokasikan} Cup"
            vh.b.tvSisaGudang.text = "Sisa: ${item.sisa_gudang} Cup"

            val formatRupiah = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            vh.b.tvDetailHargaKopi.text = formatRupiah.format(item.harga_jual).replace(",00", "")

            val isExpanded = (pos == posisiTerbuka)
            vh.b.layoutAksi.visibility = if (isExpanded) View.VISIBLE else View.GONE
            vh.b.viewDividerAksi.visibility = if (isExpanded) View.VISIBLE else View.GONE

            vh.b.root.setOnClickListener {
                val posisiLama = posisiTerbuka
                posisiTerbuka = if (posisiTerbuka == pos) -1 else pos
                notifyItemChanged(posisiLama)
                notifyItemChanged(pos)
            }

            vh.b.btnEditItem.setOnClickListener { tampilkanDialogEditStok(item) }
            vh.b.btnHapusItem.setOnClickListener { tampilkanDialogHapusStok(item) }
        }

        override fun getItemCount(): Int = data.size
    }
}