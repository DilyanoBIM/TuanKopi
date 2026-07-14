package com.example.tuankopi.AktivitasRider

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.tuankopi.RiderDashboardActivity
import com.example.tuankopi.databinding.FragmentClosingHarianBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ClosingHarianFragment : Fragment() {

    private var _binding: FragmentClosingHarianBinding? = null
    private val binding get() = _binding!!

    private lateinit var mFirestore: FirebaseFirestore
    private lateinit var mAuth: FirebaseAuth

    private var uidRider = ""
    private var namaRider = ""
    private var tanggalTarget = ""
    private var docIdStokTarget = ""
    private var docIdClosing = ""

    private var totalTunaiSistem = 0L
    private var totalQrisSistem = 0L
    private var modalAwal = 0L

    companion object {
        fun newInstance(tanggal: String): ClosingHarianFragment {
            val fragment = ClosingHarianFragment()
            val args = Bundle().apply { putString("TANGGAL", tanggal) }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClosingHarianBinding.inflate(inflater, container, false)
        mFirestore = FirebaseFirestore.getInstance()
        mAuth = FirebaseAuth.getInstance()

        uidRider = mAuth.currentUser?.uid ?: ""
        tanggalTarget = arguments?.getString("TANGGAL") ?: ""

        val cleanTgl = tanggalTarget.replace("-", "")
        docIdStokTarget = "${cleanTgl}_$uidRider"
        docIdClosing = "${tanggalTarget}_$uidRider"

        val tanggalIndo = formatKeTanggalIndo(tanggalTarget)
        binding.tvInfoTanggalClosing.text = "Closing: $tanggalIndo"

        binding.btnBackDetail.setOnClickListener {
            val tglFragment = AktivitasPilihTanggalFragment.newInstance("CLOSING")
            (activity as? RiderDashboardActivity)?.gantiRiderFragment(tglFragment)
        }

        pantauStatusLaporanClosing()
        ambilDataKalkulasiSistem()

        binding.etUangFisik.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { hitungSelisih() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.btnKirimClosing.setOnClickListener {
            konfirmasiClosing()
        }

        return binding.root
    }

    private fun pantauStatusLaporanClosing() {
        mFirestore.collection("closing_laporan").document(docIdClosing)
            .addSnapshotListener { snapshot, error ->
                if (error != null || !isAdded) return@addSnapshotListener

                if (snapshot != null && snapshot.exists()) {
                    val statusValidasi = snapshot.getString("status_validasi") ?: "PENDING"
                    val fisikDisetor = snapshot.getLong("uang_tunai_fisik") ?: 0L
                    val catatanOwner = snapshot.getString("catatan_owner") ?: ""

                    // Hanya isi otomatis jika EditText sedang dalam keadaan terkunci
                    // agar tidak mengganggu jika Rider sedang mengetik ulang
                    if (!binding.etUangFisik.isEnabled) {
                        binding.etUangFisik.setText(fisikDisetor.toString())
                    }

                    tampilkanLayarStatusClosing(statusValidasi, fisikDisetor, catatanOwner)
                } else {
                    binding.tvInfoFisikTerkirim.text = "Total Setoran Diserahkan: Rp 0"
                    binding.tvStatusValidasi.text = "BELUM MELAPORKAN PENJUALAN"
                    binding.tvStatusValidasi.setTextColor(Color.GRAY)
                }
            }
    }

    private fun tampilkanLayarStatusClosing(statusValidasi: String, fisikDisetor: Long, catatanOwner: String) {

        val fmtRp = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        binding.tvInfoFisikTerkirim.text = "Total Setoran Diserahkan: ${fmtRp.format(fisikDisetor).replace(",00", "")}"

        when (statusValidasi.uppercase(Locale.getDefault())) {
            "PENDING" -> {
                binding.tvStatusValidasi.text = "⏳ MENUNGGU OWNER VALIDASI"
                binding.tvStatusValidasi.setTextColor(Color.parseColor("#F57F17"))
                kunciLayarSudahClosing()
            }
            "COCOK" -> {
                binding.tvStatusValidasi.text = "✅ SUCCESS (COCOK)"
                binding.tvStatusValidasi.setTextColor(Color.parseColor("#2E7D32"))
                kunciLayarSudahClosing()
            }
            "SELISIH" -> {
                val teksTampil = if (catatanOwner.isNotEmpty()) {
                    "❌ SELISIH\n(Catatan: $catatanOwner)"
                } else {
                    "❌ SELISIH (DITOLAK)"
                }
                binding.tvStatusValidasi.text = teksTampil
                binding.tvStatusValidasi.setTextColor(Color.parseColor("#C62828"))

                // Jika statusnya SELISIH, panggil fungsi untuk membuka penguncian input
                bukaLayarUntukRevisi()
            }
            else -> {
                binding.tvStatusValidasi.text = statusValidasi
                binding.tvStatusValidasi.setTextColor(Color.GRAY)
                kunciLayarSudahClosing()
            }
        }
    }

    private fun ambilDataKalkulasiSistem() {
        mFirestore.collection("stok_harian").document(docIdStokTarget).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    modalAwal = doc.getLong("modal_kembalian") ?: 0L
                    namaRider = doc.getString("nama_rider") ?: "Rider"

                    // (Logika kunci layar dihapus dari sini agar tidak bertabrakan dengan listener laporan closing)
                    perbaruiTampilanUI()
                }
            }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateTarget = try { sdf.parse(tanggalTarget) } catch (e: Exception) { null } ?: return

        val cal = Calendar.getInstance().apply { time = dateTarget }
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.time

        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
        val endOfDay = cal.time

        mFirestore.collection("transactions")
            .whereEqualTo("id_rider", uidRider)
            .whereEqualTo("status_pembayaran", "SUCCESS")
            .whereGreaterThanOrEqualTo("waktu_transaksi", startOfDay)
            .whereLessThanOrEqualTo("waktu_transaksi", endOfDay)
            .addSnapshotListener { snapshot, error ->
                if (snapshot == null || !isAdded) return@addSnapshotListener

                Thread {
                    var tunaiLokal = 0L
                    var qrisLokal = 0L

                    snapshot.documents.forEach { doc ->
                        val metode = doc.getString("metode_pembayaran") ?: ""
                        val total = doc.getLong("total_harga") ?: 0L
                        if (metode == "TUNAI") tunaiLokal += total
                        else if (metode == "QRIS") qrisLokal += total
                    }

                    activity?.runOnUiThread {
                        if (!isAdded) return@runOnUiThread
                        totalTunaiSistem = tunaiLokal
                        totalQrisSistem = qrisLokal
                        perbaruiTampilanUI()
                    }
                }.start()
            }
    }

    private fun perbaruiTampilanUI() {
        if (!isAdded) return
        val fmtRp = NumberFormat.getCurrencyInstance(Locale("in", "ID"))

        binding.tvTunaiSistem.text = fmtRp.format(totalTunaiSistem).replace(",00", "")
        binding.tvQrisSistem.text = fmtRp.format(totalQrisSistem).replace(",00", "")
        binding.tvModalSistem.text = fmtRp.format(modalAwal).replace(",00", "")

        val totalPenjualanSistem = totalTunaiSistem + totalQrisSistem
        binding.tvTotalPenjualanSistem.text = fmtRp.format(totalPenjualanSistem).replace(",00", "")

        val totalSeharusnyaAda = modalAwal + totalTunaiSistem
        binding.tvTargetFisik.text = "Target Uang di Laci: ${fmtRp.format(totalSeharusnyaAda).replace(",00", "")}"

        hitungSelisih()
    }

    private fun hitungSelisih() {
        val strFisik = binding.etUangFisik.text.toString().trim()
        val uangFisik = if (strFisik.isEmpty()) 0L else strFisik.toLong()
        val targetLaci = modalAwal + totalTunaiSistem
        val selisih = uangFisik - targetLaci

        val fmtRp = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        binding.tvSelisih.text = "Selisih Perhitungan: ${fmtRp.format(selisih).replace(",00", "")}"
    }

    private fun konfirmasiClosing() {
        val strFisik = binding.etUangFisik.text.toString().trim()
        if (strFisik.isEmpty()) {
            Toast.makeText(context, "Harap masukkan jumlah fisik uang tunai di kotak kasir!", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Kirim Laporan Closing?")
            .setMessage("Data closing akan direkam dan dikirim ke Owner untuk divalidasi.")
            .setPositiveButton("Ya, Kirim Laporan") { _, _ -> prosesKirimClosing() }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun prosesKirimClosing() {
        binding.btnKirimClosing.isEnabled = false
        val uangFisik = binding.etUangFisik.text.toString().toLong()
        val selisih = uangFisik - (modalAwal + totalTunaiSistem)

        val dataUpdate = hashMapOf(
            "waktu_closing_rider" to FieldValue.serverTimestamp(),
            "status_validasi" to "PENDING",
            "uang_tunai_fisik" to uangFisik,
            "nominal_selisih" to selisih,
            "catatan_owner" to ""
        )

        mFirestore.runTransaction { transaction ->
            val refClosing = mFirestore.collection("closing_laporan").document(docIdClosing)

            // Gunakan update untuk revisi
            transaction.update(refClosing, dataUpdate)

            // Update status stok (jika perlu)
            val refStok = mFirestore.collection("stok_harian").document(docIdStokTarget)
            transaction.update(refStok, "status_stok", "CLOSED")

            null
        }.addOnSuccessListener {
            Toast.makeText(context, "Laporan berhasil dikirim ulang!", Toast.LENGTH_SHORT).show()
            kunciLayarSudahClosing()
        }.addOnFailureListener { e ->
            binding.btnKirimClosing.isEnabled = true
            Toast.makeText(context, "Gagal: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun kunciLayarSudahClosing() {
        binding.etUangFisik.isEnabled = false
        binding.btnKirimClosing.isEnabled = false
        binding.btnKirimClosing.text = "Laporan Sudah Terkunci"
        binding.btnKirimClosing.setBackgroundColor(Color.GRAY)
    }

    // Fungsi baru untuk membuka kunci layar agar Rider bisa melakukan revisi
    private fun bukaLayarUntukRevisi() {
        binding.etUangFisik.isEnabled = true
        binding.btnKirimClosing.isEnabled = true
        binding.btnKirimClosing.text = "KIRIM ULANG LAPORAN"
        binding.btnKirimClosing.setBackgroundColor(Color.parseColor("#00236F"))
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}