package com.example.tuankopi.AktivitasRider

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tuankopi.RiderDashboardActivity
import com.example.tuankopi.databinding.FragmentRiwayatTransaksiBinding
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RiwayatTransaksiFragment : Fragment() {

    private var _binding: FragmentRiwayatTransaksiBinding? = null
    private val binding get() = _binding!!

    private lateinit var mFirestore: FirebaseFirestore
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mAdapter: TransactionAdapter

    private var rawTransactionList = ArrayList<Map<String, Any>>()
    private var filteredList = ArrayList<Map<String, Any>>()
    private var tanggalTarget = ""
    private var currentFilter = "Semua"

    companion object {
        fun newInstance(tanggal: String): RiwayatTransaksiFragment {
            val fragment = RiwayatTransaksiFragment()
            val args = Bundle().apply { putString("TANGGAL", tanggal) }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRiwayatTransaksiBinding.inflate(inflater, container, false)
        mFirestore = FirebaseFirestore.getInstance()
        mAuth = FirebaseAuth.getInstance()

        tanggalTarget = arguments?.getString("TANGGAL") ?: ""

        // Format tanggal sebelum ditampilkan di Header UI
        val tanggalIndo = formatKeTanggalIndo(tanggalTarget)
        binding.tvInfoTanggal.text = "Transaksi: $tanggalIndo"

        binding.btnBackDetail.setOnClickListener {
            val tglFragment = AktivitasPilihTanggalFragment.newInstance("RIWAYAT")
            (activity as? RiderDashboardActivity)?.gantiRiderFragment(tglFragment)
        }

        setupRecyclerView()
        setupFilters()
        fetchRiwayatBerdasarkanTanggal() // Tetap menggunakan YYYY-MM-DD

        return binding.root
    }

    private fun setupRecyclerView() {
        mAdapter = TransactionAdapter(filteredList) { transactionData ->
            tampilkanDetailTransaksi(transactionData)
        }
        binding.rvRiwayatTransaksi.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = mAdapter
        }
    }

    private fun setupFilters() {
        binding.cgFilterTransaksi.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val chip = group.findViewById<Chip>(checkedIds[0])
            currentFilter = chip.text.toString()
            terapkanFilter()
        }
    }

    private fun fetchRiwayatBerdasarkanTanggal() {
        val uid = mAuth.currentUser?.uid ?: return
        if (tanggalTarget.isEmpty()) return

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateTarget = sdf.parse(tanggalTarget) ?: return

        val cal = Calendar.getInstance().apply { time = dateTarget }
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
        val startOfDay = cal.time

        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
        val endOfDay = cal.time

        mFirestore.collection("transactions")
            .whereEqualTo("id_rider", uid)
            .whereGreaterThanOrEqualTo("waktu_transaksi", startOfDay)
            .whereLessThanOrEqualTo("waktu_transaksi", endOfDay)
            .orderBy("waktu_transaksi", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || !isAdded) return@addSnapshotListener

                rawTransactionList.clear()
                snapshot?.documents?.forEach { doc ->
                    val data = doc.data
                    if (data != null) rawTransactionList.add(data)
                }
                terapkanFilter()
            }
    }

    private fun terapkanFilter() {
        filteredList.clear()
        var totalOmset = 0L

        if (currentFilter == "Semua") {
            filteredList.addAll(rawTransactionList)
            totalOmset = rawTransactionList.filter { it["status_pembayaran"].toString().uppercase() == "SUCCESS" }
                .sumOf { it["total_harga"] as? Long ?: 0L }
        } else {
            val filterKata = currentFilter.uppercase(Locale.getDefault())
            filteredList.addAll(rawTransactionList.filter {
                val metode = it["metode_pembayaran"].toString().uppercase()
                val status = it["status_pembayaran"].toString().uppercase()
                metode == filterKata || status == filterKata
            })

            totalOmset = filteredList.filter { it["status_pembayaran"].toString().uppercase() == "SUCCESS" }
                .sumOf { it["total_harga"] as? Long ?: 0L }
        }

        mAdapter.notifyDataSetChanged()

        val fmtRp = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        binding.tvRingkasanTotal.text = fmtRp.format(totalOmset).replace(",00", "")
        binding.tvRingkasanQty.text = "${filteredList.size} Transaksi"

        if (filteredList.isEmpty()) {
            binding.tvEmptyStateRiwayat.visibility = View.VISIBLE
            binding.rvRiwayatTransaksi.visibility = View.GONE
        } else {
            binding.tvEmptyStateRiwayat.visibility = View.GONE
            binding.rvRiwayatTransaksi.visibility = View.VISIBLE
        }
    }

    private fun tampilkanDetailTransaksi(data: Map<String, Any>) {
        val orderId = data["order_id"].toString()
        val metode = data["metode_pembayaran"].toString()
        val status = data["status_pembayaran"].toString()
        val total = data["total_harga"] as? Long ?: 0L

        val items = data["items"] as? List<*> ?: emptyList<Any>()
        val fmtRp = NumberFormat.getCurrencyInstance(Locale("in", "ID"))

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        items.forEach { rawItem ->
            val item = rawItem as? Map<*, *> ?: return@forEach
            val nama = item["nama_produk"].toString()
            val qty = (item["qty"] as? Long) ?: 0L
            val harga = (item["harga_satuan"] as? Long) ?: 0L

            val itemText = TextView(context).apply {
                text = "- $nama (x$qty) : ${fmtRp.format(harga * qty).replace(",00", "")}"
                textSize = 14f
                setTextColor(Color.DKGRAY)
                setPadding(0, 8, 0, 8)
            }
            container.addView(itemText)
        }

        val totalText = TextView(context).apply {
            text = "\nTotal Bayar: ${fmtRp.format(total).replace(",00", "")}\nMetode: $metode\nStatus: $status"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            setPadding(0, 16, 0, 0)
        }
        container.addView(totalText)

        AlertDialog.Builder(requireContext())
            .setTitle("Detail: $orderId")
            .setView(container)
            .setPositiveButton("Tutup") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    // Fungsi format tanggal ditambahkan ke sini
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