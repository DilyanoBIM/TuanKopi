package com.example.tuankopi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.tuankopi.databinding.FragmentLogistikBinding
import com.example.tuankopi.distribusi.ManageDistribusiFragment

class LogistikFragment : Fragment() {
    private var _binding: FragmentLogistikBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLogistikBinding.inflate(inflater, container, false)

        binding.cardStokGudang.setOnClickListener {
            (requireActivity() as OwnerDashboardActivity)
                .bukaHalaman(ManageStokGudangFragment(), "Stok Gudang Harian")
        }

        binding.cardDistribusiPagi.setOnClickListener {
            (requireActivity() as OwnerDashboardActivity)
                .bukaHalaman(ManageDistribusiFragment(), "Pilih Tanggal Distribusi")
        }

        return binding.root
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}