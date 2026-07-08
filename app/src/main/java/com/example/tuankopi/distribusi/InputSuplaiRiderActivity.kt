package com.example.tuankopi.distribusi

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tuankopi.StokGudang
import com.example.tuankopi.databinding.ActivityInputSuplaiRiderBinding
import com.example.tuankopi.databinding.ItemPilihProdukGudangBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import java.text.NumberFormat
import java.util.Locale

class InputSuplaiRiderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInputSuplaiRiderBinding
    private lateinit var mFirestore: FirebaseFirestore
    private var listStokGudangPusat = ArrayList<StokGudang>()
    private lateinit var mAdapter: SuplaiAdapter

    private var targetTanggal = ""
    private var riderUid = ""
    private var riderNama = ""
    private var cleanedTgl = ""

    private val kuotaAlokasiBaru = HashMap<String, Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInputSuplaiRiderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        targetTanggal = intent.getStringExtra("TARGET_TANGGAL") ?: ""
        riderUid = intent.getStringExtra("RIDER_UID") ?: ""
        riderNama = intent.getStringExtra("RIDER_NAMA") ?: ""

        cleanedTgl = targetTanggal.replace("-", "")

        setSupportActionBar(binding.customToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Bagi Jatah: $riderNama"

        ViewCompat.setOnApplyWindowInsetsListener(binding.customToolbar) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, insets.top, 0, 0)
            windowInsets
        }

        binding.tvHeaderPilih.text = "Ambil Produk & Atur Modal Awal"
        binding.btnSimpanBatchGudang.text = "KONFIRMASI DISTRIBUSI MOTOR"

        mFirestore = FirebaseFirestore.getInstance()

        setupRecyclerView()
        muatStokTersediaGudangPusatMap()

        binding.btnSimpanBatchGudang.setOnClickListener {
            eksekusiTransaksiSuplaiHilir()
        }
    }

    private fun setupRecyclerView() {
        mAdapter = SuplaiAdapter(listStokGudangPusat) { idProd, qty ->
            if (qty > 0) kuotaAlokasiBaru[idProd] = qty else kuotaAlokasiBaru.remove(idProd)
        }
        binding.rvPilihProduk.layoutManager = LinearLayoutManager(this)
        binding.rvPilihProduk.adapter = mAdapter
    }

    private fun muatStokTersediaGudangPusatMap() {
        if (cleanedTgl.isEmpty()) return

        mFirestore.collection("stok_gudang").document(cleanedTgl).get()
            .addOnSuccessListener { docGudang ->
                if (docGudang != null && docGudang.exists()) {
                    listStokGudangPusat.clear()

                    listStokGudangPusat.add(
                        StokGudang(
                            id_produk = "KEY_MODAL_CASH",
                            nama_produk = "💰 MODAL CASH AWAL KEMBALIAN",
                            harga_jual = 0L,
                            sisa_gudang = 999999L,
                            stok_masuk_awal = 1L
                        )
                    )

                    kuotaAlokasiBaru["KEY_MODAL_CASH"] = 50000L

                    val rawMapDetail = docGudang.get("detail_gudang") as? Map<*, *>
                    val listProdukLokal = ArrayList<StokGudang>()

                    if (rawMapDetail != null) {
                        for ((_, value) in rawMapDetail) {
                            val dataKopi = value as? Map<*, *> ?: continue

                            val idProd = dataKopi["id_produk"] as? String ?: ""
                            val namaProd = dataKopi["nama_produk"] as? String ?: "Menu"
                            val harga = dataKopi["harga_jual"] as? Long ?: 0L
                            val awalGudang = dataKopi["stok_masuk_awal"] as? Long ?: 0L
                            val alokasiGudang = dataKopi["stok_dialokasikan"] as? Long ?: 0L
                            val sisaGudang = dataKopi["sisa_gudang"] as? Long ?: 0L

                            if (awalGudang > 0L) {
                                listProdukLokal.add(
                                    StokGudang(
                                        id_gudang = "GUD_$cleanedTgl",
                                        tanggal = targetTanggal,
                                        id_produk = idProd,
                                        nama_produk = namaProd,
                                        stok_masuk_awal = awalGudang,
                                        stok_dialokasikan = alokasiGudang,
                                        sisa_gudang = sisaGudang,
                                        harga_jual = harga
                                    )
                                )
                            }
                        }
                    }

                    listProdukLokal.sortBy { it.nama_produk }
                    listStokGudangPusat.addAll(listProdukLokal)

                    mAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "Owner belum mengisi slot produksi awal di menu Stok Gudang!", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun eksekusiTransaksiSuplaiHilir() {
        val nominalModalTerinput = kuotaAlokasiBaru["KEY_MODAL_CASH"] ?: 0L

        val kuotaKopiMurni = HashMap(kuotaAlokasiBaru)
        kuotaKopiMurni.remove("KEY_MODAL_CASH")

        if (kuotaKopiMurni.isEmpty()) {
            Toast.makeText(this, "Mohon isi kuota cup kopi yang akan dibawa keliling!", Toast.LENGTH_SHORT).show()
            return
        }

        val sbRingkasan = StringBuilder()
        val formatterRupiah = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        val textModalFormat = formatterRupiah.format(nominalModalTerinput).replace(",00", "")

        sbRingkasan.append("📋 RINCIAN DISTRIBUSI RIDER:\n")
        sbRingkasan.append("Nama Rider: $riderNama\n")
        sbRingkasan.append("Modal Cash Awal: $textModalFormat\n\n")
        sbRingkasan.append("☕ DAFTAR MUATAN CUP KOPI:\n")

        var nomorUrut = 1
        for ((idProd, qtyBawa) in kuotaKopiMurni) {
            val namaProdukAsli = listStokGudangPusat.find { it.id_produk == idProd }?.nama_produk ?: "Menu Kopi"
            sbRingkasan.append("${nomorUrut++}. $namaProdukAsli -> $qtyBawa Cup\n")
        }
        sbRingkasan.append("\nApakah seluruh data logistik di atas sudah sesuai?")

        val context = this
        AlertDialog.Builder(context).apply {
            setTitle("Konfirmasi Distribusi Motor")
            setMessage(sbRingkasan.toString())
            setCancelable(false)
            setPositiveButton("Ya, Kirim Jatah") { dialog, _ ->
                dialog.dismiss()
                eksekusiSimpanKeFirestoreTransaksional(nominalModalTerinput, kuotaKopiMurni)
            }
            setNegativeButton("Cek Kembali") { dialog, _ ->
                dialog.dismiss()
            }
            create()
            show()
        }
    }

    private fun eksekusiSimpanKeFirestoreTransaksional(nominalModalTerinput: Long, kuotaKopiMurni: HashMap<String, Long>) {
        val riderDocId = "${cleanedTgl}_$riderUid"
        val refGudangTunggal = mFirestore.collection("stok_gudang").document(cleanedTgl)
        val refRider = mFirestore.collection("stok_harian").document(riderDocId)

        mFirestore.runTransaction { transaction ->
            val snapshotGudang = transaction.get(refGudangTunggal)
            if (!snapshotGudang.exists()) {
                throw FirebaseFirestoreException("Dokumen stok gudang pusat tidak ditemukan!", FirebaseFirestoreException.Code.NOT_FOUND)
            }

            val rawDetailGudang = snapshotGudang.get("detail_gudang") as? Map<*, *> ?: HashMap<String, Any>()
            val detailGudangTerbarui = HashMap<String, Any>()
            for ((k, v) in rawDetailGudang) {
                detailGudangTerbarui[k.toString()] = v!!
            }

            val snapshotRider = transaction.get(refRider)
            val detailStokRiderTerbarui = HashMap<String, Any>()

            if (snapshotRider.exists()) {
                val rawDetailStokRiderLama = snapshotRider.get("detail_stok") as? Map<*, *>
                if (rawDetailStokRiderLama != null) {
                    for ((k, v) in rawDetailStokRiderLama) {
                        detailStokRiderTerbarui[k.toString()] = v!!
                    }
                }
            }

            for ((idProd, qtyBawa) in kuotaKopiMurni) {
                val dataProdukGudangLama = rawDetailGudang[idProd] as? Map<*, *>
                    ?: throw FirebaseFirestoreException("Produk dengan ID $idProd tidak ditemukan di gudang pusat!", FirebaseFirestoreException.Code.NOT_FOUND)

                val namaProd = dataProdukGudangLama["nama_produk"] as? String ?: "Menu Kopi"
                val hargaJual = dataProdukGudangLama["harga_jual"] as? Long ?: 0L
                val masukAwalGudang = dataProdukGudangLama["stok_masuk_awal"] as? Long ?: 0L
                val alokasiGudangLama = dataProdukGudangLama["stok_dialokasikan"] as? Long ?: 0L
                val sisaGudangLama = dataProdukGudangLama["sisa_gudang"] as? Long
                    ?: ((dataProdukGudangLama["stok_total"] as? Long ?: masukAwalGudang) - alokasiGudangLama)

                if (qtyBawa > sisaGudangLama) {
                    throw FirebaseFirestoreException("Stok tidak cukup! $namaProd sisa gudang pusat: $sisaGudangLama, jatah diminta: $qtyBawa", FirebaseFirestoreException.Code.ABORTED)
                }

                val alokasiGudangBaru = alokasiGudangLama + qtyBawa
                val sisaGudangBaru = sisaGudangLama - qtyBawa

                detailGudangTerbarui[idProd] = hashMapOf(
                    "id_produk" to idProd,
                    "nama_produk" to namaProd,
                    "harga_jual" to hargaJual,
                    "stok_masuk_awal" to masukAwalGudang,
                    "stok_tambahan" to (dataProdukGudangLama["stok_tambahan"] as? Long ?: 0L),
                    "stok_dialokasikan" to alokasiGudangBaru,
                    "sisa_gudang" to sisaGudangBaru,
                    "stok_total" to (dataProdukGudangLama["stok_total"] as? Long ?: masukAwalGudang)
                )

                val dataRiderLama = detailStokRiderTerbarui[idProd] as? Map<*, *>
                val awalRiderBaru: Long
                val tambahanRiderBaru: Long
                val totalRiderBaru: Long
                val terjualRiderLama = dataRiderLama?.get("terjual") as? Long ?: 0L
                val sisaRiderBaru: Long
                val diterimaStatusBaru: Boolean

                if (dataRiderLama == null) {
                    awalRiderBaru = qtyBawa
                    tambahanRiderBaru = 0L
                    totalRiderBaru = qtyBawa
                    sisaRiderBaru = qtyBawa - terjualRiderLama
                    diterimaStatusBaru = false
                } else {
                    val awalRiderLama = dataRiderLama["stok_awal"] as? Long ?: 0L
                    val tambahanRiderLama = dataRiderLama["stok_tambahan"] as? Long ?: 0L

                    awalRiderBaru = awalRiderLama
                    tambahanRiderBaru = tambahanRiderLama + qtyBawa
                    totalRiderBaru = awalRiderBaru + tambahanRiderBaru
                    sisaRiderBaru = totalRiderBaru - terjualRiderLama
                    diterimaStatusBaru = false
                }

                detailStokRiderTerbarui[idProd] = hashMapOf(
                    "id_produk" to idProd,
                    "nama_produk" to namaProd,
                    "stok_awal" to awalRiderBaru,
                    "stok_tambahan" to tambahanRiderBaru,
                    "total_stok" to totalRiderBaru,
                    "terjual" to terjualRiderLama,
                    "sisa_stok" to sisaRiderBaru,
                    "harga_jual" to hargaJual,
                    "diterima" to diterimaStatusBaru
                )
            }

            transaction.update(refGudangTunggal, "detail_gudang", detailGudangTerbarui)

            val dataStokHarianRider = hashMapOf(
                "id_stok" to "STK_$riderDocId",
                "tanggal" to targetTanggal,
                "id_rider" to riderUid,
                "nama_rider" to riderNama,
                "modal_kembalian" to nominalModalTerinput,
                "status_stok" to "CLOSED",
                "status_jualan" to "BELUM JUALAN",
                "detail_stok" to detailStokRiderTerbarui
            )
            transaction.set(refRider, dataStokHarianRider)

            null
        }.addOnSuccessListener {
            Toast.makeText(this, "Suplai distribusi untuk $riderNama berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Gagal Distribusi: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    inner class SuplaiAdapter(
        private val data: List<StokGudang>,
        val onQtyChanged: (String, Long) -> Unit
    ) : RecyclerView.Adapter<SuplaiAdapter.ViewHolder>() {

        inner class ViewHolder(val b: ItemPilihProdukGudangBinding) : RecyclerView.ViewHolder(b.root) {
            var textWatcher: TextWatcher? = null
        }

        override fun onCreateViewHolder(p: ViewGroup, vt: Int): ViewHolder =
            ViewHolder(ItemPilihProdukGudangBinding.inflate(LayoutInflater.from(p.context), p, false))

        override fun onBindViewHolder(vh: ViewHolder, pos: Int) {
            val item = data[pos]

            vh.b.etJumlahMasukLokal.removeTextChangedListener(vh.textWatcher)

            if (item.id_produk == "KEY_MODAL_CASH") {
                vh.b.tvNamaProdukBawaan.text = item.nama_produk
                vh.b.tvHargaProdukBawaan.text = "Isi nominal Rupiah cash awal"
            } else {
                vh.b.tvNamaProdukBawaan.text = "${item.nama_produk} (Sisa di rumah: ${item.sisa_gudang})"
                val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
                vh.b.tvHargaProdukBawaan.text = formatter.format(item.harga_jual).replace(",00", "")
            }

            val qtyLokal = kuotaAlokasiBaru[item.id_produk]
            vh.b.etJumlahMasukLokal.setText(qtyLokal?.toString() ?: "")

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