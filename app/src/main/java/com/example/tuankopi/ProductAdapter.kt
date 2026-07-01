package com.example.tuankopi

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tuankopi.databinding.ItemProductBinding
import java.text.NumberFormat
import java.util.Locale

class ProductAdapter(
    private var listProduct: List<Product>,
    private val onStatusChanged: (String, Boolean) -> Unit,
    private val onItemClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = listProduct[position]
        with(holder.binding) {
            tvNamaProdukItem.text = product.nama_produk

            // Format Rupiah
            val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            tvHargaProdukItem.text = format.format(product.harga_jual).replace(",00", "")

            switchStatusProduk.setOnCheckedChangeListener(null)
            switchStatusProduk.isChecked = product.status_tersedia

            switchStatusProduk.setOnCheckedChangeListener { _, isChecked ->
                onStatusChanged(product.id_produk, isChecked)
            }

            root.setOnClickListener {
                onItemClick(product)
            }
        }
    }

    override fun getItemCount(): Int = listProduct.size

    fun updateData(newList: List<Product>) {
        this.listProduct = newList
        notifyDataSetChanged()
    }
}