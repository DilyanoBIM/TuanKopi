package com.example.tuankopi.riwayat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tuankopi.databinding.FragmentRiwayatPenjualanBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class RiwayatPenjualanFragment : Fragment() {
    private var _binding: FragmentRiwayatPenjualanBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    private val transactionList = mutableListOf<TransactionModel>()
    private lateinit var adapter: RiwayatPenjualanAdapter

    // Variabel Filter
    private var startDate: Date? = null
    private var endDate: Date? = null
    private var selectedRiderId: String = "ALL"
    private var selectedMetode: String = "ALL"

    // Map untuk menyimpan ID Rider (Nama Rider -> ID Rider)
    private val riderMap = mutableMapOf<String, String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRiwayatPenjualanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFilters()
        fetchRiders() // Mengambil daftar Rider untuk Spinner
        loadData()    // Load data awal (Semua waktu, Semua Rider, Semua Metode)
    }

    private fun setupRecyclerView() {
        adapter = RiwayatPenjualanAdapter(transactionList)
        binding.rvRiwayatPenjualan.layoutManager = LinearLayoutManager(context)
        binding.rvRiwayatPenjualan.adapter = adapter
    }

    private fun setupFilters() {
        // 1. Filter Rentang Tanggal menggunakan MaterialDatePicker
        binding.btnDateRange.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Pilih Rentang Tanggal")
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                // Konversi milidetik ke Date
                startDate = Date(selection.first)
                // Tambahkan 1 hari (minus 1 milidetik) untuk cover sampai akhir hari endDate
                endDate = Date(selection.second + 86399999)

                val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
                // TAMBAHKAN !! PADA startDate DAN endDate
                binding.btnDateRange.text = "${sdf.format(startDate!!)} - ${sdf.format(endDate!!)}"
                loadData() // Refresh Data
            }
            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }

        // 2. Filter Metode Pembayaran
        val metodeOptions = arrayOf("Semua Pembayaran", "TUNAI", "QRIS")
        binding.spinnerMetode.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, metodeOptions)
        binding.spinnerMetode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                selectedMetode = when(position) {
                    1 -> "TUNAI"
                    2 -> "QRIS"
                    else -> "ALL"
                }
                loadData()
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun fetchRiders() {
        // Ambil dari koleksi users yang role-nya "rider"
        db.collection("users")
            .whereEqualTo("role", "rider")
            .get()
            .addOnSuccessListener { documents ->
                val riderNames = mutableListOf("Semua Rider")
                riderMap["Semua Rider"] = "ALL"

                for (doc in documents) {
                    val nama = doc.getString("nama") ?: "Unknown"
                    val uid = doc.getString("uid") ?: ""
                    riderNames.add(nama)
                    riderMap[nama] = uid
                }

                val riderAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, riderNames)
                binding.spinnerRider.adapter = riderAdapter
                binding.spinnerRider.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                        val selectedName = riderNames[position]
                        selectedRiderId = riderMap[selectedName] ?: "ALL"
                        loadData()
                    }
                    override fun onNothingSelected(p0: AdapterView<*>?) {}
                }
            }
    }

    private fun loadData() {
        var query: Query = db.collection("transactions")
            .whereEqualTo("status_pembayaran", "SUCCESS") // Hanya ambil yang sukses
            .orderBy("waktu_transaksi", Query.Direction.DESCENDING)

        // Eksekusi Query
        query.get().addOnSuccessListener { documents ->
            transactionList.clear()
            var totalOmset = 0L

            for (doc in documents) {
                val data = doc.toObject(TransactionModel::class.java)

                // --- PROSES FILTER LOKAL ---
                // Mengapa lokal? Karena Firestore punya limitasi query composite index
                // jika memfilter beberapa field dan range secara bersamaan.

                // 1. Filter Tanggal
                val txDate = data.waktu_transaksi?.toDate()
                if (startDate != null && endDate != null && txDate != null) {
                    if (txDate.before(startDate) || txDate.after(endDate)) continue
                }

                // 2. Filter Rider
                if (selectedRiderId != "ALL" && data.id_rider != selectedRiderId) continue

                // 3. Filter Metode Pembayaran
                if (selectedMetode != "ALL" && data.metode_pembayaran != selectedMetode) continue

                transactionList.add(data)
                totalOmset += data.total_harga
            }

            adapter.notifyDataSetChanged()

            // Update Label Total Omset sesuai filter
            val localeID = Locale("in", "ID")
            val formatRupiah = NumberFormat.getCurrencyInstance(localeID)
            binding.tvTotalOmsetFilter.text = "Total Omset: ${formatRupiah.format(totalOmset)}"

        }.addOnFailureListener { e ->
            Toast.makeText(context, "Gagal memuat data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}