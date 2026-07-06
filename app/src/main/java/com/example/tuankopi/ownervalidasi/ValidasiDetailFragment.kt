package com.example.tuankopi.ownervalidasi

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

        binding.btnValidasiCocok.setOnClickListener { prosesValidasi("COCOK") }
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

        binding.tvNamaDanTanggal.text = "Rider: ${laporan.nama_rider} | Tanggal: ${laporan.tanggal}"
        binding.tvTunaiSistem.text = "Total Tunai Sistem: ${fmtRp.format(laporan.total_tunai_sistem)}"
        binding.tvQrisSistem.text = "Total QRIS Sistem: ${fmtRp.format(laporan.total_qris_sistem)}"
        binding.tvTotalOmset.text = "Total Omset (Kotor): ${fmtRp.format(laporan.total_omset_sistem)}"

        binding.tvUangFisikDilaporkan.text = "Fisik Diserahkan Rider: ${fmtRp.format(laporan.uang_tunai_fisik)}"

        if (laporan.nominal_selisih != 0L) {
            binding.tvStatusSelisih.text = "Selisih Tercatat Rider: ${fmtRp.format(laporan.nominal_selisih)}"
            binding.tvStatusSelisih.setTextColor(android.graphics.Color.RED)
        } else {
            binding.tvStatusSelisih.text = "Selisih: Rp 0 (Balance)"
            binding.tvStatusSelisih.setTextColor(android.graphics.Color.GREEN)
        }
    }

    private fun prosesValidasi(statusUpdate: String) {
        val catatan = binding.etCatatanOwner.text.toString().trim()

        // Validasi catatan jika Owner menandai status sebagai SELISIH
        if (statusUpdate == "SELISIH" && catatan.isEmpty()) {
            Toast.makeText(context, "Harap isi catatan untuk memberitahu Rider terkait selisih!", Toast.LENGTH_SHORT).show()
            return
        }

        // Kunci tombol agar tidak double-click
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
                    parentFragmentManager.popBackStack() // Kembali ke daftar List
                }
            }
            .addOnFailureListener { e ->
                binding.btnValidasiCocok.isEnabled = true
                binding.btnTolakSelisih.isEnabled = true
                Toast.makeText(context, "Gagal memperbarui validasi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}