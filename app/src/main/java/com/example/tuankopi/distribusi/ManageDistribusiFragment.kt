package com.example.tuankopi.distribusi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tuankopi.OwnerDashboardActivity
import com.example.tuankopi.databinding.FragmentManageDistribusiBinding
import com.example.tuankopi.databinding.ItemTanggalGudangBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class ManageDistribusiFragment : Fragment() {

    private var _binding: FragmentManageDistribusiBinding? = null
    private val binding get() = _binding!!
    private lateinit var mFirestore: FirebaseFirestore
    private var listTanggal = ArrayList<String>()
    private lateinit var mAdapter: TanggalAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageDistribusiBinding.inflate(inflater, container, false)
        mFirestore = FirebaseFirestore.getInstance()

        setupRecyclerView()
        muatDaftarTanggalGudang()

        return binding.root
    }

    private fun setupRecyclerView() {
        mAdapter = TanggalAdapter(listTanggal) { tgl ->
            val tanggalIndo = formatKeTanggalIndo(tgl)
            val fragment = PilihRiderFragment().apply {
                arguments = Bundle().apply { putString("TARGET_TANGGAL", tgl) }
            }
            // Judul di atas juga ikut diformat
            (requireActivity() as OwnerDashboardActivity).bukaHalaman(fragment, "Rider - $tanggalIndo")
        }
        binding.rvTanggalGudang.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTanggalGudang.adapter = mAdapter
    }

    private fun muatDaftarTanggalGudang() {
        mFirestore.collection("stok_gudang")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val setTanggal = HashSet<String>()
                    for (doc in snapshot.documents) {
                        val tgl = doc.getString("tanggal")
                        if (tgl != null) setTanggal.add(tgl)
                    }
                    listTanggal.clear()
                    listTanggal.addAll(setTanggal.sortedDescending())
                    mAdapter.notifyDataSetChanged()
                }
            }
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

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    inner class TanggalAdapter(private val data: List<String>, val click: (String) -> Unit) : RecyclerView.Adapter<TanggalAdapter.ViewHolder>() {
        inner class ViewHolder(val b: ItemTanggalGudangBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(p: ViewGroup, vt: Int): ViewHolder =
            ViewHolder(ItemTanggalGudangBinding.inflate(LayoutInflater.from(p.context), p, false))

        override fun onBindViewHolder(vh: ViewHolder, pos: Int) {
            val tanggalAsli = data[pos]

            // Format teks yang ditampilkan
            vh.b.tvItemTanggal.text = formatKeTanggalIndo(tanggalAsli)
            vh.b.root.setOnClickListener { click(tanggalAsli) }
        }

        override fun getItemCount(): Int = data.size
    }
}