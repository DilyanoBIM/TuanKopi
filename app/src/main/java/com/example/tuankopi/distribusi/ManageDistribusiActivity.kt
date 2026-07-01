package com.example.tuankopi.distribusi

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tuankopi.databinding.ActivityManageStokGudangBinding
import com.example.tuankopi.databinding.ItemTanggalGudangBinding
import com.google.firebase.firestore.FirebaseFirestore

class ManageDistribusiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageStokGudangBinding
    private lateinit var mFirestore: FirebaseFirestore
    private var listTanggal = ArrayList<String>()
    private lateinit var mAdapter: TanggalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageStokGudangBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Pilih Tanggal Distribusi"

        // Sembunyikan FAB tambah tanggal karena distribusi hanya mengikuti tanggal gudang yang sudah ada
        binding.fabAddStokGudang.hide()

        mFirestore = FirebaseFirestore.getInstance()
        setupRecyclerView()
        muatDaftarTanggalGudang()
    }

    private fun setupRecyclerView() {
        mAdapter = TanggalAdapter(listTanggal) { tgl ->
            val intent = Intent(this, PilihRiderActivity::class.java)
            intent.putExtra("TARGET_TANGGAL", tgl)
            startActivity(intent)
        }
        binding.rvTanggalGudang.layoutManager = LinearLayoutManager(this)
        binding.rvTanggalGudang.adapter = mAdapter
    }

    private fun muatDaftarTanggalGudang() {
        mFirestore.collection("stok_gudang")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val setTanggal = HashSet<String>()
                    for (doc in snapshot.documents) {
                        val tgl = doc.getString("tanggal")
                        if (tgl != null) setTanggal.add(tgl)
                    }
                    listTanggal.clear()
                    listTanggal.addAll(setTanggal.sortedDescending())
                    mAdapter.notifyDataSetChanged()
                }
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    inner class TanggalAdapter(private val data: List<String>, val click: (String) -> Unit) : RecyclerView.Adapter<TanggalAdapter.ViewHolder>() {
        inner class ViewHolder(val b: ItemTanggalGudangBinding) : RecyclerView.ViewHolder(b.root)
        override fun onCreateViewHolder(p: ViewGroup, vt: Int): ViewHolder =
            ViewHolder(ItemTanggalGudangBinding.inflate(LayoutInflater.from(p.context), p, false))
        override fun onBindViewHolder(vh: ViewHolder, pos: Int) {
            vh.b.tvItemTanggal.text = data[pos]
            vh.b.root.setOnClickListener { click(data[pos]) }
        }
        override fun getItemCount(): Int = data.size
    }
}