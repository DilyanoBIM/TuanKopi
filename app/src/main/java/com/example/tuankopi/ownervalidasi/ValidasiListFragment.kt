package com.example.tuankopi.ownervalidasi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tuankopi.R
import com.example.tuankopi.databinding.FragmentValidasiListBinding
import com.example.tuankopi.databinding.ItemValidasiSetoranBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ValidasiListFragment : Fragment() {
    private var _binding: FragmentValidasiListBinding? = null
    private val binding get() = _binding!!
    private lateinit var mFirestore: FirebaseFirestore

    private var masterClosingList = listOf<ClosingLaporan>()
    private var currentMode = "TANGGAL"
    private var selectedTanggal = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentValidasiListBinding.inflate(inflater, container, false)
        mFirestore = FirebaseFirestore.getInstance()

        binding.rvValidasi.layoutManager = LinearLayoutManager(requireContext())

        binding.btnBackMode.setOnClickListener {
            currentMode = "TANGGAL"
            updateUI()
        }

        loadSeluruhDataClosing()
        return binding.root
    }

    private fun loadSeluruhDataClosing() {
        // IMPROVISASI: Ambil semua data tanpa filter PENDING agar riwayat lama bisa diaudit ulang
        mFirestore.collection("closing_laporan")
            .orderBy("tanggal", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || !isAdded) return@addSnapshotListener
                masterClosingList = snapshot?.documents?.mapNotNull { it.toObject(ClosingLaporan::class.java) } ?: emptyList()
                updateUI()
            }
    }

    private fun updateUI() {
        if (currentMode == "TANGGAL") {
            binding.btnBackMode.visibility = View.GONE
            binding.tvHeaderList.text = "Pilih Tanggal Closing"

            // Ambil daftar tanggal unik
            val listTanggalUnique = masterClosingList.map { it.tanggal }.distinct()

            binding.rvValidasi.adapter = ValidasiAdapter(listTanggalUnique, isModeTanggal = true) { item ->
                selectedTanggal = item.tanggal
                currentMode = "RIDER"
                updateUI()
            }
        } else {
            binding.btnBackMode.visibility = View.VISIBLE
            binding.tvHeaderList.text = "Rider pada Tanggal $selectedTanggal"

            // Filter rider yang memiliki data closing di tanggal terpilih
            val listRiderBerdasarkanTanggal = masterClosingList.filter { it.tanggal == selectedTanggal }

            binding.rvValidasi.adapter = ValidasiAdapter(listRiderBerdasarkanTanggal, isModeTanggal = false) { item ->
                bukaDetailValidasi(item.id_closing)
            }
        }
    }

    private fun bukaDetailValidasi(idClosing: String) {
        val detailFragment = ValidasiDetailFragment.newInstance(idClosing)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, detailFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class ValidasiAdapter(
        private val dataList: List<Any>,
        private val isModeTanggal: Boolean,
        private val onClick: (ClosingLaporan) -> Unit
    ) : RecyclerView.Adapter<ValidasiAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemValidasiSetoranBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ItemValidasiSetoranBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (isModeTanggal) {
                val tanggal = dataList[position] as String
                holder.binding.tvTanggalClosing.text = "📅 Laporan Hari: $tanggal"
                holder.binding.tvNamaRider.text = "Klik untuk memeriksa setoran masuk"
                holder.binding.tvStatusValidasi.visibility = View.GONE
                holder.binding.root.setOnClickListener { onClick(ClosingLaporan(tanggal = tanggal)) }
            } else {
                val item = dataList[position] as ClosingLaporan
                holder.binding.tvTanggalClosing.text = "👤 Rider: ${item.nama_rider}"
                holder.binding.tvNamaRider.text = "Fisik Diserahkan: Rp ${item.uang_tunai_fisik}"
                holder.binding.tvStatusValidasi.visibility = View.VISIBLE

                // Set warna label status list berdasarkan isi data pembukuan harian
                when(item.status_validasi) {
                    "PENDING" -> {
                        holder.binding.tvStatusValidasi.text = "⏳ PENDING AUDIT"
                        holder.binding.tvStatusValidasi.setTextColor(android.graphics.Color.parseColor("#F57F17"))
                    }
                    "SUCCESS", "COCOK" -> {
                        holder.binding.tvStatusValidasi.text = "✅ VALID (SUCCESS)"
                        holder.binding.tvStatusValidasi.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
                    }
                    "SELISIH" -> {
                        holder.binding.tvStatusValidasi.text = "❌ TERDAPAT SELISIH"
                        holder.binding.tvStatusValidasi.setTextColor(android.graphics.Color.parseColor("#C62828"))
                    }
                    else -> holder.binding.tvStatusValidasi.text = item.status_validasi
                }
                holder.binding.root.setOnClickListener { onClick(item) }
            }
        }

        override fun getItemCount(): Int = dataList.size
    }
}