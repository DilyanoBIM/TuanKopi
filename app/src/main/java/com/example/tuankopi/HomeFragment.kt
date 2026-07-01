package com.example.tuankopi

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.tuankopi.databinding.FragmentHomeBinding
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
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

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mFirestore: FirebaseFirestore

    // Simpan referensi listener agar bisa di-detach saat fragment destroy
    private val listeners = mutableListOf<ListenerRegistration>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding   = FragmentHomeBinding.inflate(inflater, container, false)
        mAuth      = FirebaseAuth.getInstance()
        mFirestore = FirebaseFirestore.getInstance()

        muatDataProfilOwner()
        muatAnalitikBisnisUtama()

        return binding.root
    }

    // ─────────────────────────────────────────────
    // Profil Owner – satu kali baca, tidak perlu realtime
    // ─────────────────────────────────────────────
    // ─────────────────────────────────────────────
    // Profil Owner – satu kali baca, tidak perlu realtime
    // ─────────────────────────────────────────────
    private fun muatDataProfilOwner() {
        val uid = mAuth.currentUser?.uid ?: return
        mFirestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (!isAdded || doc == null || !doc.exists()) return@addOnSuccessListener

                // Mengambil field nama dan role dari dokumen Firestore
                val namaUser = doc.getString("nama") ?: "User"
                val roleUser = doc.getString("role") ?: "owner"

                // Set text sapaan: "Selamat datang, Bimo Dilyano (owner)"
                binding.tvNamaDashboard.text = "$namaUser ($roleUser)"
            }
            .addOnFailureListener {
                if (isAdded) {
                    binding.tvNamaDashboard.text = "Owner Tuan Kopi"
                }
            }
    }

    // ─────────────────────────────────────────────
    // Analitik utama
    // closing_laporan  → realtime (data utama dashboard)
    // transactions     → get() satu kali (tren chart)
    // ─────────────────────────────────────────────
    private fun muatAnalitikBisnisUtama() {
        val sdf            = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val tanggalHariIni = sdf.format(Calendar.getInstance().time)

        val reg = mFirestore.collection("closing_laporan")
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null || !isAdded) return@addSnapshotListener

                var omsetHariIni      = 0.0
                var omsetMingguIni    = 0.0
                var omsetBulanIni     = 0.0
                var totalTunaiHariIni = 0f
                var totalQrisHariIni  = 0f
                val petaRankingRider  = HashMap<String, Double>()

                for (doc in snapshots.documents) {
                    val tanggalDoc  = doc.getString("tanggal")           ?: ""
                    val omsetSistem = doc.getDouble("total_omset_sistem") ?: 0.0
                    val tunaiSistem = doc.getDouble("total_tunai_sistem") ?: 0.0
                    val qrisSistem  = doc.getDouble("total_qris_sistem")  ?: 0.0
                    val namaRider   = doc.getString("nama_rider")         ?: "Rider"

                    if (tanggalDoc == tanggalHariIni) {
                        omsetHariIni      += omsetSistem
                        totalTunaiHariIni += tunaiSistem.toFloat()
                        totalQrisHariIni  += qrisSistem.toFloat()
                        petaRankingRider[namaRider] =
                            (petaRankingRider[namaRider] ?: 0.0) + omsetSistem
                    }
                    omsetMingguIni += omsetSistem
                    omsetBulanIni  += omsetSistem
                }

                if (!isAdded) return@addSnapshotListener

                binding.tvOmsetHarian.text   = formatRupiah(omsetHariIni)
                binding.tvOmsetMingguan.text = formatRupiah(omsetMingguIni)
                binding.tvOmsetBulanan.text  = formatRupiah(omsetBulanIni)

                tampilkanPieChartMetodePembayaran(totalTunaiHariIni, totalQrisHariIni)
                susunTabelLiveRankingRider(petaRankingRider)
                hitungLabaBersih(omsetHariIni, tanggalHariIni)
            }
        listeners.add(reg)

        // Bar chart tren – cukup get() satu kali, tidak perlu realtime
        mFirestore.collection("transactions")
            .whereEqualTo("status_pembayaran", "SUCCESS")
            .get()
            .addOnSuccessListener { snapshots ->
                if (snapshots == null || !isAdded) return@addOnSuccessListener
                val entriBar = ArrayList<BarEntry>()
                snapshots.documents.forEachIndexed { idx, doc ->
                    val harga = doc.getDouble("total_harga") ?: 0.0
                    entriBar.add(BarEntry(idx.toFloat(), harga.toFloat()))
                }
                if (entriBar.isNotEmpty()) tampilkanBarChartTrenPenjualan(entriBar)
            }
    }

    // ─────────────────────────────────────────────
    // Laba bersih – get() satu kali
    // ─────────────────────────────────────────────
    private fun hitungLabaBersih(omsetHariIni: Double, tanggalHariIni: String) {
        mFirestore.collection("operasional_expenses")
            .whereEqualTo("tanggal", tanggalHariIni)
            .get()
            .addOnSuccessListener { snapshots ->
                if (snapshots == null || !isAdded) return@addOnSuccessListener
                var totalPengeluaran = 0.0
                for (doc in snapshots.documents) {
                    totalPengeluaran += doc.getDouble("nominal") ?: 0.0
                }
                val labaBersih = omsetHariIni - (omsetHariIni * 0.4) - totalPengeluaran
                if (isAdded) binding.tvLabaBersih.text = formatRupiah(labaBersih)
            }
    }

    // ─────────────────────────────────────────────
    // Bar Chart Tren Penjualan
    // ─────────────────────────────────────────────
    private fun tampilkanBarChartTrenPenjualan(listEntri: ArrayList<BarEntry>) {
        val dataSet = BarDataSet(listEntri, "").apply {
            color          = Color.parseColor("#00236F")
            highLightColor = Color.parseColor("#4B6BCC")
            valueTextColor = Color.TRANSPARENT
            valueTextSize  = 0f
        }

        binding.barChartTren.apply {
            data = BarData(dataSet).also { it.barWidth = 0.6f }
            description.isEnabled = false
            legend.isEnabled      = false
            setTouchEnabled(false)
            axisRight.isEnabled   = false
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor      = Color.parseColor("#F3F4F6")
                setDrawAxisLine(false)
                textColor      = Color.parseColor("#9CA3AF")
                textSize       = 10f
            }
            xAxis.isEnabled = false
            setFitBars(true)
            animateY(500)
            invalidate()
        }
    }

    // ─────────────────────────────────────────────
    // Pie Chart Metode Pembayaran
    // ─────────────────────────────────────────────
    private fun tampilkanPieChartMetodePembayaran(tunai: Float, qris: Float) {
        val safeQris  = if (tunai == 0f && qris == 0f) 65f else qris
        val safeTunai = if (tunai == 0f && qris == 0f) 35f else tunai
        val total     = safeQris + safeTunai
        val pctQris   = if (total > 0) (safeQris / total * 100).toInt() else 65

        val entriPie = arrayListOf(
            PieEntry(safeQris,  "QRIS"),
            PieEntry(safeTunai, "Tunai")
        )

        val dataSet = PieDataSet(entriPie, "").apply {
            colors = arrayListOf(
                Color.parseColor("#00236F"),
                Color.parseColor("#DCE1FF")
            )
            valueTextColor = Color.TRANSPARENT
            valueTextSize  = 0f
            sliceSpace     = 3f
        }

        binding.pieChartMetode.apply {
            data                    = PieData(dataSet)
            description.isEnabled   = false
            legend.isEnabled        = false
            isDrawHoleEnabled       = true
            holeRadius              = 72f
            transparentCircleRadius = 75f
            setHoleColor(Color.WHITE)
            centerText              = "$pctQris%\nQRIS"
            setCenterTextSize(16f)
            setCenterTextColor(Color.parseColor("#00236F"))
            setTouchEnabled(false)
            animateY(500)
            invalidate()
        }

        binding.tvQrisAmount.text  = formatRupiah(safeQris.toDouble())
        binding.tvTunaiAmount.text = formatRupiah(safeTunai.toDouble())
    }

    // ─────────────────────────────────────────────
    // Tabel Live Ranking Rider
    // ─────────────────────────────────────────────
    private fun susunTabelLiveRankingRider(petaRanking: HashMap<String, Double>) {
        val ctx = context ?: return
        binding.containerRankingRider.removeAllViews()

        val daftarUrut = petaRanking.toList().sortedByDescending { it.second }

        daftarUrut.forEachIndexed { index, item ->
            val urutan = index + 1

            val rankTextColor = when (urutan) {
                1    -> Color.parseColor("#B45309")
                2    -> Color.parseColor("#475569")
                3    -> Color.parseColor("#C2410C")
                else -> Color.parseColor("#374151")
            }
            val rankBgColor = when (urutan) {
                1    -> Color.parseColor("#FEF3C7")
                2    -> Color.parseColor("#F1F5F9")
                3    -> Color.parseColor("#FFF7ED")
                else -> Color.parseColor("#F3F4F6")
            }

            val baris = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity     = android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(ctx, 16), dp(ctx, 14), dp(ctx, 16), dp(ctx, 14))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                // Badge rank
                addView(TextView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(ctx, 36), dp(ctx, 36))
                    text     = urutan.toString()
                    textSize = 13f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(rankTextColor)
                    setBackgroundColor(rankBgColor)
                    gravity  = android.view.Gravity.CENTER
                })

                // Nama rider
                addView(TextView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                    ).apply { marginStart = dp(ctx, 12) }
                    text     = item.first
                    textSize = 14f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.parseColor("#191C1E"))
                })

                // Total omset
                addView(TextView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    text     = formatRupiah(item.second)
                    textSize = 13f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.parseColor("#00236F"))
                })
            }
            binding.containerRankingRider.addView(baris)

            // Divider
            if (urutan < daftarUrut.size) {
                binding.containerRankingRider.addView(View(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1
                    ).apply { setMargins(dp(ctx, 16), 0, dp(ctx, 16), 0) }
                    setBackgroundColor(Color.parseColor("#F3F4F6"))
                })
            }
        }
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────
    private fun dp(context: Context, dp: Int): Int =
        (dp * context.resources.displayMetrics.density).toInt()

    private fun formatRupiah(angka: Double): String =
        NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            .format(angka)
            .replace(",00", "")

    // ─────────────────────────────────────────────
    // Lifecycle – detach semua listener agar tidak memory leak
    // ─────────────────────────────────────────────
    override fun onDestroyView() {
        super.onDestroyView()
        listeners.forEach { it.remove() }
        listeners.clear()
        _binding = null
    }
}