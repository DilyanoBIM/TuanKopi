package com.example.tuankopi.ownervalidasi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentValidasiListBinding.inflate(inflater, container, false)
        mFirestore = FirebaseFirestore.getInstance()

        binding.rvValidasi.layoutManager = LinearLayoutManager(requireContext())
        loadDataPending()

        return binding.root
    }

    private fun loadDataPending() {
        mFirestore.collection("closing_laporan")
            .whereEqualTo("status_validasi", "PENDING")
            .orderBy("waktu_closing_rider", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || !isAdded) return@addSnapshotListener

                val listLaporan = snapshot?.documents?.mapNotNull { it.toObject(ClosingLaporan::class.java) } ?: emptyList()

                binding.rvValidasi.adapter = ValidasiAdapter(listLaporan) { closingData ->
                    bukaDetailValidasi(closingData.id_closing)
                }
            }
    }

    private fun bukaDetailValidasi(idClosing: String) {
        val detailFragment = ValidasiDetailFragment.newInstance(idClosing)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, detailFragment) // Sesuaikan dengan ID container Anda
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Inner Adapter untuk Efisiensi
class ValidasiAdapter(
    private val list: List<ClosingLaporan>,
    private val onClick: (ClosingLaporan) -> Unit
) : RecyclerView.Adapter<ValidasiAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemValidasiSetoranBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemValidasiSetoranBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.binding.tvTanggalClosing.text = "Tanggal: ${item.tanggal}"
        holder.binding.tvNamaRider.text = "Rider: ${item.nama_rider}"
        holder.binding.root.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = list.size
}