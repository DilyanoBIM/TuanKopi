package com.example.tuankopi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.tuankopi.databinding.FragmentDataMasterBinding

class DataMasterFragment : Fragment() {
    private var _binding: FragmentDataMasterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDataMasterBinding.inflate(inflater, container, false)

        binding.cardManageRider.setOnClickListener {
            (requireActivity() as OwnerDashboardActivity)
                .bukaHalaman(ManageRiderFragment(), "Kelola Operasional Rider")
        }

        binding.cardManageProduct.setOnClickListener {
            // UBAH BAGIAN INI MENJADI FRAGMENT
            (requireActivity() as OwnerDashboardActivity)
                .bukaHalaman(ManageProductFragment(), "Katalog Produk Master")
        }

        return binding.root
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}