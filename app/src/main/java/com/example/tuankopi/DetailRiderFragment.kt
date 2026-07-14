package com.example.tuankopi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.tuankopi.databinding.FragmentDetailRiderBinding

class DetailRiderFragment : Fragment() {

    private var _binding: FragmentDetailRiderBinding? = null
    // Pastikan casting dan referensi View berasal dari AndroidX / android.view.View
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailRiderBinding.inflate(inflater, container, false)

        val uid = arguments?.getString("RIDER_UID") ?: "-"
        val nama = arguments?.getString("RIDER_NAMA") ?: "-"
        val email = arguments?.getString("RIDER_EMAIL") ?: "-"
        val noHp = arguments?.getString("RIDER_NOHP") ?: "-"

        // ID di bawah ini sekarang akan terbaca normal setelah XML dinamai dengan benar
        binding.tvDetailNama.text = "Nama: $nama"
        binding.tvDetailEmail.text = "Email: $email"
        binding.tvDetailNoHp.text = "No HP/WA: $noHp"
        binding.tvDetailUid.text = "UID: $uid"

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}