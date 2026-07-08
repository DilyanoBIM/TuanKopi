package com.example.tuankopi.AktivitasRider

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.tuankopi.RiderDashboardActivity
import com.example.tuankopi.databinding.FragmentRiderAktivitasBinding

class RiderAktivitasFragment : Fragment() {

    private var _binding: FragmentRiderAktivitasBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRiderAktivitasBinding.inflate(inflater, container, false)

        binding.cardRiwayatTransaksi.setOnClickListener {
            val fragmentTgl = AktivitasPilihTanggalFragment.newInstance("RIWAYAT")
            (activity as? RiderDashboardActivity)?.gantiRiderFragment(fragmentTgl)
        }

        binding.cardClosingHarian.setOnClickListener {
            val fragmentTgl = AktivitasPilihTanggalFragment.newInstance("CLOSING")
            (activity as? RiderDashboardActivity)?.gantiRiderFragment(fragmentTgl)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}