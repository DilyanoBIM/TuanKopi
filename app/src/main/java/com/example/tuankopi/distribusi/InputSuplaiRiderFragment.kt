package com.example.tuankopi.distribusi

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tuankopi.StokGudang
import com.example.tuankopi.databinding.FragmentInputSuplaiRiderBinding
import com.example.tuankopi.databinding.ItemPilihProdukGudangBinding
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import java.text.NumberFormat
import java.util.Locale

class InputSuplaiRiderFragment : Fragment() {

    private var _binding: FragmentInputSuplaiRiderBinding? = null
    private val binding get() = _binding!!
    private lateinit var mFirestore: FirebaseFirestore
    private var listStokGudangPusat = ArrayList<StokGudang>()
    private lateinit var mAdapter: SuplaiAdapter

    private var targetTanggal = ""
    private var riderUid = ""
    private var riderNama = ""
    private var cleanedTgl = ""

    private val kuotaAlokasiBaru = HashMap<String, Long>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInputSuplaiRiderBinding.inflate(inflater, container, false)

        targetTanggal = arguments?.getString("TARGET_TANGGAL") ?: ""
        riderUid = arguments?.getString("RIDER_UID") ?: ""
        riderNama = arguments?.getString("RIDER_NAMA") ?: ""
        cleanedTgl = targetTanggal.replace("-", "")

        binding.tvHeaderPilih.text = "Ambil Produk & Atur Modal Awal"
        binding.btnSimpanBatchGudang.text = "KONFIRMASI DISTRIBUSI MOTOR"

        mFirestore = FirebaseFirestore.getInstance()

        setupRecyclerView()
        muatStokTersediaGudangPusatMap()

        binding.btnSimpanBatchGudang.setOnClickListener {
            eksekusiTransaksiSuplaiHilir()
        }

