package com.example.tuankopi

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.tuankopi.databinding.FragmentRiderDetailKonfirmasiBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import java.text.NumberFormat
import java.util.Locale

class RiderDetailKonfirmasiFragment : Fragment() {

    private var _binding: FragmentRiderDetailKonfirmasiBinding? = null
    private val binding get() = _binding!!

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mFirestore: FirebaseFirestore
    private var detailListener: ListenerRegistration? = null

    private var tanggalTarget = ""
    private var docIdStokTarget = ""
    private var isLocked = false
    private val petaVerifikasiItem = HashMap<String, Int>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRiderDetailKonfirmasiBinding.inflate(inflater, container, false)
        mAuth = FirebaseAuth.getInstance()
        mFirestore = FirebaseFirestore.getInstance()

        // Fungsi Tombol Kembali menuju Daftar Tanggal Logistik
        binding.btnBackDetail.setOnClickListener {
            (activity as? RiderDashboardActivity)?.gantiRiderFragment(RiderKonfirmasiStokFragment())
        }

        tanggalTarget = arguments?.getString("KEY_TANGGAL") ?: ""
        binding.tvTanggalTerpilih.text = "Tanggal: $tanggalTarget"

        val uidRider = mAuth.currentUser?.uid ?: ""
        val cleanTgl = tanggalTarget.replace("-", "")
        docIdStokTarget = "${cleanTgl}_$uidRider"

        dengarkanLiveDetailStok()

        binding.btnTerimaStokFinal.setOnClickListener {
            eksekusiKonfirmasiTerimaStokKolektif()
        }

