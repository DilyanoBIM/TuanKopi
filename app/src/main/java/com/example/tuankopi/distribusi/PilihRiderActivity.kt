package com.example.tuankopi.distribusi

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tuankopi.User
import com.example.tuankopi.databinding.ActivityPilihRiderBinding
import com.example.tuankopi.databinding.ItemTanggalGudangBinding
import com.google.firebase.firestore.FirebaseFirestore

class PilihRiderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPilihRiderBinding
    private lateinit var mFirestore: FirebaseFirestore
    private var listRider = ArrayList<User>()
    private lateinit var mAdapter: RiderAdapter
    private var tanggalTarget = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPilihRiderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tanggalTarget = intent.getStringExtra("TARGET_TANGGAL") ?: ""

        setSupportActionBar(binding.customToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Rider - $tanggalTarget"

        ViewCompat.setOnApplyWindowInsetsListener(binding.customToolbar) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, insets.top, 0, 0)
            windowInsets
        }

        mFirestore = FirebaseFirestore.getInstance()

        setupRecyclerView()
        muatRiderAktif()
    }

    private fun setupRecyclerView() {
        mAdapter = RiderAdapter(listRider) { rider ->
            val intent = Intent(this, DetailDistribusiRiderActivity::class.java)
            intent.putExtra("TARGET_TANGGAL", tanggalTarget)
            intent.putExtra("RIDER_UID", rider.uid)
            intent.putExtra("RIDER_NAMA", rider.nama)
            startActivity(intent)
        }
        binding.rvPilihRider.layoutManager = LinearLayoutManager(this)
        binding.rvPilihRider.adapter = mAdapter
    }

    private fun muatRiderAktif() {
        mFirestore.collection("users")
            .whereEqualTo("role", "rider")
            .whereEqualTo("status_akun", true)
            .get()
            .addOnSuccessListener { snapshots ->
                if (snapshots != null) {
                    listRider.clear()
                    for (doc in snapshots.documents) {
                        val u = doc.toObject(User::class.java)
                        if (u != null) listRider.add(u)
                    }
                    mAdapter.notifyDataSetChanged()
                }
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    inner class RiderAdapter(private val data: List<User>, val click: (User) -> Unit) : RecyclerView.Adapter<RiderAdapter.ViewHolder>() {
        inner class ViewHolder(val b: ItemTanggalGudangBinding) : RecyclerView.ViewHolder(b.root)
        override fun onCreateViewHolder(p: ViewGroup, vt: Int): ViewHolder =
            ViewHolder(ItemTanggalGudangBinding.inflate(LayoutInflater.from(p.context), p, false))
        override fun onBindViewHolder(vh: ViewHolder, pos: Int) {
            vh.b.tvItemTanggal.text = data[pos].nama
            vh.b.root.setOnClickListener { click(data[pos]) }
        }
        override fun getItemCount(): Int = data.size
    }
}