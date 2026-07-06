package com.example.tuankopi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tuankopi.databinding.FragmentRiderKonfirmasiStokBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RiderKonfirmasiStokFragment : Fragment() {

    private var _binding: FragmentRiderKonfirmasiStokBinding? = null
    private val binding get() = _binding!!

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mFirestore: FirebaseFirestore
    private var listTanggal = ArrayList<String>()
    private lateinit var mAdapter: TanggalRiderAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRiderKonfirmasiStokBinding.inflate(inflater, container, false)
        mAuth = FirebaseAuth.getInstance()
        mFirestore = FirebaseFirestore.getInstance()

        // Fungsi Tombol Kembali menuju Beranda Rider
        binding.btnBack.setOnClickListener {
            (activity as? RiderDashboardActivity)?.gantiRiderFragment(RiderDashboardFragment())
        }

        setupRecyclerView()
        muatSemuaTanggalAlokasiRider()

        return binding.root
    }

    private fun setupRecyclerView() {
        mAdapter = TanggalRiderAdapter(listTanggal) { tglTerpilih ->
            val fragmentDetail = RiderDetailKonfirmasiFragment().apply {
                arguments = Bundle().apply {
                    putString("KEY_TANGGAL", tglTerpilih)
                }
            }
            (activity as? RiderDashboardActivity)?.gantiRiderFragment(fragmentDetail)
        }
        binding.rvTanggalStokRider.layoutManager = LinearLayoutManager(context)
        binding.rvTanggalStokRider.adapter = mAdapter
    }

    private fun muatSemuaTanggalAlokasiRider() {
        val uidRider = mAuth.currentUser?.uid ?: ""
        if (uidRider.isEmpty()) return

        mFirestore.collection("stok_harian")
            .whereEqualTo("id_rider", uidRider)
            .get()
            .addOnSuccessListener { snapshots ->
                if (snapshots != null) {
                    listTanggal.clear()
                    for (doc in snapshots.documents) {
                        val tgl = doc.getString("tanggal")
                        if (tgl != null) listTanggal.add(tgl)
                    }
                    listTanggal.sortDescending()
                    mAdapter.notifyDataSetChanged()

                    if (listTanggal.isEmpty()) {
                        Toast.makeText(context, "Belum ada jatah distribusi dari Owner.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class TanggalRiderAdapter(
        private val data: List<String>,
        val click: (String) -> Unit
    ) : RecyclerView.Adapter<TanggalRiderAdapter.ViewHolder>() {

        inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
            val textTgl: TextView = view.findViewById(R.id.tvItemTanggal)
        }

        override fun onCreateViewHolder(p: ViewGroup, vt: Int): ViewHolder {
            val v = LayoutInflater.from(p.context).inflate(R.layout.item_tanggal_gudang, p, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(vh: ViewHolder, pos: Int) {
            vh.textTgl.text = data[pos]
            vh.view.setOnClickListener { click(data[pos]) }
        }

        override fun getItemCount(): Int = data.size
    }
}