        return binding.root
    }

    private fun dengarkanLiveDetailStok() {
        detailListener = mFirestore.collection("stok_harian").document(docIdStokTarget)
            .addSnapshotListener { snapshot, error ->
                if (error != null || !isAdded) return@addSnapshotListener
                if (snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val statusStokUtama = snapshot.getString("status_stok") ?: "CLOSED"
                isLocked = (statusStokUtama == "AKTIF" || statusStokUtama == "OPEN")

                binding.containerDetailItemStok.removeAllViews()

                val modalKembalian = snapshot.getLong("modal_kembalian") ?: 0L
                val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
                val textModalRupiah = formatter.format(modalKembalian).replace(",00", "")

                if (!petaVerifikasiItem.containsKey("KEY_MODAL")) {
                    petaVerifikasiItem["KEY_MODAL"] = if (isLocked) 1 else 0
                }
                tampilkanBarisVerifikasiGenerik("KEY_MODAL", "💰 Modal Cash Awal Keliling", textModalRupiah)

                val detailStokMap = snapshot.get("detail_stok") as? Map<*, *> ?: return@addSnapshotListener
                var totalItemHitung = 1

                for ((key, value) in detailStokMap) {
                    val idProduk = key.toString()
                    val dataItem = value as? Map<*, *> ?: continue
                    val namaProduk = dataItem["nama_produk"] as? String ?: "Menu Kopi"
                    val jatahStokTotal = dataItem["stok_total"] as? Long ?: (dataItem["stok_awal"] as? Long ?: 0L)
                    val statusDiterimaItem = dataItem["diterima"] as? Boolean ?: false

                    totalItemHitung++

                    if (!petaVerifikasiItem.containsKey(idProduk)) {
                        petaVerifikasiItem[idProduk] = if (statusDiterimaItem) 1 else 0
                    }

                    tampilkanBarisVerifikasiGenerik(idProduk, namaProduk, "$jatahStokTotal Cup")
                }

                evaluasiStateTombolKeputusan(totalItemHitung)
            }
    }

    private fun tampilkanBarisVerifikasiGenerik(idKey: String, namaLabel: String, valueLabel: String) {
        val ctx = context ?: return

        val cardItem = CardView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dpToPx(ctx, 12))
            }
            radius = dpToPx(ctx, 8).toFloat()
            cardElevation = dpToPx(ctx, 2).toFloat()
            setCardBackgroundColor(Color.WHITE)
        }

        val susunanKonten = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val p = dpToPx(ctx, 16)
            setPadding(p, p, p, p)
        }

        val tvInfo = TextView(ctx).apply {
            text = "$namaLabel — $valueLabel"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#191C1E"))
        }
        susunanKonten.addView(tvInfo)

        val groupTombol = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dpToPx(ctx, 10)
            }
        }

        val btnSesuai = Button(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(ctx, 40))
            text = "SESUAI"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            isEnabled = !isLocked

            if (petaVerifikasiItem[idKey] == 1) {
                setBackgroundColor(Color.parseColor("#2E7D32"))
                setTextColor(Color.WHITE)
            } else {
                setBackgroundColor(Color.parseColor("#E0E0E0"))
                setTextColor(Color.BLACK)
            }
        }

        btnSesuai.setOnClickListener {
            if (isLocked) return@setOnClickListener
            petaVerifikasiItem[idKey] = 1

            btnSesuai.setBackgroundColor(Color.parseColor("#2E7D32"))
            btnSesuai.setTextColor(Color.WHITE)

            evaluasiStateTombolKeputusan(petaVerifikasiItem.size)
        }

        groupTombol.addView(btnSesuai)
        susunanKonten.addView(groupTombol)
        cardItem.addView(susunanKonten)
        binding.containerDetailItemStok.addView(cardItem)
    }

    private fun evaluasiStateTombolKeputusan(totalItem: Int) {
        if (isLocked) {
            binding.tvStatusKunciStok.text = "Stok aktif! Selamat berjualan."
            binding.tvStatusKunciStok.visibility = View.VISIBLE
            binding.btnTerimaStokFinal.visibility = View.GONE
            return
        }

        if (petaVerifikasiItem.isEmpty()) return
        var jumlahSudahPilih = 0

        for ((_, status) in petaVerifikasiItem) {
            if (status == 1) jumlahSudahPilih++
        }

        binding.tvStatusKunciStok.visibility = View.GONE

        // Tampilkan tombol final hanya jika semua item sudah diklik "Sesuai"
        if (jumlahSudahPilih == totalItem) {
            binding.btnTerimaStokFinal.visibility = View.VISIBLE
        } else {
            binding.btnTerimaStokFinal.visibility = View.GONE
        }
    }

    private fun eksekusiKonfirmasiTerimaStokKolektif() {
        val refRider = mFirestore.collection("stok_harian").document(docIdStokTarget)

        mFirestore.runTransaction { transaction ->
            val snapshotRider = transaction.get(refRider)
            if (!snapshotRider.exists()) {
                throw FirebaseFirestoreException("Dokumen jatah harian tidak ditemukan!", FirebaseFirestoreException.Code.NOT_FOUND)
            }

            val rawDetailStok = snapshotRider.get("detail_stok") as? Map<*, *> ?: HashMap<String, Any>()
            val detailStokTerbarui = HashMap<String, Any>()

            for ((k, v) in rawDetailStok) {
                val idProd = k.toString()
                val subMapDataKopi = v as? Map<*, *> ?: continue
                val subMapTerbarui = HashMap<String, Any>()

                for ((subKey, subVal) in subMapDataKopi) {
                    subMapTerbarui[subKey.toString()] = subVal!!
                }

                if (petaVerifikasiItem[idProd] == 1) {
                    subMapTerbarui["diterima"] = true
                }

                detailStokTerbarui[idProd] = subMapTerbarui
            }

            transaction.update(refRider, "detail_stok", detailStokTerbarui)
            transaction.update(refRider, "status_stok", "AKTIF")

            null
        }.addOnSuccessListener {
            Toast.makeText(context, "Seluruh jatah muatan & modal keliling berhasil Anda konfirmasi!", Toast.LENGTH_SHORT).show()
            (activity as? RiderDashboardActivity)?.gantiRiderFragment(RiderDashboardFragment())
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Gagal mengonfirmasi: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun dpToPx(c: Context, dp: Int): Int = (dp * c.resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        detailListener?.remove()
        _binding = null
    }
}