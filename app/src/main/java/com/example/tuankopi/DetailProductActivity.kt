package com.example.tuankopi

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tuankopi.databinding.ActivityDetailProductBinding
import java.text.NumberFormat
import java.util.Locale

class DetailProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailProductBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.customToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Informasi Menu"

        ViewCompat.setOnApplyWindowInsetsListener(binding.customToolbar) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, insets.top, 0, 0)
            windowInsets
        }

        val id = intent.getStringExtra("PROD_ID") ?: "-"
        val nama = intent.getStringExtra("PROD_NAMA") ?: "-"
        val harga = intent.getLongExtra("PROD_HARGA", 0)

        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        val hargaFormat = format.format(harga).replace(",00", "")

        binding.tvDetailIdProduk.text = "ID Menu: $id"
        binding.tvDetailNamaProduk.text = "Varian: $nama"
        binding.tvDetailHargaProduk.text = "Harga Jual: $hargaFormat"
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}