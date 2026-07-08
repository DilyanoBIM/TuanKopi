package com.example.tuankopi

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tuankopi.databinding.FragmentRiderStokAktifBinding
import com.example.tuankopi.databinding.ItemRiwayatStokTanggalBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.NumberFormat
import java.util.Locale

data class ItemTanggalDokumen(
    val docId: String,
    val tanggal: String,
    val statusStok: String,
    val modalKembalian: Long
)

data class SubDetailKopi(
    val namaProduk: String,
    val stokAwal: Long,
    val terjual: Long,
    val sisaStok: Long
)

class RiderStokAktifFragment : Fragment() {

    private var _binding: FragmentRiderStokAktifBinding? = null
    private val binding get() = _binding!!

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mFirestore: FirebaseFirestore
    private val listTanggal = ArrayList<ItemTanggalDokumen>()
    private lateinit var mAdapter: TanggalAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRiderStokAktifBinding.inflate(inflater, container, false)
        mAuth = FirebaseAuth.getInstance()
        mFirestore = FirebaseFirestore.getInstance()

        binding.btnBack.setOnClickListener {
            (activity as? RiderDashboardActivity)?.gantiRiderFragment(RiderStokFragment())
        }

        setupRecyclerView()
        muatDaftarTanggalAktifRider()

        return binding.root
    }

    private fun setupRecyclerView() {
        mAdapter = TanggalAdapter(listTanggal) { item ->
            val fragmentDetail = RiderStokDetailFragment.newInstance(item.docId, item.tanggal, item.modalKembalian)
            (activity as? RiderDashboardActivity)?.gantiRiderFragment(fragmentDetail)
        }
        binding.rvRiwayatStokTanggal.layoutManager = LinearLayoutManager(context)
        binding.rvRiwayatStokTanggal.adapter = mAdapter
    }

    private fun muatDaftarTanggalAktifRider() {
        val currentUid = mAuth.currentUser?.uid ?: ""
        if (currentUid.isEmpty()) return

        mFirestore.collection("stok_harian")
            .whereEqualTo("id_rider", currentUid)
            .whereEqualTo("status_stok", "AKTIF")
            .orderBy("tanggal", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (!isAdded) return@addSnapshotListener

                if (error != null) {
                    binding.tvStokAktifKosong.text = "Gagal memuat: ${error.localizedMessage}"
                    binding.tvStokAktifKosong.visibility = View.VISIBLE
                    binding.rvRiwayatStokTanggal.visibility = View.GONE
                    return@addSnapshotListener
                }

                if (snapshots == null || snapshots.isEmpty) {
                    binding.tvStokAktifKosong.text = "Tidak ada jatah stok gerobak berstatus AKTIF."
                    binding.tvStokAktifKosong.visibility = View.VISIBLE
                    binding.rvRiwayatStokTanggal.visibility = View.GONE
                    return@addSnapshotListener
                }

                listTanggal.clear()
                for (doc in snapshots.documents) {
                    var memilikiKopiDiterima = false
                    val detailMap = doc.get("detail_stok") as? Map<*, *>
                    if (detailMap != null) {
                        for ((_, value) in detailMap) {
                            val subMap = value as? Map<*, *> ?: continue
                            if (subMap["diterima"] as? Boolean == true) {
                                memilikiKopiDiterima = true
                                break
                            }
                        }
                    }

                    if (memilikiKopiDiterima) {
                        listTanggal.add(
                            ItemTanggalDokumen(
                                docId = doc.id,
                                tanggal = doc.getString("tanggal") ?: "Unknown",
                                statusStok = doc.getString("status_stok") ?: "AKTIF",
                                modalKembalian = doc.getLong("modal_kembalian") ?: 0L
                            )
                        )
                    }
                }

                if (listTanggal.isEmpty()) {
                    binding.tvStokAktifKosong.text = "Belum ada tanggal jualan yang Anda terima jatahnya."
                    binding.tvStokAktifKosong.visibility = View.VISIBLE
                    binding.rvRiwayatStokTanggal.visibility = View.GONE
                } else {
                    binding.tvStokAktifKosong.visibility = View.GONE
                    binding.rvRiwayatStokTanggal.visibility = View.VISIBLE
                    mAdapter.notifyDataSetChanged()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class TanggalAdapter(
        private val data: List<ItemTanggalDokumen>,
        val klikListener: (ItemTanggalDokumen) -> Unit
    ) : RecyclerView.Adapter<TanggalAdapter.ViewHolder>() {

        inner class ViewHolder(val b: ItemRiwayatStokTanggalBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(ItemRiwayatStokTanggalBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = data[position]
            holder.b.tvRiwayatTglDokumen.text = item.tanggal
            holder.b.tvRiwayatStatusStok.text = item.statusStok

            holder.b.tvRiwayatStatusStok.setTextColor(Color.parseColor("#2E7D32"))
            holder.b.tvRiwayatStatusStok.setBackgroundColor(Color.parseColor("#E8F5E9"))

            val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            holder.b.tvRiwayatModalBawaan.text = "Modal Kembalian: " + formatter.format(item.modalKembalian).replace(",00", "")

            holder.itemView.setOnClickListener { klikListener(item) }
        }

        override fun getItemCount(): Int = data.size
    }
}