package com.example.tuankopi.AktivitasRider

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tuankopi.databinding.ItemRiwayatTransaksiBinding
import java.text.NumberFormat
import java.util.Locale

class TransactionAdapter(
    private val transactionList: List<Map<String, Any>>,
    private val onItemClick: (Map<String, Any>) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemRiwayatTransaksiBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(transaction: Map<String, Any>) {
            val orderId = transaction["order_id"]?.toString() ?: "-"
            val metode = transaction["metode_pembayaran"]?.toString() ?: "-"
            val status = transaction["status_pembayaran"]?.toString() ?: "-"
            val total = transaction["total_harga"] as? Long ?: 0L

            val fmtRp = NumberFormat.getCurrencyInstance(Locale("in", "ID"))

            binding.tvItemOrderId.text = orderId
            binding.tvItemTotal.text = fmtRp.format(total).replace(",00", "")
            binding.tvItemMetode.text = "Metode: $metode"
            binding.tvItemStatus.text = status

            when (status.uppercase()) {
                "SUCCESS" -> binding.tvItemStatus.setTextColor(Color.parseColor("#2E7D32"))
                "FAILED" -> binding.tvItemStatus.setTextColor(Color.parseColor("#C62828"))
                else -> binding.tvItemStatus.setTextColor(Color.parseColor("#F57F17"))
            }

            binding.root.setOnClickListener {
                onItemClick(transaction)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRiwayatTransaksiBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(transactionList[position])
    }

    override fun getItemCount(): Int = transactionList.size
}