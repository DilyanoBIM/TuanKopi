package com.example.tuankopi

import android.content.Context
import android.graphics.Color
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
import com.example.tuankopi.databinding.FragmentRiderKasirBinding
import com.example.tuankopi.databinding.ItemMenuKasirBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.NumberFormat
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Calendar

data class ProdukKasir(
    val idProduk: String,
    val namaProduk: String,
    val hargaJual: Long,
    val sisaStokMotor: Long,
    var qtyBeli: Long = 0L
)

class RiderKasirFragment : Fragment() {

    private var _binding: FragmentRiderKasirBinding? = null
    private val binding get() = _binding!!

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mFirestore: FirebaseFirestore
    private val httpClient = OkHttpClient()

    private var uidRider = ""
    private var namaRider = ""
    private var tanggalHariIni = ""
    private var docIdStokTarget = ""

    private var listProdukKasir = ArrayList<ProdukKasir>()
    private lateinit var mAdapter: KasirAdapter
    private var totalBelanjaGross = 0L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRiderKasirBinding.inflate(inflater, container, false)
        mAuth = FirebaseAuth.getInstance()
        mFirestore = FirebaseFirestore.getInstance()

        uidRider = mAuth.currentUser?.uid ?: ""
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        tanggalHariIni = sdf.format(Calendar.getInstance().time)
        docIdStokTarget = "${tanggalHariIni.replace("-", "")}_$uidRider"

        setupRecyclerView()
        validasiKeaktifanKasirDanMuatMenu()

