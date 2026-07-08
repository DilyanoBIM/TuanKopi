package com.example.tuankopi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.tuankopi.databinding.FragmentAuditProfitBinding
import com.example.tuankopi.ownervalidasi.ValidasiListFragment
import com.example.tuankopi.riwayat.RiwayatPenjualanFragment

class AuditProfitFragment : Fragment() {
    private var _binding: FragmentAuditProfitBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAuditProfitBinding.inflate(inflater, container, false)

        binding.cardRiwayatPenjualan.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, RiwayatPenjualanFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cardValidasiSetoran.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ValidasiListFragment())
                .addToBackStack(null)
                .commit()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}