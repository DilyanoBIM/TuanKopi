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
        val tvOrderId: TextView = view.findViewById(android.R.id.text1) // Sesuaikan ID Layout Anda
        val tvTanggal: TextView = view.findViewById(android.R.id.text2)
        val tvDetailItems: TextView = view.findViewById(R.id.tvDetailItems) // Buat custom TextView di item_riwayat.xml
        val tvMetode: TextView = view.findViewById(R.id.tvMetode)
        val tvTotal: TextView = view.findViewById(R.id.tvTotal)
        val tvNamaRider: TextView = view.findViewById(R.id.tvNamaRider)
    }

    // Untuk efisiensi, asumsikan Anda membuat layout custom "item_riwayat_penjualan.xml"
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Ganti R.layout.item_riwayat_penjualan dengan nama file XML item Anda
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_riwayat_penjualan, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = list[position]

        holder.tvOrderId.text = data.order_id
        holder.tvNamaRider.text = "Rider: ${data.nama_rider}"

        // Format Waktu
        data.waktu_transaksi?.toDate()?.let { date ->
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
            holder.tvTanggal.text = sdf.format(date)
        }

        // Format Uang
        val formatRupiah = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        holder.tvTotal.text = formatRupiah.format(data.total_harga)

        // Indikator Warna Pembayaran
        holder.tvMetode.text = data.metode_pembayaran
        if (data.metode_pembayaran == "QRIS") {
            holder.tvMetode.setTextColor(Color.parseColor("#1565C0")) // Biru
        } else {
            holder.tvMetode.setTextColor(Color.parseColor("#2E7D32")) // Hijau
        }

        // Gabungkan Item Produk yang Dibeli
        val itemDetails = StringBuilder()
        for (item in data.items) {
            itemDetails.append("- ${item.qty}x ${item.nama_produk}\n")
        }
        holder.tvDetailItems.text = itemDetails.toString().trim()
    }

    override fun getItemCount() = list.size
}