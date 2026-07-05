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
    private var tanggalHariIni = ""
    private var docIdStokTarget = ""
    private var docIdClosing = ""

    private var totalTunaiSistem = 0L
    private var totalQrisSistem = 0L
    private var modalAwal = 0L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClosingHarianBinding.inflate(inflater, container, false)
        mFirestore = FirebaseFirestore.getInstance()
        mAuth = FirebaseAuth.getInstance()

        uidRider = mAuth.currentUser?.uid ?: ""
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        tanggalHariIni = sdf.format(Calendar.getInstance().time)
        docIdStokTarget = "${tanggalHariIni.replace("-", "")}_$uidRider"
        docIdClosing = docIdStokTarget

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

                    tampilkanLayarStatusClosing(statusValidasi, fisikDisetor)
                } else {
                    binding.layoutInputFisik.visibility = View.VISIBLE
                    binding.layoutStatusClosing.visibility = View.GONE
                }
            }
    }

    private fun tampilkanLayarStatusClosing(statusValidasi: String, fisikDisetor: Long) {
        binding.layoutInputFisik.visibility = View.GONE
        binding.layoutStatusClosing.visibility = View.VISIBLE

        val fmtRp = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        binding.tvInfoFisikTerkirim.text = "Uang Disetorkan: ${fmtRp.format(fisikDisetor).replace(",00", "")}"

        when (statusValidasi) {
            "PENDING" -> {
                binding.tvStatusValidasi.text = "⏳ PENDING"
                binding.tvStatusValidasi.setTextColor(Color.parseColor("#F57F17"))
            }
            "COCOK" -> {
                binding.tvStatusValidasi.text = "✅ SUCCESS (COCOK)"
                binding.tvStatusValidasi.setTextColor(Color.parseColor("#2E7D32"))
            }
            "SELISIH" -> {
                binding.tvStatusValidasi.text = "❌ SELISIH (CEK OWNER)"
                binding.tvStatusValidasi.setTextColor(Color.parseColor("#C62828"))
            }
            else -> {
                binding.tvStatusValidasi.text = statusValidasi
                binding.tvStatusValidasi.setTextColor(Color.GRAY)
            }
        }
    }

    private fun ambilDataKalkulasiSistem() {
        mFirestore.collection("stok_harian").document(docIdStokTarget).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    modalAwal = doc.getLong("modal_kembalian") ?: 0L
                    namaRider = doc.getString("nama_rider") ?: "Rider"

                    val statusStok = doc.getString("status_stok")
                    if (statusStok == "CLOSED") {
                        kunciLayarSudahClosing()
                    }

                    perbaruiTampilanUI()
                }
            }

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        mFirestore.collection("transactions")
            .whereEqualTo("id_rider", uidRider)
            .whereEqualTo("status_pembayaran", "SUCCESS")
            .whereGreaterThanOrEqualTo("waktu_transaksi", cal.time)
            .addSnapshotListener { snapshot, _ ->
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
        binding.tvSelisih.text = "Selisih: ${fmtRp.format(selisih).replace(",00", "")}"
    }

    private fun konfirmasiClosing() {
        val strFisik = binding.etUangFisik.text.toString().trim()
        if (strFisik.isEmpty()) {
            Toast.makeText(context, "Harap masukkan jumlah fisik uang tunai!", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Kunci Aplikasi?")
            .setMessage("Data closing akan dikirim ke Owner. Anda tidak bisa lagi menerima orderan untuk hari ini.")
            .setPositiveButton("Ya, Tutup Buku") { _, _ -> prosesKirimClosing() }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun prosesKirimClosing() {
        binding.btnKirimClosing.isEnabled = false
        val uangFisik = binding.etUangFisik.text.toString().toLong()
        val selisih = uangFisik - (modalAwal + totalTunaiSistem)

        val dataClosing = hashMapOf(
            "id_closing" to docIdClosing,
            "tanggal" to tanggalHariIni,
            "id_rider" to uidRider,
            "nama_rider" to namaRider,
            "waktu_closing_rider" to FieldValue.serverTimestamp(),
            "status_validasi" to "PENDING",
            "total_tunai_sistem" to totalTunaiSistem,
            "total_qris_sistem" to totalQrisSistem,
            "total_omset_sistem" to (totalTunaiSistem + totalQrisSistem),
            "uang_tunai_fisik" to uangFisik,
            "nominal_selisih" to selisih,
            "catatan_owner" to ""
        )

        mFirestore.runTransaction { transaction ->
            // 1. LAKUKAN SEMUA PROSES READ TERLEBIH DAHULU (Sesuai Aturan Firestore)
            val refStok = mFirestore.collection("stok_harian").document(docIdStokTarget)
            val snapStok = transaction.get(refStok)

            // 2. SETELAH READ SELESAI, LAKUKAN SEMUA PROSES WRITE/UPDATE/SET
            val refClosing = mFirestore.collection("closing_laporan").document(docIdClosing)
            transaction.set(refClosing, dataClosing)

            if (snapStok.exists()) {
                transaction.update(refStok, mapOf(
                    "status_stok" to "CLOSED",
                    "status_jualan" to "SELESAI JUALAN"
                ))
            }
            null
        }.addOnSuccessListener {
            Toast.makeText(context, "Tutup Buku Selesai!", Toast.LENGTH_LONG).show()
            kunciLayarSudahClosing()
        }.addOnFailureListener { e ->
            binding.btnKirimClosing.isEnabled = true
            Toast.makeText(context, "Gagal: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun kunciLayarSudahClosing() {
        binding.etUangFisik.isEnabled = false
        binding.btnKirimClosing.isEnabled = false
        binding.btnKirimClosing.text = "Laporan Sudah Dikirim"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}