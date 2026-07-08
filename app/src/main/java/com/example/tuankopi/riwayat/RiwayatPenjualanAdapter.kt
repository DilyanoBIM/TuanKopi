package com.example.tuankopi.riwayat

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tuankopi.R
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class RiwayatPenjualanAdapter(private val list: List<TransactionModel>) :
    RecyclerView.Adapter<RiwayatPenjualanAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvOrderId: TextView = view.findViewById(android.R.id.text1)
        val tvTanggal: TextView = view.findViewById(android.R.id.text2)
        val tvDetailItems: TextView = view.findViewById(R.id.tvDetailItems)
        val tvMetode: TextView = view.findViewById(R.id.tvMetode)
        val tvTotal: TextView = view.findViewById(R.id.tvTotal)
        val tvNamaRider: TextView = view.findViewById(R.id.tvNamaRider)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_riwayat_penjualan, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = list[position]

        holder.tvOrderId.text = data.order_id
        holder.tvNamaRider.text = "Rider: ${data.nama_rider}"

        data.waktu_transaksi?.toDate()?.let { date ->
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
            holder.tvTanggal.text = sdf.format(date)
        }

        val formatRupiah = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        holder.tvTotal.text = formatRupiah.format(data.total_harga)

        holder.tvMetode.text = data.metode_pembayaran
        if (data.metode_pembayaran == "QRIS") {
            holder.tvMetode.setTextColor(Color.parseColor("#1565C0"))
        } else {
            holder.tvMetode.setTextColor(Color.parseColor("#2E7D32"))
        }

        val itemDetails = StringBuilder()
        for (item in data.items) {
            itemDetails.append("- ${item.qty}x ${item.nama_produk}\n")
        }
        holder.tvDetailItems.text = itemDetails.toString().trim()
    }

    override fun getItemCount() = list.size
}