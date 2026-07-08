package com.example.tuankopi

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.tuankopi.databinding.FragmentLogistikBinding
import com.example.tuankopi.distribusi.ManageDistribusiActivity

class LogistikFragment : Fragment() {
    private var _binding: FragmentLogistikBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLogistikBinding.inflate(inflater, container, false)

        binding.cardStokGudang.setOnClickListener {
            val intent = Intent(requireActivity(), ManageStokGudangActivity::class.java)
            startActivity(intent)
        }

        binding.cardDistribusiPagi.setOnClickListener {
            val intent = Intent(requireActivity(), ManageDistribusiActivity::class.java)
            startActivity(intent)
        }

        return binding.root
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}