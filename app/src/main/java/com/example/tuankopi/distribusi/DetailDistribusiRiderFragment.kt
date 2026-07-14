package com.example.tuankopi.distribusi

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tuankopi.OwnerDashboardActivity
import com.example.tuankopi.databinding.FragmentDetailDistribusiRiderBinding
import com.example.tuankopi.databinding.ItemDetailGudangBinding
import com.example.tuankopi.databinding.ItemSesiDistribusiBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import java.text.NumberFormat
import java.util.Locale

data class ItemMuatanRider(
    val idProduk: String,
    val namaProduk: String,
    val stokAwal: Long,
    val stokTambahan: Long = 0L,
    val totalStok: Long = 0L,
    val terjual: Long = 0L,
    val sisaStok: Long = 0L,
    val hargaJual: Long = 0L
)

// PENYESUAIAN BARU: representasi 1 item di dalam 1 sesi distribusi (histori, bukan kumulatif)
data class ItemSuplaiSesi(
    val idProduk: String,
    val namaProduk: String,
    val qty: Long,
    val hargaJual: Long
)

// PENYESUAIAN BARU: representasi 1 sesi distribusi lengkap dengan status diterima/pending
data class SesiDistribusi(
    val sesiKe: Long,
    val modal: Long,
    val status: String, // "PENDING" atau "DITERIMA"
    val items: List<ItemSuplaiSesi>
)

class DetailDistribusiRiderFragment : Fragment() {

    private var _binding: FragmentDetailDistribusiRiderBinding? = null
    private val binding get() = _binding!!
    private lateinit var mFirestore: FirebaseFirestore
    private var targetTanggal = ""
    private var riderUid = ""
    private var riderNama = ""
    private var cleanedTgl = ""

    private var posisiTerbuka: Int = -1
    private var listMuatanLokal = ArrayList<ItemMuatanRider>()
    private lateinit var mAdapter: MuatanAdapter

    // PENYESUAIAN BARU: list & adapter untuk riwayat sesi distribusi
    private var listSesiLokal = ArrayList<SesiDistribusi>()
    private lateinit var mAdapterSesi: SesiAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailDistribusiRiderBinding.inflate(inflater, container, false)

        targetTanggal = arguments?.getString("TARGET_TANGGAL") ?: ""
        riderUid = arguments?.getString("RIDER_UID") ?: ""
        riderNama = arguments?.getString("RIDER_NAMA") ?: ""
        cleanedTgl = targetTanggal.replace("-", "")

        mFirestore = FirebaseFirestore.getInstance()
        setupRecyclerView()

        binding.fabTambahJatahSuplai.setOnClickListener {
            val fragment = InputSuplaiRiderFragment().apply {
                arguments = Bundle().apply {
                    putString("TARGET_TANGGAL", targetTanggal)
                    putString("RIDER_UID", riderUid)
                    putString("RIDER_NAMA", riderNama)
                }
            }
            (requireActivity() as OwnerDashboardActivity).bukaHalaman(fragment, "Bagi Jatah: $riderNama")
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        muatStokHarianRiderLive()
    }

    private fun setupRecyclerView() {
        mAdapter = MuatanAdapter(listMuatanLokal)
        binding.rvDetailDistribusiRider.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDetailDistribusiRider.adapter = mAdapter

        // PENYESUAIAN BARU: recyclerview riwayat sesi (butuh id rvSesiDistribusi di layout)
        mAdapterSesi = SesiAdapter(listSesiLokal)
        binding.rvSesiDistribusi.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSesiDistribusi.adapter = mAdapterSesi
    }

