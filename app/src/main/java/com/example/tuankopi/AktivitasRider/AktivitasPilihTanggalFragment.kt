package com.example.tuankopi.AktivitasRider

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tuankopi.R
import com.example.tuankopi.RiderDashboardActivity
import com.example.tuankopi.databinding.FragmentAktivitasPilihTanggalBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AktivitasPilihTanggalFragment : Fragment() {

    private var _binding: FragmentAktivitasPilihTanggalBinding? = null
    private val binding get() = _binding!!

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mFirestore: FirebaseFirestore
    private val listTanggal = ArrayList<String>()
    private lateinit var mAdapter: TanggalAdapter

    private var modeTujuan = ""

    companion object {
        fun newInstance(mode: String): AktivitasPilihTanggalFragment {
            val fragment = AktivitasPilihTanggalFragment()
            val args = Bundle().apply { putString("MODE", mode) }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAktivitasPilihTanggalBinding.inflate(inflater, container, false)
        mAuth = FirebaseAuth.getInstance()
        mFirestore = FirebaseFirestore.getInstance()

        modeTujuan = arguments?.getString("MODE") ?: "RIWAYAT"

        // Sesuaikan Header
        val activity = activity as? RiderDashboardActivity
        activity?.setSupportActionBar(binding.customToolbar)
        activity?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.customToolbar.setNavigationOnClickListener {
            activity?.gantiRiderFragment(RiderAktivitasFragment())
        }

        binding.tvDetailHeader.text = if (modeTujuan == "RIWAYAT") "Pilih Tanggal Transaksi" else "Pilih Tanggal Closing"

        setupRecyclerView()
        muatTanggalBerdasarkanFilterMode()

        return binding.root
    }

    private fun setupRecyclerView() {
        mAdapter = TanggalAdapter(listTanggal) { tglTerpilih ->
            if (modeTujuan == "RIWAYAT") {
                val fragment = RiwayatTransaksiFragment.newInstance(tglTerpilih)
                (activity as? RiderDashboardActivity)?.gantiRiderFragment(fragment)
            } else {
                val fragment = ClosingHarianFragment.newInstance(tglTerpilih)
                (activity as? RiderDashboardActivity)?.gantiRiderFragment(fragment)
            }
        }
        binding.rvTanggalAktivitas.layoutManager = LinearLayoutManager(context)
        binding.rvTanggalAktivitas.adapter = mAdapter
    }

    private fun muatTanggalBerdasarkanFilterMode() {
        val uidRider = mAuth.currentUser?.uid ?: return

        var query = mFirestore.collection("stok_harian")
            .whereEqualTo("id_rider", uidRider)

        // Jika mode CLOSING, pastikan Rider sudah menyelesaikan jualan di jalan.
        // Kita JANGAN filter status_stok = AKTIF, agar riwayat yang sudah CLOSED di masa lalu tetap bisa dilihat.
        if (modeTujuan == "CLOSING") {
            query = query.whereEqualTo("status_jualan", "SELESAI JUALAN")
        }

        query.orderBy("tanggal", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshots ->
                if (snapshots != null && isAdded) {
                    listTanggal.clear()
                    for (doc in snapshots.documents) {
                        val tgl = doc.getString("tanggal")
                        if (tgl != null) listTanggal.add(tgl)
                    }
                    mAdapter.notifyDataSetChanged()

                    if (listTanggal.isEmpty()) {
                        val pesan = if (modeTujuan == "RIWAYAT") "Belum ada riwayat operasional." else "Belum ada riwayat closing atau belum selesai jualan hari ini."
                        Toast.makeText(context, pesan, Toast.LENGTH_LONG).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Gagal memuat: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class TanggalAdapter(private val data: List<String>, val click: (String) -> Unit) : RecyclerView.Adapter<TanggalAdapter.ViewHolder>() {
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