        return binding.root
    }

    private fun setupRecyclerView() {
        mAdapter = SuplaiAdapter(listStokGudangPusat) { idProd, qty ->
            if (qty > 0) kuotaAlokasiBaru[idProd] = qty else kuotaAlokasiBaru.remove(idProd)
        }
        binding.rvPilihProduk.layoutManager = LinearLayoutManager(requireContext())
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
                            harga_jual = 0L, sisa_gudang = 999999L, stok_masuk_awal = 1L
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
                                        id_gudang = "GUD_$cleanedTgl", tanggal = targetTanggal,
                                        id_produk = idProd, nama_produk = namaProd,
                                        stok_masuk_awal = awalGudang, stok_dialokasikan = alokasiGudang,
                                        sisa_gudang = sisaGudang, harga_jual = harga
                                    )
                                )
                            }
                        }
                    }
                    listProdukLokal.sortBy { it.nama_produk }
                    listStokGudangPusat.addAll(listProdukLokal)
                    mAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(requireContext(), "Owner belum mengisi slot produksi awal di menu Stok Gudang!", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun eksekusiTransaksiSuplaiHilir() {
        val nominalModalTerinput = kuotaAlokasiBaru["KEY_MODAL_CASH"] ?: 0L
        val kuotaKopiMurni = HashMap(kuotaAlokasiBaru)
        kuotaKopiMurni.remove("KEY_MODAL_CASH")

        if (kuotaKopiMurni.isEmpty()) {
            Toast.makeText(requireContext(), "Mohon isi kuota cup kopi yang akan dibawa keliling!", Toast.LENGTH_SHORT).show()
            return
        }

        val sbRingkasan = StringBuilder()
        val formatterRupiah = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        val textModalFormat = formatterRupiah.format(nominalModalTerinput).replace(",00", "")

        sbRingkasan.append("📋 RINCIAN DISTRIBUSI RIDER:\nNama Rider: $riderNama\nModal Cash: $textModalFormat\n\n☕ MUATAN:\n")
        var nomorUrut = 1
        for ((idProd, qtyBawa) in kuotaKopiMurni) {
            val namaProdukAsli = listStokGudangPusat.find { it.id_produk == idProd }?.nama_produk ?: "Menu Kopi"
            sbRingkasan.append("${nomorUrut++}. $namaProdukAsli -> $qtyBawa Cup\n")
        }

        AlertDialog.Builder(requireContext()).apply {
            setTitle("Konfirmasi Distribusi")
            setMessage(sbRingkasan.toString())
            setCancelable(false)
            setPositiveButton("Kirim Jatah") { dialog, _ ->
                dialog.dismiss()
                eksekusiSimpanKeFirestoreTransaksional(nominalModalTerinput, kuotaKopiMurni)
            }
            setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            create().show()
        }
    }

    private fun eksekusiSimpanKeFirestoreTransaksional(nominalModalTerinput: Long, kuotaKopiMurni: HashMap<String, Long>) {
        val riderDocId = "${cleanedTgl}_$riderUid"
        val refGudangTunggal = mFirestore.collection("stok_gudang").document(cleanedTgl)
        val refRider = mFirestore.collection("stok_harian").document(riderDocId)

        mFirestore.runTransaction { transaction ->
            val snapshotGudang = transaction.get(refGudangTunggal)
            val snapshotRider = transaction.get(refRider)

            if (!snapshotGudang.exists()) throw FirebaseFirestoreException("Dokumen stok gudang pusat tidak ditemukan!", FirebaseFirestoreException.Code.NOT_FOUND)

            // TAMBAHAN: Keamanan Backend - Tolak jika Rider sudah selesai jualan
            if (snapshotRider.exists() && snapshotRider.getString("status_jualan") == "SELESAI JUALAN") {
                throw FirebaseFirestoreException("TIDAK DAPAT DISTRIBUSI KE RIDER ${riderNama.uppercase()} KARENA RIDER TELAH SELESAI JUALAN", FirebaseFirestoreException.Code.ABORTED)
            }

            // CEK APAKAH RIDER MASIH PUNYA PENDINGAN
            if (snapshotRider.exists() && snapshotRider.contains("pending_suplai")) {
                throw FirebaseFirestoreException("Distribusi ditolak! Rider belum mengonfirmasi/menerima distribusi Anda yang sebelumnya.", FirebaseFirestoreException.Code.ABORTED)
            }

            val rawDetailGudang = snapshotGudang.get("detail_gudang") as? Map<*, *> ?: HashMap<String, Any>()
            val detailGudangTerbarui = HashMap<String, Any>()
            for ((k, v) in rawDetailGudang) detailGudangTerbarui[k.toString()] = v!!

            val sesiTerakhir = if (snapshotRider.exists()) snapshotRider.getLong("sesi_terakhir") ?: 0L else 0L
            val sesiBaru = sesiTerakhir + 1

            val itemsPending = HashMap<String, Any>()

            for ((idProd, qtyBawa) in kuotaKopiMurni) {
                val dataProdukGudangLama = rawDetailGudang[idProd] as? Map<*, *>
                    ?: throw FirebaseFirestoreException("Produk $idProd tidak ditemukan di gudang!", FirebaseFirestoreException.Code.NOT_FOUND)

                val namaProd = dataProdukGudangLama["nama_produk"] as? String ?: "Menu Kopi"
                val hargaJual = dataProdukGudangLama["harga_jual"] as? Long ?: 0L
                val masukAwalGudang = dataProdukGudangLama["stok_masuk_awal"] as? Long ?: 0L
                val alokasiGudangLama = dataProdukGudangLama["stok_dialokasikan"] as? Long ?: 0L
                val sisaGudangLama = dataProdukGudangLama["sisa_gudang"] as? Long ?: ((dataProdukGudangLama["stok_total"] as? Long ?: masukAwalGudang) - alokasiGudangLama)

                if (qtyBawa > sisaGudangLama) throw FirebaseFirestoreException("Stok $namaProd di gudang pusat tidak cukup!", FirebaseFirestoreException.Code.ABORTED)

                // POTONG STOK DARI GUDANG UTAMA
                detailGudangTerbarui[idProd] = hashMapOf(
                    "id_produk" to idProd, "nama_produk" to namaProd, "harga_jual" to hargaJual,
                    "stok_masuk_awal" to masukAwalGudang,
                    "stok_tambahan" to (dataProdukGudangLama["stok_tambahan"] as? Long ?: 0L),
                    "stok_dialokasikan" to alokasiGudangLama + qtyBawa,
                    "sisa_gudang" to sisaGudangLama - qtyBawa,
                    "stok_total" to (dataProdukGudangLama["stok_total"] as? Long ?: masukAwalGudang)
                )

                // MASUKKAN KE PENDING RIDER
                itemsPending[idProd] = hashMapOf(
                    "nama_produk" to namaProd,
                    "qty" to qtyBawa,
                    "harga_jual" to hargaJual
                )
            }

            transaction.update(refGudangTunggal, "detail_gudang", detailGudangTerbarui)

            val pendingSuplaiObj = hashMapOf(
                "sesi_ke" to sesiBaru,
                "modal_baru" to nominalModalTerinput,
                "items" to itemsPending
            )

            if (snapshotRider.exists()) {
                transaction.update(refRider, "pending_suplai", pendingSuplaiObj)
            } else {
                val dataStokHarianRider = hashMapOf(
                    "id_stok" to "STK_$riderDocId", "tanggal" to targetTanggal,
                    "id_rider" to riderUid, "nama_rider" to riderNama,
                    "modal_kembalian" to 0L, "status_stok" to "CLOSED",
                    "status_jualan" to "BELUM JUALAN", "sesi_terakhir" to 0L,
                    "detail_stok" to HashMap<String, Any>(),
                    "pending_suplai" to pendingSuplaiObj
                )
                transaction.set(refRider, dataStokHarianRider)
            }
            null
        }.addOnSuccessListener {
            Toast.makeText(requireContext(), "Suplai (Sesi Terbaru) telah dikirim ke Rider!", Toast.LENGTH_SHORT).show()
            requireActivity().supportFragmentManager.popBackStack()
        }.addOnFailureListener { e ->
            // PERUBAHAN: Menampilkan format toast murni khusus untuk validasi SELESAI JUALAN
            val errorMsg = e.message ?: "Terjadi kesalahan"
            if (errorMsg.contains("TIDAK DAPAT DISTRIBUSI")) {
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), "Gagal: $errorMsg", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    inner class SuplaiAdapter(
        private val data: List<StokGudang>, val onQtyChanged: (String, Long) -> Unit
    ) : RecyclerView.Adapter<SuplaiAdapter.ViewHolder>() {
        inner class ViewHolder(val b: ItemPilihProdukGudangBinding) : RecyclerView.ViewHolder(b.root) {
            var textWatcher: TextWatcher? = null
        }
        override fun onCreateViewHolder(p: ViewGroup, vt: Int): ViewHolder = ViewHolder(ItemPilihProdukGudangBinding.inflate(LayoutInflater.from(p.context), p, false))
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