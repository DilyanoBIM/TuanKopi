package com.example.tuankopi.AktivitasRider

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class AktivitasPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> RiwayatTransaksiFragment()
            1 -> ClosingHarianFragment()
            else -> RiwayatTransaksiFragment()
        }
    }
}