package com.example.tuankopi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.tuankopi.databinding.FragmentRiderStokBinding

class RiderStokFragment : Fragment() {

    private var _binding: FragmentRiderStokBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate layout menggunakan View Binding agar sinkron dengan XML buatan kita
        _binding = FragmentRiderStokBinding.inflate(inflater, container, false)

// Di dalam RiderStokFragment.kt pada blok onCreateView:
        binding.cardKonfirmasiStokPagi.setOnClickListener {
            // Alihkan ke Fragment Konfirmasi dengan memanfaatkan fungsi navigasi activity induk
            (activity as? RiderDashboardActivity)?.gantiRiderFragment(RiderKonfirmasiStokFragment())
        }

        binding.cardSisaStokAktif.setOnClickListener {
            // Alihkan murni ke halaman list multi-tanggal yang sudah kita lengkapi logic-nya
            (activity as? RiderDashboardActivity)?.gantiRiderFragment(RiderStokAktifFragment())
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}