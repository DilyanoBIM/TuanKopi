package com.example.tuankopi

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.tuankopi.databinding.FragmentRiderDashboardBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RiderDashboardFragment : Fragment() {

    private var _binding: FragmentRiderDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mFirestore: FirebaseFirestore
    private val listeners = mutableListOf<ListenerRegistration>()

    private var uidRider = ""
    private var namaRider = ""
    private var tanggalHariIni = ""

    private var modalKasAwalLokal = 0L
    private var totalTunaiOmsetHariIni = 0f
    private var totalQrisOmsetHariIni = 0f

    // Map penampung jatah awal dan total distribusi harian murni dari stok_harian
    private val petaStokAwalKomoditas = HashMap<String, Long>()
    private val petaNamaProdukLokal = HashMap<String, String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRiderDashboardBinding.inflate(inflater, container, false)
        mAuth = FirebaseAuth.getInstance()
        mFirestore = FirebaseFirestore.getInstance()

        uidRider = mAuth.currentUser?.uid ?: ""
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        tanggalHariIni = sdf.format(Calendar.getInstance().time)

        muatProfilRiderDasar()
        jalankanLiveListenerDashboardRider()

        binding.btnShortcutKasir.setOnClickListener {
            (activity as? RiderDashboardActivity)?.gantiRiderFragment(RiderKasirFragment())
        }

        return binding.root
    }

    private fun muatProfilRiderDasar() {
        if (uidRider.isEmpty()) return
        mFirestore.collection("users").document(uidRider).get()
            .addOnSuccessListener { doc ->
                if (!isAdded || doc == null || !doc.exists()) return@addOnSuccessListener
                namaRider = doc.getString("nama") ?: "Rider"
                binding.tvRiderWelcome.text = "Halo, $namaRider"
                binding.tvRiderUid.text = "UID: $uidRider"
            }
    }

    private fun jalankanLiveListenerDashboardRider() {
        if (uidRider.isEmpty()) return
        val cleanTgl = tanggalHariIni.replace("-", "")
        val docIdStok = "${cleanTgl}_$uidRider"

        // 1. LISTENER KOLEKSI 1: stok_harian (Murni memantau status jualan, modal kembalian, dan jatah kuota awal)
        val lrStok = mFirestore.collection("stok_harian").document(docIdStok)
            .addSnapshotListener { snapshot, error ->
                if (error != null || !isAdded) return@addSnapshotListener

                if (snapshot == null || !snapshot.exists()) {
                    updateUIVisibilityJualan("BELUM JUALAN")
                    resetUIStokKopi()
                    return@addSnapshotListener
                }

                val statusJualanLapangan = snapshot.getString("status_jualan") ?: "BELUM JUALAN"
                updateUIVisibilityJualan(statusJualanLapangan)

                modalKasAwalLokal = snapshot.getLong("modal_kembalian") ?: 0L
                val formatterRupiah = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
                binding.tvModalKembalianDashboard.text = formatterRupiah.format(modalKasAwalLokal).replace(",00", "")

                petaStokAwalKomoditas.clear()
                petaNamaProdukLokal.clear()

                val detailStokMap = snapshot.get("detail_stok") as? Map<*, *>
                if (detailStokMap != null) {
                    for ((key, value) in detailStokMap) {
                        val idProd = key.toString()
                        val dataMap = value as? Map<*, *> ?: continue
                        val isDiterima = dataMap["diterima"] as? Boolean ?: false
                        if (!isDiterima) continue

                        val namaProd = dataMap["nama_produk"] as? String ?: "Menu"
                        val totalBawaanProd = dataMap["total_stok"] as? Long ?: (dataMap["stok_awal"] as? Long ?: 0L)

                        petaStokAwalKomoditas[idProd] = totalBawaanProd
                        petaNamaProdukLokal[idProd] = namaProd
                    }
                }

                // Memicu perhitungan ulang kuantitas produk menggunakan basis jatah terbaru yang telah dimuat
                pemicuKalkulasiUlangKuantitasStokLive()
            }
        listeners.add(lrStok)

        // 2. LISTENER KOLEKSI 2: transactions (Live agregasi finansial, chart metode pembayaran, dan real terjual murni SUCCESS)
        val lrTransactions = mFirestore.collection("transactions")
            .whereEqualTo("id_rider", uidRider)
            .whereEqualTo("status_pembayaran", "SUCCESS")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null || !isAdded) return@addSnapshotListener

                var totalTunai = 0f
                var totalQris = 0f

                // Peta penampung akumulasi produk terjual riil (id_produk -> total_qty_terjual)
                val petaKuantitasTerjualRealtime = HashMap<String, Long>()

                for (doc in snapshot.documents) {
                    val tglTrans = doc.getTimestamp("waktu_transaksi")?.toDate()
                    if (tglTrans != null) {
                        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(tglTrans)
                        if (fmt != tanggalHariIni) continue
                    }

                    // Ambil nominal keuangan transaksi
                    val metode = doc.getString("metode_pembayaran") ?: ""
                    val totalHarga = doc.getLong("total_harga")?.toFloat() ?: 0f

                    if (metode == "TUNAI") totalTunai += totalHarga
                    else if (metode == "QRIS") totalQris += totalHarga

                    // Ekstraksi data produk terlaris dari array "items" di dalam transaksi sukses
                    val arrayItems = doc.get("items") as? List<*>
                    if (arrayItems != null) {
                        for (itemObj in arrayItems) {
                            val dataItemMap = itemObj as? Map<*, *> ?: continue
                            val idProd = dataItemMap["id_produk"] as? String ?: ""
                            val qtyBeli = dataItemMap["qty"] as? Long ?: 0L

                            if (idProd.isNotEmpty()) {
                                petaKuantitasTerjualRealtime[idProd] = (petaKuantitasTerjualRealtime[idProd] ?: 0L) + qtyBeli
                            }
                        }
                    }
                }

                totalTunaiOmsetHariIni = totalTunai
                totalQrisOmsetHariIni = totalQris

                updateTampilanUangLaciFisikLive()
                tampilkanPieChartPendapatan(totalTunai, totalQris)

                // Kalkulasi sisa lapangan dan peringkat produk menggunakan data riil transaksi SUCCESS
                kalkulasiSaringanProdukDashboardLive(petaKuantitasTerjualRealtime)
            }
        listeners.add(lrTransactions)
    }

    private fun pemicuKalkulasiUlangKuantitasStokLive() {
        // Fungsi pembantu jika snapshot stok_harian berubah duluan sebelum ada transaksi baru
        kalkulasiSaringanProdukDashboardLive(HashMap())
    }

    private fun kalkulasiSaringanProdukDashboardLive(petaTerjualRealtime: HashMap<String, Long>) {
        if (!isAdded) return

        var totalStokKumulatif = 0L
        var totalTerjualKumulatif = 0L
        var totalSisaKumulatif = 0L

        val petaRankingUntukTabel = HashMap<String, Long>()
        val petaSisaUntukTabel = HashMap<String, Long>()

        // Iterasi seluruh jatah awal produk yang ada di motor untuk disinkronkan dengan total transaksi sukses
        for ((idProd, qtyAwalTotal) in petaStokAwalKomoditas) {
            val namaProduk = petaNamaProdukLokal[idProd] ?: "Menu"
            val qtyTerjualRiil = petaTerjualRealtime[idProd] ?: 0L
            val qtySisaRiil = qtyAwalTotal - qtyTerjualRiil

            totalStokKumulatif += qtyAwalTotal
            totalTerjualKumulatif += qtyTerjualRiil
            totalSisaKumulatif += qtySisaRiil

            petaRankingUntukTabel[namaProduk] = qtyTerjualRiil
            petaSisaUntukTabel[namaProduk] = qtySisaRiil
        }

        // Tampilkan agregasi kumulatif ke dalam widget panel ringkasan atas
        binding.tvTotalStokBawaan.text = totalStokKumulatif.toString()
        binding.tvTotalStokTerjual.text = totalTerjualKumulatif.toString()
        binding.tvTotalStokSisa.text = totalSisaKumulatif.toString()

        susunDaftarLiveRankingProduk(petaRankingUntukTabel, petaSisaUntukTabel)
    }

    private fun updateTampilanUangLaciFisikLive() {
        if (!isAdded) return
        val formatterRupiah = NumberFormat.getCurrencyInstance(Locale("in", "ID"))

        binding.tvPembayaranTunaiLive.text = formatterRupiah.format(totalTunaiOmsetHariIni.toLong()).replace(",00", "")
        binding.tvPembayaranQrisLive.text = formatterRupiah.format(totalQrisOmsetHariIni.toLong()).replace(",00", "")

        val totalPendapatanGross = totalTunaiOmsetHariIni.toLong() + totalQrisOmsetHariIni.toLong()
        binding.tvTotalPendapatanHariIni.text = formatterRupiah.format(totalPendapatanGross).replace(",00", "")

        val akumulasiLaciMotor = modalKasAwalLokal + totalTunaiOmsetHariIni.toLong()
        binding.tvTotalFisikLaciMotor.text = formatterRupiah.format(akumulasiLaciMotor).replace(",00", "")
    }

    private fun susunDaftarLiveRankingProduk(petaRanking: HashMap<String, Long>, petaSisa: HashMap<String, Long>) {
        val ctx = context ?: return
        binding.containerRankingProdukRider.removeAllViews()

        if (petaStokAwalKomoditas.isEmpty()) {
            val tvKosong = TextView(ctx).apply {
                text = "Belum ada item produk kopi yang diterima."
                setTextColor(Color.GRAY)
                textSize = 12f
                setPadding(0, 10, 0, 10)
            }
            binding.containerRankingProdukRider.addView(tvKosong)
            return
        }

        // Urutkan produk berdasarkan jumlah kuantitas yang paling banyak terjual hari ini
        val daftarUrut = petaRanking.toList().sortedByDescending { it.second }

        daftarUrut.forEachIndexed { index, pair ->
            val namaProductKey = pair.first
            val totalTerjualItem = pair.second
            val sisaStokItem = petaSisa[namaProductKey] ?: 0L

            val baris = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 12, 0, 12)

                val tvNama = TextView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f)
                    text = "${index + 1}. $namaProductKey"
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.parseColor("#191C1E"))
                    textSize = 13f
                }

                val tvQtyTerjual = TextView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9f)
                    text = "$totalTerjualItem Terjual"
                    setTextColor(Color.parseColor("#2E7D32"))
                    setTypeface(null, Typeface.BOLD)
                    textSize = 12f
                    gravity = Gravity.END
                }

                val tvQtySisa = TextView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9f)
                    text = "Sisa: $sisaStokItem Cup"
                    setTextColor(Color.parseColor("#C62828"))
                    setTypeface(null, Typeface.BOLD)
                    textSize = 12f
                    gravity = Gravity.END
                }

                addView(tvNama)
                addView(tvQtyTerjual)
                addView(tvQtySisa)
            }
            binding.containerRankingProdukRider.addView(baris)
        }
    }

    private fun updateUIVisibilityJualan(statusJualan: String) {
        val labelTeks = when (statusJualan) {
            "SEDANG JUALAN" -> "SEDANG JUALAN"
            "SELESAI JUALAN" -> "SELESAI JUALAN"
            else -> "BELUM MULAI JUALAN"
        }
        binding.tvStatusHariIni.text = labelTeks

        when (statusJualan) {
            "SEDANG JUALAN" -> {
                binding.tvStatusHariIni.setTextColor(Color.parseColor("#2E7D32"))
                binding.btnShortcutKasir.visibility = View.VISIBLE
                binding.btnMulaiJualan.isEnabled = false
                binding.btnSelesaiJualan.isEnabled = true
            }
            "SELESAI JUALAN" -> {
                binding.tvStatusHariIni.setTextColor(Color.parseColor("#C62828"))
                binding.btnShortcutKasir.visibility = View.GONE
                binding.btnMulaiJualan.isEnabled = false
                binding.btnSelesaiJualan.isEnabled = false
            }
            else -> {
                binding.tvStatusHariIni.setTextColor(Color.parseColor("#00236F"))
                binding.btnShortcutKasir.visibility = View.GONE
                binding.btnMulaiJualan.isEnabled = true
                binding.btnSelesaiJualan.isEnabled = false
            }
        }

        binding.btnMulaiJualan.setOnClickListener { merubahStatusJualanRider("SEDANG JUALAN") }
        binding.btnSelesaiJualan.setOnClickListener { merubahStatusJualanRider("SELESAI JUALAN") }
    }

    private fun merubahStatusJualanRider(statusBaruJualan: String) {
        val cleanTgl = tanggalHariIni.replace("-", "")
        val docId = "${cleanTgl}_$uidRider"

        mFirestore.collection("stok_harian").document(docId).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null && snapshot.exists()) {
                    val statusStokLogistik = snapshot.getString("status_stok") ?: "CLOSED"

                    if (statusBaruJualan == "SEDANG JUALAN" && statusStokLogistik != "AKTIF") {
                        Toast.makeText(context, "Gagal! Konfirmasi jatah cup di menu 'Konfirmasi Stok' dulu!", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }

                    mFirestore.collection("stok_harian").document(docId)
                        .update("status_jualan", statusBaruJualan)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Sukses merubah status jualan!", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    private fun resetUIStokKopi() {
        binding.tvTotalStokBawaan.text = "0"
        binding.tvTotalStokTerjual.text = "0"
        binding.tvTotalStokSisa.text = "0"
        binding.tvModalKembalianDashboard.text = "Rp 0"
        binding.tvPembayaranTunaiLive.text = "Rp 0"
        binding.tvPembayaranQrisLive.text = "Rp 0"
        binding.tvTotalPendapatanHariIni.text = "Rp 0"
        binding.tvTotalFisikLaciMotor.text = "Rp 0"
        binding.containerRankingProdukRider.removeAllViews()
    }

    private fun tampilkanPieChartPendapatan(tunai: Float, qris: Float) {
        val entriPie = ArrayList<PieEntry>()
        val total = tunai + qris

        val safeTunai = if (total == 0f) 50f else tunai
        val safeQris = if (total == 0f) 50f else qris

        val formatterRupiah = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        val labelTunaiFormat = formatterRupiah.format(safeTunai.toLong()).replace(",00", "")
        val labelQrisFormat = formatterRupiah.format(safeQris.toLong()).replace(",00", "")

        val pctTunai = if (total > 0) ((tunai / total) * 100).toInt() else 0
        val pctQris = if (total > 0) ((qris / total) * 100).toInt() else 0

        entriPie.add(PieEntry(safeTunai, "Tunai ($pctTunai%) $labelTunaiFormat"))
        entriPie.add(PieEntry(safeQris, "QRIS ($pctQris%) $labelQrisFormat"))

        val dataSet = PieDataSet(entriPie, "").apply {
            colors = arrayListOf(Color.parseColor("#4682B4"), Color.parseColor("#B22222"))
            sliceSpace = 2.5f
            xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            valueLinePart1OffsetPercentage = 75f
            valueLinePart1Length = 0.3f
            valueLinePart2Length = 0.4f
            valueLineColor = Color.parseColor("#191C1E")
        }

        val dataPieFinal = PieData(dataSet).apply {
            setValueTextColor(Color.parseColor("#191C1E"))
            setValueTextSize(12f)
            setValueTypeface(Typeface.DEFAULT_BOLD)
            setValueFormatter(object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getPieLabel(value: Float, pieEntry: PieEntry?): String {
                    return pieEntry?.label ?: ""
                }
            })
        }

        binding.pieChartRiderPendapatan.apply {
            data = dataPieFinal
            isDrawHoleEnabled = false
            description.isEnabled = false
            setTouchEnabled(true)
            setEntryLabelColor(Color.TRANSPARENT)
            setEntryLabelTextSize(0f)
            setUsePercentValues(false)
            setExtraOffsets(50f, 16f, 50f, 12f)

            legend.apply {
                isEnabled = true
                textColor = Color.parseColor("#191C1E")
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.VERTICAL
                verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
                setDrawInside(false)
                yEntrySpace = 6f
            }

            animateY(650, com.github.mikephil.charting.animation.Easing.EaseOutQuad)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listeners.forEach { it.remove() }
        listeners.clear()
        _binding = null
    }
}