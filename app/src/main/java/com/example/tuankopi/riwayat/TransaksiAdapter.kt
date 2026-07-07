package com.example.tuankopi.riwayat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tuankopi.R
import com.example.tuankopi.databinding.ItemTransaksiBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class TransaksiAdapter : ListAdapter<Transaksi, TransaksiAdapter.TransaksiViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransaksiViewHolder {
        val binding = ItemTransaksiBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TransaksiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransaksiViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TransaksiViewHolder(private val binding: ItemTransaksiBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val rupiah = NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
            maximumFractionDigits = 0
        }
        private val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("in", "ID"))

        fun bind(t: Transaksi) {
            binding.tvOrderId.text = t.order_id
            binding.tvNamaRider.text = t.nama_rider.ifBlank { "-" }
            binding.tvWaktu.text = t.waktu_transaksi?.toDate()?.let { sdf.format(it) } ?: "-"
            binding.tvItems.text = t.ringkasanItem()
            binding.tvTotal.text = rupiah.format(t.total_harga)

            binding.chipMetode.text = t.metode_pembayaran
            val context = binding.root.context
            val warnaMetode = if (t.metode_pembayaran == "QRIS") {
                ContextCompat.getColor(context, R.color.metode_qris)
            } else {
                ContextCompat.getColor(context, R.color.metode_tunai)
            }
            binding.chipMetode.chipBackgroundColor =
                android.content.res.ColorStateList.valueOf(warnaMetode)

            binding.tvStatus.text = t.status_pembayaran
            val warnaStatus = when (t.status_pembayaran) {
                "SUCCESS" -> ContextCompat.getColor(context, R.color.status_success)
                "FAILED" -> ContextCompat.getColor(context, R.color.status_failed)
                else -> ContextCompat.getColor(context, R.color.status_pending)
            }
            binding.tvStatus.setTextColor(warnaStatus)
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Transaksi>() {
            override fun areItemsTheSame(oldItem: Transaksi, newItem: Transaksi) =
                oldItem.order_id == newItem.order_id

            override fun areContentsTheSame(oldItem: Transaksi, newItem: Transaksi) =
                oldItem == newItem
        }
    }
}