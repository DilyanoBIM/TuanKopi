package com.example.tuankopi

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.tuankopi.databinding.FragmentDataMasterBinding

class DataMasterFragment : Fragment() {
    private var _binding: FragmentDataMasterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDataMasterBinding.inflate(inflater, container, false)

        // Kelola Rider mengarah ke halaman kelola list rider buatan kita kemarin
        binding.cardManageRider.setOnClickListener {
            startActivity(Intent(requireActivity(), ManageRiderActivity::class.java))
        }

        binding.cardManageProduct.setOnClickListener {
            val intent = Intent(requireActivity(), ManageProductActivity::class.java)
            startActivity(intent)
        }

        return binding.root
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}