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

    private val listeners = mutableListOf<ListenerRegistration>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding   = FragmentHomeBinding.inflate(inflater, container, false)
        mAuth      = FirebaseAuth.getInstance()
        mFirestore = FirebaseFirestore.getInstance()

        // Menampilkan informasi Hari/Tanggal saat ini (NOW)
        val sdfIndo = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
        binding.tvTanggalHariIni.text = sdfIndo.format(Calendar.getInstance().time)

        muatDataProfilOwner()
        muatAnalitikBisnisRealtime()

        return binding.root
    }

    private fun muatDataProfilOwner() {
        val uid = mAuth.currentUser?.uid ?: return
        mFirestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (!isAdded || doc == null || !doc.exists()) return@addOnSuccessListener
                val namaUser = doc.getString("nama") ?: "User"
                val roleUser = doc.getString("role") ?: "owner"
                binding.tvNamaDashboard.text = "$namaUser ($roleUser)"
            }
            .addOnFailureListener {
                if (isAdded) binding.tvNamaDashboard.text = "Owner Tuan Kopi"
            }
    }

    private fun muatAnalitikBisnisRealtime() {
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val tanggalHariIni = sdf.format(cal.time)

        val calMinggu = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val waktuMulaiMinggu = calMinggu.time

        val calBulan = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val waktuMulaiBulan = calBulan.time

        val regTransactions = mFirestore.collection("transactions")
            .whereEqualTo("status_pembayaran", "SUCCESS")
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null || !isAdded) return@addSnapshotListener

                var omsetHariIni   = 0.0
                var omsetMingguIni = 0.0
                var omsetBulanIni  = 0.0

                var totalTunaiHariIni = 0f
                var totalQrisHariIni  = 0f

                val petaRankingRider  = HashMap<String, Double>()

                snapshots.documents.forEachIndexed { _, doc ->
                    val totalHarga        = doc.getLong("total_harga")?.toDouble() ?: 0.0
                    val metodePembayaran  = doc.getString("metode_pembayaran") ?: "TUNAI"
                    val namaRider         = doc.getString("nama_rider") ?: "Rider"
                    val waktuTransaksi    = doc.getTimestamp("waktu_transaksi")?.toDate()

                    if (waktuTransaksi != null) {
                        val tanggalTransStr = sdf.format(waktuTransaksi)

                        if (tanggalTransStr == tanggalHariIni) {
                            omsetHariIni += totalHarga
                            if (metodePembayaran == "QRIS") {
                                totalQrisHariIni += totalHarga.toFloat()
                            } else {
                                totalTunaiHariIni += totalHarga.toFloat()
                            }

                            petaRankingRider[namaRider] = (petaRankingRider[namaRider] ?: 0.0) + totalHarga
                        }

                        if (waktuTransaksi.after(waktuMulaiMinggu) || tanggalTransStr == sdf.format(waktuMulaiMinggu)) {
                            omsetMingguIni += totalHarga
                        }

                        if (waktuTransaksi.after(waktuMulaiBulan) || tanggalTransStr == sdf.format(waktuMulaiBulan)) {
                            omsetBulanIni += totalHarga
                        }
                    }
                }

                if (!isAdded) return@addSnapshotListener

                binding.tvOmsetHarian.text   = formatRupiah(omsetHariIni)
                binding.tvOmsetMingguan.text = formatRupiah(omsetMingguIni)
                binding.tvOmsetBulanan.text  = formatRupiah(omsetBulanIni)

                tampilkanPieChartMetodePembayaran(totalTunaiHariIni, totalQrisHariIni)
                susunTabelLiveRankingRider(petaRankingRider)

                hitungLabaBersih(omsetHariIni, tanggalHariIni)
            }
        listeners.add(regTransactions)
    }

    private fun hitungLabaBersih(omsetHariIni: Double, tanggalHariIni: String) {
        mFirestore.collection("operasional_expenses")
            .whereEqualTo("tanggal", tanggalHariIni)
            .get()
            .addOnSuccessListener { snapshots ->
                if (snapshots == null || !isAdded) return@addOnSuccessListener
                var totalPengeluaranField = 0.0
                for (doc in snapshots.documents) {
                    totalPengeluaranField += doc.getLong("nominal")?.toDouble() ?: 0.0
                }

                val labaBersihFinal = omsetHariIni - (omsetHariIni * 0.4) - totalPengeluaranField
                if (isAdded) binding.tvLabaBersih.text = formatRupiah(labaBersihFinal)
            }
    }

    private fun tampilkanPieChartMetodePembayaran(tunai: Float, qris: Float) {
        val safeQris  = if (tunai == 0f && qris == 0f) 0f else qris
        val safeTunai = if (tunai == 0f && qris == 0f) 0f else tunai
        val total     = safeQris + safeTunai
        val pctQris   = if (total > 0) ((safeQris / total) * 100).toInt() else 0

        val entriPie = arrayListOf(
            PieEntry(if (total == 0f) 1f else safeQris, "QRIS"),
            PieEntry(if (total == 0f) 0f else safeTunai, "Tunai")
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

    private fun susunTabelLiveRankingRider(petaRanking: HashMap<String, Double>) {
        val ctx = context ?: return
        binding.containerRankingRider.removeAllViews()

        if (petaRanking.isEmpty()) {
            binding.containerRankingRider.addView(TextView(ctx).apply {
                text = "Belum ada transaksi hari ini"
                setPadding(32, 32, 32, 32)
                gravity = android.view.Gravity.CENTER
                textSize = 13f
                setTextColor(Color.GRAY)
            })
            return
        }

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

                addView(TextView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(ctx, 36), dp(ctx, 36))
                    text     = urutan.toString()
                    textSize = 13f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(rankTextColor)
                    setBackgroundColor(rankBgColor)
                    gravity  = android.view.Gravity.CENTER
                })

                addView(TextView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                    ).apply { marginStart = dp(ctx, 12) }
                    text     = item.first
                    textSize = 14f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.parseColor("#191C1E"))
                })

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

    private fun dp(context: Context, dp: Int): Int =
        (dp * context.resources.displayMetrics.density).toInt()

    private fun formatRupiah(angka: Double): String =
        NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            .format(angka)
            .replace(",00", "")

    override fun onDestroyView() {
        super.onDestroyView()
        listeners.forEach { it.remove() }
        listeners.clear()
        _binding = null
    }
}