        binding.rgMetodePembayaran.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbBayarQris) {
                binding.layoutInputCashTunai.visibility = View.GONE
                binding.etUangBayarTunai.setText("")
                binding.tvUangKembalianTunai.text = "Kembalian: Rp 0"
            } else {
                binding.layoutInputCashTunai.visibility = View.VISIBLE
            }
        }

        binding.etUangBayarTunai.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                hitungUangKembalianSecaraLive()
            }
        })

        binding.btnSimpanTransaksiFinal.setOnClickListener {
            eksekusiSimpanInvoiceTransaction()
        }

        return binding.root
    }

    private fun setupRecyclerView() {
        mAdapter = KasirAdapter(listProdukKasir) {
            kalkulasiUlangTotalKeranjang()
        }
        binding.rvMenuKasirRider.layoutManager = LinearLayoutManager(context)
        binding.rvMenuKasirRider.adapter = mAdapter
    }

    private fun validasiKeaktifanKasirDanMuatMenu() {
        mFirestore.collection("stok_harian").document(docIdStokTarget).get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                if (snapshot == null || !snapshot.exists()) {
                    kunciAksesKasirLayar("Belum ada alokasi jatah logistik untuk hari ini.")
                    return@addOnSuccessListener
                }

                namaRider = snapshot.getString("nama_rider") ?: "Rider"
                val statusStokLogistik = snapshot.getString("status_stok") ?: "CLOSED"
                val statusJualanLapangan = snapshot.getString("status_jualan") ?: "BELUM JUALAN"

                if (statusStokLogistik != "AKTIF" || statusJualanLapangan != "SEDANG JUALAN") {
                    kunciAksesKasirLayar("Kasir Terkunci! Pastikan sudah Terima Jatah dan klik 'Mulai Jualan'.")
                    return@addOnSuccessListener
                }

                listProdukKasir.clear()
                val rawMap = snapshot.get("detail_stok") as? Map<*, *>
                if (rawMap != null) {
                    for ((key, value) in rawMap) {
                        val idProd = key.toString()
                        val dataMap = value as? Map<*, *> ?: continue
                        val isDiterima = dataMap["diterima"] as? Boolean ?: false

                        if (isDiterima) {
                            val nama = dataMap["nama_produk"] as? String ?: "Menu Kopi"
                            val harga = dataMap["harga_jual"] as? Long ?: 0L
                            val sisa = dataMap["sisa_stok"] as? Long ?: 0L

                            if (sisa > 0L) {
                                listProdukKasir.add(ProdukKasir(idProd, nama, harga, sisa))
                            }
                        }
                    }
                }

                listProdukKasir.sortBy { it.namaProduk }
                mAdapter.notifyDataSetChanged()
            }
    }

    private fun kunciAksesKasirLayar(pesanWarning: String) {
        binding.tvKasirStatusWarning.text = pesanWarning
        binding.tvKasirStatusWarning.visibility = View.VISIBLE
        binding.btnSimpanTransaksiFinal.isEnabled = false
        binding.etUangBayarTunai.isEnabled = false
    }

    private fun kalkulasiUlangTotalKeranjang() {
        totalBelanjaGross = 0L
        for (item in listProdukKasir) {
            totalBelanjaGross += (item.hargaJual * item.qtyBeli)
        }
        val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        binding.tvTotalHargaKeranjang.text = formatter.format(totalBelanjaGross).replace(",00", "")
        hitungUangKembalianSecaraLive()
    }

    private fun hitungUangKembalianSecaraLive() {
        if (binding.rgMetodePembayaran.checkedRadioButtonId == R.id.rbBayarQris) return
        val txt = binding.etUangBayarTunai.text.toString().trim()
        val cashDiterima = if (txt.isEmpty()) 0L else txt.toLong()
        val kembalian = cashDiterima - totalBelanjaGross

        val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        if (kembalian < 0) {
            binding.tvUangKembalianTunai.text = "Uang Pembayaran Kurang!"
            binding.tvUangKembalianTunai.setTextColor(Color.RED)
        } else {
            binding.tvUangKembalianTunai.text = "Kembalian: ${formatter.format(kembalian).replace(",00", "")}"
            binding.tvUangKembalianTunai.setTextColor(Color.parseColor("#2E7D32"))
        }
    }

    private fun eksekusiSimpanInvoiceTransaction() {
        val listItemDibeli = listProdukKasir.filter { it.qtyBeli > 0 }
        if (listItemDibeli.isEmpty()) {
            Toast.makeText(context, "Keranjang belanja kosong!", Toast.LENGTH_SHORT).show()
            return
        }

        val metodePembayaran = if (binding.rgMetodePembayaran.checkedRadioButtonId == R.id.rbBayarTunai) "TUNAI" else "QRIS"
        val timeStampInvoice = System.currentTimeMillis()
        val orderIdRandom = "TK-${tanggalHariIni.replace("-", "")}-$timeStampInvoice"

        if (metodePembayaran == "TUNAI") {
            // Logika TUNAI: Tetap menggunakan transaksi lokal langsung sukses
            prosesTransaksiTunaiLokal(orderIdRandom, listItemDibeli)
        } else {
            // Logika QRIS: Tembak ke API PHP Backend (Status awal: PENDING)
            prosesTransaksiQrisViaBackend(orderIdRandom, listItemDibeli)
        }
    }

    private fun prosesTransaksiTunaiLokal(orderId: String, listItemDibeli: List<ProdukKasir>) {
        val txtTunai = binding.etUangBayarTunai.text.toString().trim()
        val nominalDiterima = if (txtTunai.isEmpty()) 0L else txtTunai.toLong()
        val nominalKembalian = nominalDiterima - totalBelanjaGross

        if (nominalKembalian < 0) {
            Toast.makeText(context, "Nominal pembayaran tunai kurang!", Toast.LENGTH_SHORT).show()
            return
        }

        val refStokHarian = mFirestore.collection("stok_harian").document(docIdStokTarget)
        val refNewTransaction = mFirestore.collection("transactions").document(orderId)

        binding.btnSimpanTransaksiFinal.isEnabled = false

        mFirestore.runTransaction { transaction ->
            val snapshotStok = transaction.get(refStokHarian)
            if (!snapshotStok.exists()) throw FirebaseFirestoreException("Data Logistik Harian Hilang!", FirebaseFirestoreException.Code.NOT_FOUND)

            val rawDetailStok = snapshotStok.get("detail_stok") as? Map<*, *> ?: HashMap<String, Any>()
            val detailStokTerbarui = HashMap<String, Any>()
            for ((k, v) in rawDetailStok) detailStokTerbarui[k.toString()] = v!!

            val arrayItemsInvoice = ArrayList<HashMap<String, Any>>()
            for (produk in listItemDibeli) {
                val subMapDataKopiLama = detailStokTerbarui[produk.idProduk] as? Map<*, *> ?: throw FirebaseFirestoreException("Produk tidak terdaftar!", FirebaseFirestoreException.Code.NOT_FOUND)

                val terjualDbLama = subMapDataKopiLama["terjual"] as? Long ?: 0L
                val sisaDbLama = subMapDataKopiLama["sisa_stok"] as? Long ?: 0L

                if (produk.qtyBeli > sisaDbLama) throw FirebaseFirestoreException("Stok ${produk.namaProduk} tidak cukup!", FirebaseFirestoreException.Code.ABORTED)

                val subMapTerbarui = HashMap<String, Any>()
                for ((subKey, subVal) in subMapDataKopiLama) { if (subVal != null) subMapTerbarui[subKey.toString()] = subVal }
                subMapTerbarui["terjual"] = terjualDbLama + java.lang.Long.valueOf(produk.qtyBeli)
                subMapTerbarui["sisa_stok"] = sisaDbLama - java.lang.Long.valueOf(produk.qtyBeli)

                detailStokTerbarui[produk.idProduk] = subMapTerbarui

                arrayItemsInvoice.add(hashMapOf(
                    "id_produk" to produk.idProduk,
                    "nama_produk" to produk.namaProduk,
                    "qty" to produk.qtyBeli,
                    "harga_satuan" to produk.hargaJual
                ))
            }

            val dataInvoice = hashMapOf(
                "order_id" to orderId,
                "id_rider" to uidRider,
                "nama_rider" to namaRider,
                "waktu_transaksi" to com.google.firebase.Timestamp.now(),
                "metode_pembayaran" to "TUNAI",
                "total_harga" to totalBelanjaGross,
                "nominal_diterima" to nominalDiterima,
                "nominal_kembalian" to nominalKembalian,
                "status_pembayaran" to "SUCCESS",
                "items" to arrayItemsInvoice
            )

            transaction.update(refStokHarian, "detail_stok", detailStokTerbarui)
            transaction.set(refNewTransaction, dataInvoice)
            null
        }.addOnSuccessListener {
            Toast.makeText(context, "Transaksi Tunai Sukses!", Toast.LENGTH_SHORT).show()
            kembaliKeDashboard()
        }.addOnFailureListener { e ->
            binding.btnSimpanTransaksiFinal.isEnabled = true
            Toast.makeText(context, "Gagal: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun prosesTransaksiQrisViaBackend(orderId: String, listItemDibeli: List<ProdukKasir>) {
        binding.btnSimpanTransaksiFinal.isEnabled = false

        val jsonArrayItems = JSONArray()
        for (item in listItemDibeli) {
            val obj = JSONObject()
            obj.put("id_produk", item.idProduk)
            obj.put("nama_produk", item.namaProduk)
            obj.put("qty", item.qtyBeli)
            obj.put("harga_satuan", item.hargaJual)
            jsonArrayItems.put(obj)
        }

        val formBody = FormBody.Builder()
            .add("order_id", orderId)
            .add("amount", totalBelanjaGross.toString())
            .add("id_rider", uidRider)
            .add("nama_rider", namaRider)
            .add("items", jsonArrayItems.toString())
            .build()

        val request = Request.Builder()
            .url("https://jovani-galvanic-laura.ngrok-free.dev/midtrans/charge_qris.php")
            .post(formBody)
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    binding.btnSimpanTransaksiFinal.isEnabled = true
                    Toast.makeText(context, "Koneksi API Gagal: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val resBody = response.body?.string() ?: ""
                android.util.Log.d("MIDTRANS_DEBUG", resBody)
                activity?.runOnUiThread {
                    try {
                        val jsonRes = JSONObject(resBody)
                        if (jsonRes.getString("status") == "success") {

                            val actionsArray = jsonRes.getJSONArray("actions")
                            var qrisUrlRaw = ""

                            for (i in 0 until actionsArray.length()) {
                                val actionObj = actionsArray.getJSONObject(i)
                                if (actionObj.getString("name") == "generate-qr-code-v2") {
                                    qrisUrlRaw = actionObj.getString("url")
                                    break
                                }
                            }

                            if (qrisUrlRaw.isEmpty()) {
                                for (i in 0 until actionsArray.length()) {
                                    val actionObj = actionsArray.getJSONObject(i)
                                    if (actionObj.getString("name") == "generate-qr-code") {
                                        qrisUrlRaw = actionObj.getString("url")
                                        break
                                    }
                                }
                            }

                            if (qrisUrlRaw.isNotEmpty()) {
                                binding.root.postDelayed({
                                    // Memanggil fungsi tanpa parameter yang tidak terpakai
                                    val dialog = tampilkanDialogQRISTester(qrisUrlRaw)

                                    if (dialog != null) {
                                        pasangRealtimeListenerTransaksi(orderId, dialog)
                                    }
                                }, 100)
                            } else {
                                Toast.makeText(context, "Gagal mendapatkan URL QR Code.", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            binding.btnSimpanTransaksiFinal.isEnabled = true
                            Toast.makeText(context, "Gagal: ${jsonRes.getString("message")}", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        binding.btnSimpanTransaksiFinal.isEnabled = true
                        Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    // Menghapus parameter urlSimulatorLengkap yang tidak digunakan
    // Ganti fungsi tampilkanDialogQRISTester di dalam RiderKasirFragment.kt dengan versi ringkas ini:
    private fun tampilkanDialogQRISTester(urlGambarQris: String): androidx.appcompat.app.AlertDialog? {
        val contextLayout = context ?: return null

        val rootContainer = android.widget.LinearLayout(contextLayout).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            gravity = android.view.Gravity.CENTER_HORIZONTAL
        }

        // Menggunakan WebView untuk merender URL image QRIS langsung dari server Midtrans
        val webViewQris = android.webkit.WebView(contextLayout).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                600
            )
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.javaScriptEnabled = true // Tambahan opsional untuk stabilitas render WebView
            loadUrl(urlGambarQris)
        }

        rootContainer.addView(webViewQris)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(contextLayout)
            .setTitle("Scan Pembayaran QRIS")
            .setMessage("Tunjukkan QR Code ini ke pelanggan untuk memproses pembayaran.")
            .setView(rootContainer)
            .setCancelable(false)
            .setPositiveButton("Batal Transaksi") { dialogInterface, _ ->
                dialogInterface.dismiss()
                binding.btnSimpanTransaksiFinal.isEnabled = true
            }
            .create()

        dialog.show()
        return dialog
    }

    private fun kembaliKeDashboard() {
        (activity as? RiderDashboardActivity)?.gantiRiderFragment(RiderDashboardFragment())
    }

    private fun tampilkanDialogQRIS(urlGambarQris: String): androidx.appcompat.app.AlertDialog? {
        val contextLayout = context ?: return null

        // Buat WebView secara dinamis
        val webView = android.webkit.WebView(contextLayout).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            loadUrl(urlGambarQris)
        }

        // Bangun dialog dan simpan ke dalam variabel
        val dialog = androidx.appcompat.app.AlertDialog.Builder(contextLayout)
            .setTitle("Scan Pembayaran QRIS")
            .setMessage("Silakan tunjukkan QR Code ini kepada pelanggan.")
            .setView(webView)
            .setCancelable(false)
            .setPositiveButton("Batal Transaksi") { dialogInterface, _ ->
                dialogInterface.dismiss()
                binding.btnSimpanTransaksiFinal.isEnabled = true
            }
            .create()

        dialog.show()
        return dialog
    }

    private fun pasangRealtimeListenerTransaksi(orderId: String, dialogPembayaran: androidx.appcompat.app.AlertDialog) {
        mFirestore.collection("transactions").document(orderId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val status = snapshot.getString("status_pembayaran") ?: "PENDING"
                if (status == "SUCCESS") {
                    Toast.makeText(context, "Pembayaran QRIS Sukses Diterima!", Toast.LENGTH_SHORT).show()
                    dialogPembayaran.dismiss()
                    kembaliKeDashboard()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class KasirAdapter(
        private val data: List<ProdukKasir>,
        val onQtyChanged: () -> Unit
    ) : RecyclerView.Adapter<KasirAdapter.ViewHolder>() {

        inner class ViewHolder(val b: ItemMenuKasirBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(ItemMenuKasirBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val prod = data[position]
            holder.b.tvNamaKopiKasir.text = prod.namaProduk
            holder.b.tvSisaStokKasir.text = "Ready Motor: ${prod.sisaStokMotor} Cup"

            val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            holder.b.tvHargaKopiKasir.text = formatter.format(prod.hargaJual).replace(",00", "")
            holder.b.tvQtyItemBelanja.text = prod.qtyBeli.toString()

            holder.b.btnTambahQty.setOnClickListener {
                if (prod.qtyBeli < prod.sisaStokMotor) {
                    prod.qtyBeli++
                    holder.b.tvQtyItemBelanja.text = prod.qtyBeli.toString()
                    onQtyChanged()
                } else {
                    Toast.makeText(context, "Batas maksimal muatan!", Toast.LENGTH_SHORT).show()
                }
            }

            holder.b.btnKurangiQty.setOnClickListener {
                if (prod.qtyBeli > 0) {
                    prod.qtyBeli--
                    holder.b.tvQtyItemBelanja.text = prod.qtyBeli.toString()
                    onQtyChanged()
                }
            }
        }

        override fun getItemCount(): Int = data.size
    }
}