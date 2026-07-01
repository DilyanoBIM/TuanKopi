package com.example.tuankopi.distribusi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tuankopi.databinding.ActivityDetailDistribusiRiderBinding
import com.example.tuankopi.databinding.ItemDetailGudangBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import java.text.NumberFormat
import java.util.Locale
import kotlin.collections.get
import kotlin.collections.iterator

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

class DetailDistribusiRiderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailDistribusiRiderBinding
    private lateinit var mFirestore: FirebaseFirestore
    private var targetTanggal = ""
    private var riderUid = ""
    private var riderNama = ""
    private var cleanedTgl = ""

    private var posisiTerbuka: Int = -1
    private var listMuatanLokal = ArrayList<ItemMuatanRider>()
    private lateinit var mAdapter: MuatanAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailDistribusiRiderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mFirestore = FirebaseFirestore.getInstance()
        targetTanggal = intent.getStringExtra("TARGET_TANGGAL") ?: ""
        riderUid = intent.getStringExtra("RIDER_UID") ?: ""
        riderNama = intent.getStringExtra("RIDER_NAMA") ?: ""

        cleanedTgl = targetTanggal.replace("-", "")

        // Set judul utama halaman
        supportActionBar?.title = "Muatan: $riderNama"

        setupRecyclerView()

        binding.fabTambahJatahSuplai.setOnClickListener {
            val intent = Intent(this, InputSuplaiRiderActivity::class.java)
            intent.putExtra("TARGET_TANGGAL", targetTanggal)
            intent.putExtra("RIDER_UID", riderUid)
            intent.putExtra("RIDER_NAMA", riderNama)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        muatStokHarianRiderLive()
    }

    private fun setupRecyclerView() {
        mAdapter = MuatanAdapter(listMuatanLokal)
        binding.rvDetailDistribusiRider.layoutManager = LinearLayoutManager(this)
        binding.rvDetailDistribusiRider.adapter = mAdapter
    }

    private fun muatStokHarianRiderLive() {
        val docId = "${cleanedTgl}_$riderUid"
        mFirestore.collection("stok_harian").document(docId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {

                    // ────────────────────────────────────────────────────────────────────────
                    // PEMBARUAN: Ambil data modal_kembalian harian lalu render ke Header Sub-Title
                    // ────────────────────────────────────────────────────────────────────────
                    val cashAwal = snapshot.getLong("modal_kembalian") ?: 0L
                    val formatterRupiah = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
                    val textModalFormat = formatterRupiah.format(cashAwal).replace(",00", "")

                    // Kita tempelkan informasi modal kembalian dinamis ini ke sub-header layout XML
                    binding.tvHeaderDetailDist.text = "Muatan $riderNama — Modal Kembalian: $textModalFormat"

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
                                    idProduk = idProd,
                                    namaProduk = namaProd,
                                    stokAwal = awal,
                                    stokTambahan = tambahan,
                                    totalStok = total,
                                    terjual = laku,
                                    sisaStok = sisa,
                                    hargaJual = harga
                                )
                            )
                        }
                    }
                    mAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun tampilkanDialogEditJatahRider(idProd: String, namaProd: String, stokAwalLama: Long) {
        val context = this
        val dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder.setTitle("Edit Jatah Awal Keliling")
        dialogBuilder.setMessage("Ubah kuota cup jatah subuh di motor Rider untuk $namaProd:")

        val input = EditText(context)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.setText(stokAwalLama.toString())
        input.setSelection(input.text.length)

        val container = LinearLayout(context)
        container.setPadding(dpToPx(context, 20), dpToPx(context, 4), dpToPx(context, 20), dpToPx(context, 4))
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
                Toast.makeText(context, "Jatah awal $namaProd berhasil direvisi!", Toast.LENGTH_SHORT).show()
                posisiTerbuka = -1
                dialog.dismiss()
            }.addOnFailureListener { e ->
                Toast.makeText(context, "Gagal mengupdate jatah: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        dialogBuilder.setNegativeButton("Batal") { dialog, _ -> dialog.cancel() }
        dialogBuilder.create().show()
    }

    private fun tampilkanDialogHapusJatahRider(idProd: String, namaProd: String, stokAwalLama: Long) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Jatah Muatan")
            .setMessage("Hapus $namaProd dari motor Rider? Seluruh kuota ($stokAwalLama cup) akan otomatis dikembalikan ke sisa gudang rumah Owner.")
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

                    val detailGudangMap = snapshotGudang.get("detail_gudang") as? Map<*, *> ?: HashMap<String, Any>()
                    val detailGudangTerbarui = HashMap<String, Any>()
                    for ((k, v) in detailGudangMap) detailGudangTerbarui[k.toString()] = v!!

                    val dataProdukGudang = detailGudangMap[idProd] as? Map<*, *>
                    if (dataProdukGudang != null) {
                        val masukAwalGudang = dataProdukGudang["stok_masuk_awal"] as? Long ?: 0L
                        val alokasiGudangLama = dataProdukGudang["stok_dialokasikan"] as? Long ?: 0L
                        val sisaGudangLama = dataProdukGudang["sisa_gudang"] as? Long ?: 0L

                        val alokasiGudangBaru = alokasiGudangLama - stokAwalLama
                        val sisaGudangBaru = sisaGudangLama + stokAwalLama

                        detailGudangTerbarui[idProd] = hashMapOf(
                            "id_produk" to idProd,
                            "nama_produk" to dataProdukGudang["nama_produk"],
                            "harga_jual" to dataProdukGudang["harga_jual"],
                            "stok_masuk_awal" to masukAwalGudang,
                            "stok_dialokasikan" to alokasiGudangBaru,
                            "sisa_gudang" to sisaGudangBaru,
                            "stok_total" to (dataProdukGudang["stok_total"] as? Long ?: masukAwalGudang)
                        )
                    }

                    val detailStokRiderMap = snapshotRider.get("detail_stok") as? Map<*, *> ?: HashMap<String, Any>()
                    val detailStokRiderTerbarui = HashMap<String, Any>()
                    for ((k, v) in detailStokRiderMap) detailStokRiderTerbarui[k.toString()] = v!!

                    detailStokRiderTerbarui.remove(idProd)

                    transaction.update(refGudangTunggal, "detail_gudang", detailGudangTerbarui)
                    transaction.update(refRider, "detail_stok", detailStokRiderTerbarui)

                    null
                }.addOnSuccessListener {
                    Toast.makeText(this, "$namaProd dicopot dari muatan motor Rider.", Toast.LENGTH_SHORT).show()
                    posisiTerbuka = -1
                    dialog.dismiss()
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Gagal menghapus jatah: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .create().show()
    }

    private fun dpToPx(context: Context, dp: Int): Int = (dp * context.resources.displayMetrics.density).toInt()

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

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
                tampilkanDialogHapusJatahRider(item.idProduk, item.namaProduk, item.stokAwal)
            }
        }
        override fun getItemCount(): Int = data.size
    }
}