    private fun muatStokHarianRiderLive() {
        val docId = "${cleanedTgl}_$riderUid"
        mFirestore.collection("stok_harian").document(docId)
            .addSnapshotListener { snapshot, _ ->
                if (_binding != null && snapshot != null && snapshot.exists()) {

                    val cashAwal = snapshot.getLong("modal_kembalian") ?: 0L
                    val formatterRupiah = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
                    val textModalFormat = formatterRupiah.format(cashAwal).replace(",00", "")

                    val sesiTerakhir = snapshot.getLong("sesi_terakhir") ?: 0L

                    // PENYESUAIAN BARU: parsing riwayat_suplai (map histori per sesi, TIDAK dihapus saat diterima)
                    listSesiLokal.clear()
                    val riwayatMap = snapshot.get("riwayat_suplai") as? Map<*, *>
                    if (riwayatMap != null) {
                        for ((_, v) in riwayatMap) {
                            val sesiData = v as? Map<*, *> ?: continue
                            val sesiKe = sesiData["sesi_ke"] as? Long ?: 0L
                            val modalSesi = sesiData["modal"] as? Long ?: 0L
                            val statusSesi = sesiData["status"] as? String ?: "PENDING"
                            val itemsRawSesi = sesiData["items"] as? Map<*, *> ?: emptyMap<Any, Any>()

                            val itemsSesi = ArrayList<ItemSuplaiSesi>()
                            for ((ik, iv) in itemsRawSesi) {
                                val idP = ik as? String ?: continue
                                val dataItem = iv as? Map<*, *> ?: continue
                                itemsSesi.add(
                                    ItemSuplaiSesi(
                                        idProduk = idP,
                                        namaProduk = dataItem["nama_produk"] as? String ?: "Kopi",
                                        qty = dataItem["qty"] as? Long ?: 0L,
                                        hargaJual = dataItem["harga_jual"] as? Long ?: 0L
                                    )
                                )
                            }
                            listSesiLokal.add(SesiDistribusi(sesiKe, modalSesi, statusSesi, itemsSesi))
                        }
                        listSesiLokal.sortByDescending { it.sesiKe }
                    }
                    val adaPending = listSesiLokal.any { it.status == "PENDING" }
                    mAdapterSesi.notifyDataSetChanged()

                    var teksHeader = "Muatan $riderNama (Sesi Aktif: $sesiTerakhir) — Modal: $textModalFormat"
                    if (adaPending) {
                        teksHeader += "\n⚠️ Ada jatah baru yang belum diterima Rider"
                    }
                    binding.tvHeaderDetailDist.text = teksHeader

                    listMuatanLokal.clear()
                    val rawMap = snapshot.get("detail_stok") as? Map<*, *>
                    if (rawMap != null) {
                        for ((key, value) in rawMap) {
                            val idProd = key as? String ?: continue
                            val dataMap = value as? Map<*, *> ?: continue
                            val namaProd = dataMap["nama_produk"] as? String ?: "Kopi"
                            val awal = (dataMap["stok_awal"] as? Long) ?: 0L
                            val tambahan = (dataMap["stok_tambahan"] as? Long) ?: 0L
                            val total = (dataMap["total_stok"] as? Long) ?: (awal + tambahan)
                            val laku = (dataMap["terjual"] as? Long) ?: 0L
                            val sisa = (dataMap["sisa_stok"] as? Long) ?: (total - laku)
                            val harga = (dataMap["harga_jual"] as? Long) ?: 0L

                            listMuatanLokal.add(
                                ItemMuatanRider(
                                    idProduk = idProd, namaProduk = namaProd, stokAwal = awal,
                                    stokTambahan = tambahan, totalStok = total, terjual = laku,
                                    sisaStok = sisa, hargaJual = harga
                                )
                            )
                        }
                    }
                    mAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun tampilkanDialogEditJatahRider(idProd: String, namaProd: String, stokAwalLama: Long) {
        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setTitle("Edit Jatah Awal Keliling")
        dialogBuilder.setMessage("Ubah kuota cup jatah subuh di motor Rider untuk $namaProd:")

        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.setText(stokAwalLama.toString())
        input.setSelection(input.text.length)

        val container = LinearLayout(requireContext())
        container.setPadding(dpToPx(requireContext(), 20), dpToPx(requireContext(), 4), dpToPx(requireContext(), 20), dpToPx(requireContext(), 4))
        container.addView(input)
        dialogBuilder.setView(container)

        dialogBuilder.setPositiveButton("Simpan") { dialog, _ ->
            val txt = input.text.toString().trim()
            if (txt.isEmpty()) return@setPositiveButton
            val stokAwalBaru = txt.toLong()

            if (stokAwalBaru == stokAwalLama) {
                dialog.dismiss()
                return@setPositiveButton
            }

            val riderDocId = "${cleanedTgl}_$riderUid"
            val refGudangTunggal = mFirestore.collection("stok_gudang").document(cleanedTgl)
            val refRider = mFirestore.collection("stok_harian").document(riderDocId)

            mFirestore.runTransaction { transaction ->
                val snapshotGudang = transaction.get(refGudangTunggal)
                val snapshotRider = transaction.get(refRider)

                if (!snapshotGudang.exists() || !snapshotRider.exists()) {
                    throw FirebaseFirestoreException("Dokumen Logistik Tidak Lengkap!", FirebaseFirestoreException.Code.NOT_FOUND)
                }

                val detailGudangMap = snapshotGudang.get("detail_gudang") as? Map<*, *> ?: HashMap<String, Any>()
                val detailGudangTerbarui = HashMap<String, Any>()
                for ((k, v) in detailGudangMap) detailGudangTerbarui[k.toString()] = v!!

                val dataProdukGudang = detailGudangMap[idProd] as? Map<*, *>
                    ?: throw FirebaseFirestoreException("Produk hilang di gudang utama!", FirebaseFirestoreException.Code.NOT_FOUND)

                val masukAwalGudang = dataProdukGudang["stok_masuk_awal"] as? Long ?: 0L
                val alokasiGudangLama = dataProdukGudang["stok_dialokasikan"] as? Long ?: 0L
                val sisaGudangLama = dataProdukGudang["sisa_gudang"] as? Long ?: 0L

                val selisih = stokAwalBaru - stokAwalLama

                if (selisih > 0 && selisih > sisaGudangLama) {
                    throw FirebaseFirestoreException("Sisa kopi di rumah Owner tidak mencukupi!", FirebaseFirestoreException.Code.ABORTED)
                }

                val alokasiGudangBaru = alokasiGudangLama + selisih
                val sisaGudangBaru = sisaGudangLama - selisih

                detailGudangTerbarui[idProd] = hashMapOf(
                    "id_produk" to idProd,
                    "nama_produk" to dataProdukGudang["nama_produk"],
                    "harga_jual" to dataProdukGudang["harga_jual"],
                    "stok_masuk_awal" to masukAwalGudang,
                    "stok_tambahan" to (dataProdukGudang["stok_tambahan"] as? Long ?: 0L),
                    "stok_dialokasikan" to alokasiGudangBaru,
                    "sisa_gudang" to sisaGudangBaru,
                    "stok_total" to (dataProdukGudang["stok_total"] as? Long ?: masukAwalGudang)
                )

                val detailStokRiderMap = snapshotRider.get("detail_stok") as? Map<*, *> ?: HashMap<String, Any>()
                val detailStokRiderTerbarui = HashMap<String, Any>()
                for ((k, v) in detailStokRiderMap) detailStokRiderTerbarui[k.toString()] = v!!

                val dataProdukRider = detailStokRiderMap[idProd] as? Map<*, *>
                    ?: throw FirebaseFirestoreException("Produk hilang di data rider!", FirebaseFirestoreException.Code.NOT_FOUND)

                val stokTambahanRider = dataProdukRider["stok_tambahan"] as? Long ?: 0L
                val terjualRider = dataProdukRider["terjual"] as? Long ?: 0L
                val statusDiterimaLama = dataProdukRider["diterima"] as? Boolean ?: false

                val totalRiderBaru = stokAwalBaru + stokTambahanRider
                val sisaStokRiderBaru = totalRiderBaru - terjualRider

                if (sisaStokRiderBaru < 0) {
                    throw FirebaseFirestoreException("Edit gagal! Total baru di bawah kuota terjual!", FirebaseFirestoreException.Code.ABORTED)
                }

                detailStokRiderTerbarui[idProd] = hashMapOf(
                    "id_produk" to idProd,
                    "nama_produk" to dataProdukRider["nama_produk"],
                    "stok_awal" to stokAwalBaru,
                    "stok_tambahan" to stokTambahanRider,
                    "total_stok" to totalRiderBaru,
                    "terjual" to terjualRider,
                    "sisa_stok" to sisaStokRiderBaru,
                    "harga_jual" to (dataProdukRider["harga_jual"] as? Long ?: 0L),
                    "diterima" to statusDiterimaLama
                )

                transaction.update(refGudangTunggal, "detail_gudang", detailGudangTerbarui)
                transaction.update(refRider, "detail_stok", detailStokRiderTerbarui)

                null
            }.addOnSuccessListener {
                Toast.makeText(requireContext(), "Jatah awal $namaProd berhasil direvisi!", Toast.LENGTH_SHORT).show()
                posisiTerbuka = -1
                dialog.dismiss()
            }.addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Gagal mengupdate jatah: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        dialogBuilder.setNegativeButton("Batal") { dialog, _ -> dialog.cancel() }
        dialogBuilder.create().show()
    }

    private fun tampilkanDialogHapusJatahRider(idProd: String, namaProd: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Jatah Muatan")
            .setMessage("Hapus $namaProd dari motor Rider? Seluruh kuota aktif (termasuk suplai tambahan) akan otomatis dikembalikan ke sisa gudang rumah Owner.")
            .setPositiveButton("Hapus") { dialog, _ ->
                val riderDocId = "${cleanedTgl}_$riderUid"
                val refGudangTunggal = mFirestore.collection("stok_gudang").document(cleanedTgl)
                val refRider = mFirestore.collection("stok_harian").document(riderDocId)

                mFirestore.runTransaction { transaction ->
                    val snapshotGudang = transaction.get(refGudangTunggal)
                    val snapshotRider = transaction.get(refRider)

                    if (!snapshotGudang.exists() || !snapshotRider.exists()) {
                        throw FirebaseFirestoreException("Dokumen tidak lengkap!", FirebaseFirestoreException.Code.NOT_FOUND)
                    }

                    val detailStokRiderMap = snapshotRider.get("detail_stok") as? Map<*, *> ?: HashMap<String, Any>()
                    val detailStokRiderTerbarui = HashMap<String, Any>()
                    for ((k, v) in detailStokRiderMap) detailStokRiderTerbarui[k.toString()] = v!!

                    val dataProdukRider = detailStokRiderMap[idProd] as? Map<*, *>

                    val totalRiderLama = dataProdukRider?.get("total_stok") as? Long
                        ?: ((dataProdukRider?.get("stok_awal") as? Long ?: 0L) + (dataProdukRider?.get("stok_tambahan") as? Long ?: 0L))

                    val detailGudangMap = snapshotGudang.get("detail_gudang") as? Map<*, *> ?: HashMap<String, Any>()
                    val detailGudangTerbarui = HashMap<String, Any>()
                    for ((k, v) in detailGudangMap) detailGudangTerbarui[k.toString()] = v!!

                    val dataProdukGudang = detailGudangMap[idProd] as? Map<*, *>
                    if (dataProdukGudang != null) {
                        val masukAwalGudang = dataProdukGudang["stok_masuk_awal"] as? Long ?: 0L
                        val alokasiGudangLama = dataProdukGudang["stok_dialokasikan"] as? Long ?: 0L
                        val sisaGudangLama = dataProdukGudang["sisa_gudang"] as? Long ?: 0L

                        val alokasiGudangBaru = alokasiGudangLama - totalRiderLama
                        val sisaGudangBaru = sisaGudangLama + totalRiderLama

                        detailGudangTerbarui[idProd] = hashMapOf(
                            "id_produk" to idProd,
                            "nama_produk" to dataProdukGudang["nama_produk"],
                            "harga_jual" to dataProdukGudang["harga_jual"],
                            "stok_masuk_awal" to masukAwalGudang,
                            "stok_tambahan" to (dataProdukGudang["stok_tambahan"] as? Long ?: 0L),
                            "stok_dialokasikan" to alokasiGudangBaru,
                            "sisa_gudang" to sisaGudangBaru,
                            "stok_total" to (dataProdukGudang["stok_total"] as? Long ?: masukAwalGudang)
                        )
                    }

                    detailStokRiderTerbarui.remove(idProd)

                    transaction.update(refGudangTunggal, "detail_gudang", detailGudangTerbarui)
                    transaction.update(refRider, "detail_stok", detailStokRiderTerbarui)

                    null
                }.addOnSuccessListener {
                    Toast.makeText(requireContext(), "$namaProd dicopot dari muatan motor Rider.", Toast.LENGTH_SHORT).show()
                    posisiTerbuka = -1
                    dialog.dismiss()
                }.addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Gagal menghapus jatah: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .create().show()
    }

    private fun dpToPx(context: Context, dp: Int): Int = (dp * context.resources.displayMetrics.density).toInt()

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    inner class MuatanAdapter(private val data: List<ItemMuatanRider>) : RecyclerView.Adapter<MuatanAdapter.ViewHolder>() {
        inner class ViewHolder(val b: ItemDetailGudangBinding) : RecyclerView.ViewHolder(b.root)
        override fun onCreateViewHolder(p: ViewGroup, vt: Int): ViewHolder =
            ViewHolder(ItemDetailGudangBinding.inflate(LayoutInflater.from(p.context), p, false))

        override fun onBindViewHolder(vh: ViewHolder, pos: Int) {
            val item = data[pos]

            vh.b.tvDetailNamaKopi.text = item.namaProduk
            vh.b.tvMasukAwal.text = "Awal: ${item.stokAwal} Cup"
            vh.b.tvTambahanStok.text = "Tambahan: ${item.stokTambahan} Cup"
            vh.b.tvStokTotalKumulatif.text = "Total: ${item.totalStok} Cup"

            vh.b.tvDialokasikan.text = "Terjual: ${item.terjual} Cup"
            vh.b.tvDialokasikan.visibility = View.VISIBLE

            vh.b.tvSisaGudang.text = "Sisa: ${item.sisaStok} Cup"
            vh.b.tvSisaGudang.visibility = View.VISIBLE

            val formatRupiah = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            vh.b.tvDetailHargaKopi.text = formatRupiah.format(item.hargaJual).replace(",00", "")
            vh.b.tvDetailHargaKopi.visibility = View.VISIBLE

            val isExpanded = (pos == posisiTerbuka)
            vh.b.layoutAksi.visibility = if (isExpanded) View.VISIBLE else View.GONE
            vh.b.viewDividerAksi.visibility = if (isExpanded) View.VISIBLE else View.GONE

            vh.b.root.setOnClickListener {
                val posisiLama = posisiTerbuka
                posisiTerbuka = if (posisiTerbuka == pos) -1 else pos
                notifyItemChanged(posisiLama)
                notifyItemChanged(pos)
            }

            vh.b.btnEditItem.setOnClickListener {
                tampilkanDialogEditJatahRider(item.idProduk, item.namaProduk, item.stokAwal)
            }

            vh.b.btnHapusItem.setOnClickListener {
                tampilkanDialogHapusJatahRider(item.idProduk, item.namaProduk)
            }
        }
        override fun getItemCount(): Int = data.size
    }

    // PENYESUAIAN BARU: adapter untuk kartu riwayat tiap sesi distribusi (histori, read-only bagi Owner)
    inner class SesiAdapter(private val data: List<SesiDistribusi>) :
        RecyclerView.Adapter<SesiAdapter.ViewHolder>() {

        inner class ViewHolder(val b: ItemSesiDistribusiBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(p: ViewGroup, vt: Int): ViewHolder =
            ViewHolder(ItemSesiDistribusiBinding.inflate(LayoutInflater.from(p.context), p, false))

        override fun onBindViewHolder(vh: ViewHolder, pos: Int) {
            val sesi = data[pos]
            val formatRupiah = NumberFormat.getCurrencyInstance(Locale("in", "ID"))

            vh.b.tvSesiJudul.text = "Distribusi Sesi ${sesi.sesiKe}"

            if (sesi.status == "DITERIMA") {
                vh.b.tvSesiStatus.text = "DITERIMA"
                vh.b.tvSesiStatus.setBackgroundColor(android.graphics.Color.parseColor("#2E7D32"))
            } else {
                vh.b.tvSesiStatus.text = "PENDING"
                vh.b.tvSesiStatus.setBackgroundColor(android.graphics.Color.parseColor("#F9A825"))
            }

            vh.b.tvSesiModal.text = "Modal Kembalian: ${formatRupiah.format(sesi.modal).replace(",00", "")}"

            val teksItem = if (sesi.items.isEmpty()) {
                "- (tidak ada produk pada sesi ini)"
            } else {
                sesi.items.joinToString("\n") { "- ${it.namaProduk}: ${it.qty} Cup" }
            }
            vh.b.tvSesiItems.text = teksItem
        }

        override fun getItemCount(): Int = data.size
    }
}