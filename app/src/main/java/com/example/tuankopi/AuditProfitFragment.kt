package com.example.tuankopi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.tuankopi.databinding.FragmentAuditProfitBinding
// Tambahkan baris import ini
import com.example.tuankopi.ownervalidasi.ValidasiListFragment

class AuditProfitFragment : Fragment() {
    private var _binding: FragmentAuditProfitBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAuditProfitBinding.inflate(inflater, container, false)

        binding.cardLiveMonitor.setOnClickListener {
            Toast.makeText(requireContext(), "Membuka Pantauan Sisa Stok Rider...", Toast.LENGTH_SHORT).show()
        }

        binding.cardValidasiSetoran.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ValidasiListFragment()) // Sesuaikan R.id.fragment_container dengan ID FrameLayout utama Anda
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