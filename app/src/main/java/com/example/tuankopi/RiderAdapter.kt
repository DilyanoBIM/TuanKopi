package com.example.tuankopi

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tuankopi.databinding.ItemRiderBinding

class RiderAdapter(
    private var listRider: List<Rider>,
    private val onStatusChanged: (String, Boolean) -> Unit,
    private val onItemClick: (Rider) -> Unit
) : RecyclerView.Adapter<RiderAdapter.RiderViewHolder>() {

    inner class RiderViewHolder(val binding: ItemRiderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RiderViewHolder {
        val binding = ItemRiderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RiderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RiderViewHolder, position: Int) {
        val rider = listRider[position]
        with(holder.binding) {
            tvNamaItem.text = rider.nama
            tvEmailItem.text = rider.email

            // Atur posisi switch tanpa memicu listener saat pertama kali dimuat
            switchStatus.setOnCheckedChangeListener(null)
            switchStatus.isChecked = rider.status_akun

            // Listener ketika Switch diklik oleh Owner
            switchStatus.setOnCheckedChangeListener { _, isChecked ->
                onStatusChanged(rider.uid, isChecked)
            }

            // Listener ketika seluruh baris item diklik (Detail)
            root.setOnClickListener {
                onItemClick(rider)
            }
        }
    }

    override fun getItemCount(): Int = listRider.size

    fun updateData(newList: List<Rider>) {
        this.listRider = newList
        notifyDataSetChanged()
    }
}