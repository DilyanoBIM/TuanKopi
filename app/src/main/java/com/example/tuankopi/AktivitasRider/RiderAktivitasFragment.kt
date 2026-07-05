package com.example.tuankopi.AktivitasRider

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.tuankopi.databinding.FragmentRiderAktivitasBinding
import com.google.android.material.tabs.TabLayoutMediator

class RiderAktivitasFragment : Fragment() {
    private var _binding: FragmentRiderAktivitasBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRiderAktivitasBinding.inflate(inflater, container, false)

        val adapter = AktivitasPagerAdapter(this)
        binding.viewPagerAktivitas.adapter = adapter

        TabLayoutMediator(binding.tabLayoutAktivitas, binding.viewPagerAktivitas) { tab, position ->
            tab.text = when (position) {
                0 -> "Riwayat Transaksi"
                1 -> "Closing Harian"
                else -> ""
            }
        }.attach()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}