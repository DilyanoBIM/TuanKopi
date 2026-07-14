package com.example.tuankopi.distribusi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tuankopi.OwnerDashboardActivity
import com.example.tuankopi.User
import com.example.tuankopi.databinding.FragmentPilihRiderBinding
import com.example.tuankopi.databinding.ItemTanggalGudangBinding
import com.google.firebase.firestore.FirebaseFirestore

class PilihRiderFragment : Fragment() {

    private var _binding: FragmentPilihRiderBinding? = null
    private val binding get() = _binding!!
    private lateinit var mFirestore: FirebaseFirestore
    private var listRider = ArrayList<User>()
    private lateinit var mAdapter: RiderAdapter
    private var tanggalTarget = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPilihRiderBinding.inflate(inflater, container, false)
        tanggalTarget = arguments?.getString("TARGET_TANGGAL") ?: ""
        mFirestore = FirebaseFirestore.getInstance()

        setupRecyclerView()
        muatRiderAktif()

        return binding.root
    }

    private fun setupRecyclerView() {
        mAdapter = RiderAdapter(listRider) { rider ->
            val fragment = DetailDistribusiRiderFragment().apply {
                arguments = Bundle().apply {
                    putString("TARGET_TANGGAL", tanggalTarget)
                    putString("RIDER_UID", rider.uid)
                    putString("RIDER_NAMA", rider.nama)
                }
            }
            (requireActivity() as OwnerDashboardActivity).bukaHalaman(fragment, "Muatan: ${rider.nama}")
        }
        binding.rvPilihRider.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPilihRider.adapter = mAdapter
    }

    private fun muatRiderAktif() {
        mFirestore.collection("users")
            .whereEqualTo("role", "rider")
            .whereEqualTo("status_akun", true)
            .get()
            .addOnSuccessListener { snapshots ->
                if (snapshots != null) {
                    listRider.clear()
                    for (doc in snapshots.documents) {
                        val u = doc.toObject(User::class.java)
                        if (u != null) listRider.add(u)
                    }
                    mAdapter.notifyDataSetChanged()
                }
            }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    inner class RiderAdapter(private val data: List<User>, val click: (User) -> Unit) : RecyclerView.Adapter<RiderAdapter.ViewHolder>() {
        inner class ViewHolder(val b: ItemTanggalGudangBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(p: ViewGroup, vt: Int): ViewHolder =
            ViewHolder(ItemTanggalGudangBinding.inflate(LayoutInflater.from(p.context), p, false))

        override fun onBindViewHolder(vh: ViewHolder, pos: Int) {
            // Menampilkan nama rider, bukan tanggal
            vh.b.tvItemTanggal.text = data[pos].nama
            vh.b.root.setOnClickListener { click(data[pos]) }
        }

        override fun getItemCount(): Int = data.size
    }
}