package com.example.tuankopi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.tuankopi.databinding.FragmentAuditProfitBinding

class AuditProfitFragment : Fragment() {
    private var _binding: FragmentAuditProfitBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAuditProfitBinding.inflate(inflater, container, false)

        binding.cardLiveMonitor.setOnClickListener {
            Toast.makeText(requireContext(), "Membuka Pantauan Sisa Stok Rider...", Toast.LENGTH_SHORT).show()
        }

        binding.cardValidasiSetoran.setOnClickListener {
            Toast.makeText(requireContext(), "Membuka Audit Setoran Keuangan...", Toast.LENGTH_SHORT).show()
        }

        return binding.root
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}