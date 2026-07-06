package com.example.tuankopi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.tuankopi.databinding.FragmentRiderStokBinding

class RiderStokFragment : Fragment() {

    private var _binding: FragmentRiderStokBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRiderStokBinding.inflate(inflater, container, false)

        binding.cardKonfirmasiStokPagi.setOnClickListener {
            (activity as? RiderDashboardActivity)?.gantiRiderFragment(RiderKonfirmasiStokFragment())
        }

        binding.cardSisaStokAktif.setOnClickListener {
            (activity as? RiderDashboardActivity)?.gantiRiderFragment(RiderStokAktifFragment())
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}