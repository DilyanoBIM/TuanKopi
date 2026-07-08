package com.example.tuankopi.ownervalidasi

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.tuankopi.databinding.FragmentValidasiDetailBinding
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

class ValidasiDetailFragment : Fragment() {
    private var _binding: FragmentValidasiDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var mFirestore: FirebaseFirestore

    private var idClosingLaporan: String? = null
    private var nominalSelisih: Long = 0L

    companion object {
        fun newInstance(idClosing: String) = ValidasiDetailFragment().apply {
            arguments = Bundle().apply { putString("ID_CLOSING", idClosing) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentValidasiDetailBinding.inflate(inflater, container, false)
        mFirestore = FirebaseFirestore.getInstance()
        idClosingLaporan = arguments?.getString("ID_CLOSING")

        loadDetailSetoran()

        binding.btnValidasiCocok.setOnClickListener { prosesValidasi("SUCCESS") }
        binding.btnTolakSelisih.setOnClickListener { prosesValidasi("SELISIH") }

        return binding.root
    }

    private fun loadDetailSetoran() {
        if (idClosingLaporan == null) return

        mFirestore.collection("closing_laporan").document(idClosingLaporan!!).get()
            .addOnSuccessListener { doc ->
                if (doc.exists() && isAdded) {
                    val laporan = doc.toObject(ClosingLaporan::class.java)
                    tampilkanData(laporan)
                }
            }
    }

    private fun tampilkanData(laporan: ClosingLaporan?) {
        if (laporan == null) return
        val fmtRp = NumberFormat.getCurrencyInstance(Locale("in", "ID"))

        nominalSelisih = laporan.nominal_selisih

        binding.tvNamaDanTanggal.text = "👤 Rider: ${laporan.nama_rider}\n📅 Tanggal: ${laporan.tanggal}"
        binding.tvTunaiSistem.text = fmtRp.format(laporan.total_tunai_sistem).replace(",00", "")
        binding.tvQrisSistem.text = fmtRp.format(laporan.total_qris_sistem).replace(",00", "")
        binding.tvTotalOmset.text = fmtRp.format(laporan.total_omset_sistem).replace(",00", "")
        binding.tvUangFisikDilaporkan.text = fmtRp.format(laporan.uang_tunai_fisik).replace(",00", "")

        if (laporan.catatan_owner.isNotEmpty()) {
            binding.etCatatanOwner.setText(laporan.catatan_owner)
        }

        if (nominalSelisih < 0) {
            binding.tvStatusSelisih.text = "⚠️ MINUS / KURANG: ${fmtRp.format(nominalSelisih).replace(",00", "")}"
            binding.tvStatusSelisih.setTextColor(Color.parseColor("#C62828"))
        } else if (nominalSelisih > 0) {
            binding.tvStatusSelisih.text = "💰 SURPLUS / LEBIH: +${fmtRp.format(nominalSelisih).replace(",00", "")}"
            binding.tvStatusSelisih.setTextColor(Color.parseColor("#F57F17"))
        } else {
            binding.tvStatusSelisih.text = "✅ SETORAN MATCH (BALANCE)"
            binding.tvStatusSelisih.setTextColor(Color.parseColor("#2E7D32"))
        }
    }

    private fun prosesValidasi(statusUpdate: String) {
        var catatan = binding.etCatatanOwner.text.toString().trim()

        if (statusUpdate == "SELISIH" && catatan.isEmpty()) {
            val fmtRp = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            catatan = "Audit Owner: Ditemukan ketidakcocokan saldo sebesar ${fmtRp.format(nominalSelisih).replace(",00", "")}. Harap hitung ulang modal kembalian Anda."
            binding.etCatatanOwner.setText(catatan)
            Toast.makeText(context, "Catatan otomatis ditambahkan sistem.", Toast.LENGTH_SHORT).show()
        }

        binding.btnValidasiCocok.isEnabled = false
        binding.btnTolakSelisih.isEnabled = false

        val updates = mapOf(
            "status_validasi" to statusUpdate,
            "waktu_validasi_owner" to FieldValue.serverTimestamp(),
            "catatan_owner" to catatan
        )

        mFirestore.collection("closing_laporan").document(idClosingLaporan!!)
            .update(updates)
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(context, "Validasi Berhasil Disimpan ($statusUpdate)", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
            .addOnFailureListener { e ->
                binding.btnValidasiCocok.isEnabled = true
                binding.btnTolakSelisih.isEnabled = true
                Toast.makeText(context, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}