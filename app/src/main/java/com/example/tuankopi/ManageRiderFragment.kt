package com.example.tuankopi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tuankopi.databinding.FragmentManageRiderBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ManageRiderFragment : Fragment() {

    private var _binding: FragmentManageRiderBinding? = null
    private val binding get() = _binding!!
    private lateinit var mFirestore: FirebaseFirestore
    private lateinit var adapterRider: RiderAdapter
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageRiderBinding.inflate(inflater, container, false)
        mFirestore = FirebaseFirestore.getInstance()

        setupRecyclerView()

        binding.fabAddRider.setOnClickListener {
            (requireActivity() as OwnerDashboardActivity)
                .bukaHalaman(AddRiderFragment(), "Tambah Rider Baru")
        }

        muatDataRiderDariFirestore()
        return binding.root
    }

    private fun setupRecyclerView() {
        adapterRider = RiderAdapter(
            listRider = emptyList(),
            onStatusChanged = { uid, statusBaru -> ubahStatusAkunRider(uid, statusBaru) },
            onItemClick = { rider ->
                val fragment = DetailRiderFragment().apply {
                    arguments = Bundle().apply {
                        putString("RIDER_UID", rider.uid)
                        putString("RIDER_NAMA", rider.nama)
                        putString("RIDER_EMAIL", rider.email)
                        putString("RIDER_NOHP", rider.no_hp)
                    }
                }
                (requireActivity() as OwnerDashboardActivity)
                    .bukaHalaman(fragment, "Detail Karyawan")
            }
        )
        binding.rvRider.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRider.adapter = adapterRider
    }

    private fun muatDataRiderDariFirestore() {
        listenerRegistration = mFirestore.collection("users")
            .whereEqualTo("role", "rider")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(requireContext(), "Gagal mengambil data: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    adapterRider.updateData(snapshot.toObjects(Rider::class.java))
                }
            }
    }

    private fun ubahStatusAkunRider(uid: String, statusBaru: Boolean) {
        mFirestore.collection("users").document(uid)
            .update("status_akun", statusBaru)
            .addOnSuccessListener {
                val pesan = if (statusBaru) "Account diaktifkan" else "Account dinonaktifkan"
                Toast.makeText(requireContext(), pesan, Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Gagal memperbarui status: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        listenerRegistration?.remove()
        listenerRegistration = null
        super.onDestroyView()
        _binding = null
    }
}