package com.example.tuankopi

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tuankopi.databinding.ActivityManageStokGudangBinding
import com.example.tuankopi.databinding.ItemTanggalGudangBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class ManageStokGudangActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageStokGudangBinding
    private lateinit var mFirestore: FirebaseFirestore
    private var listTanggal = ArrayList<String>()
    private lateinit var mAdapter: TanggalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageStokGudangBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Di dalam onCreate(), ganti inisialisasi Action bar lama dengan:
        setSupportActionBar(binding.customToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Stok Gudang Harian"

        mFirestore = FirebaseFirestore.getInstance()

        setupRecyclerView()

        // Klik FAB memunculkan DatePicker untuk menentukan tanggal logistik baru
        binding.fabAddStokGudang.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                val formattedMonth = String.format("%02d", month + 1)
                val formattedDay = String.format("%02d", dayOfMonth)
                val tanggalPilihan = "$year-$formattedMonth-$formattedDay"

                inisialisasiTanggalBaruDiFirestore(tanggalPilihan)

            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        muatDaftarTanggalGudang()
    }

    private fun setupRecyclerView() {
        // PERBAIKAN LOGIKA: Ketika tanggal di-klik, panggil dialog pilihan aksi (Lihat / Hapus)
        mAdapter = TanggalAdapter(listTanggal) { tgl ->
            tampilkanDialogPilihanAksi(tgl)
        }
        binding.rvTanggalGudang.layoutManager = LinearLayoutManager(this)
        binding.rvTanggalGudang.adapter = mAdapter
    }

    private fun muatDaftarTanggalGudang() {
        mFirestore.collection("stok_gudang")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val setTanggal = HashSet<String>()
                    for (doc in snapshot.documents) {
                        val tgl = doc.getString("tanggal")
                        if (tgl != null) setTanggal.add(tgl)
                    }
                    listTanggal.clear()
                    listTanggal.addAll(setTanggal.sortedDescending()) // Urutkan tanggal terbaru di atas
                    mAdapter.notifyDataSetChanged()
                }
            }
    }

    // ────────────────────────────────────────────────────────────────────────
    // FITUR BARU: Dialog Pilihan Aksi (Lihat Detail vs Hapus Total)
    // ────────────────────────────────────────────────────────────────────────
    private fun tampilkanDialogPilihanAksi(tanggalTerpilih: String) {
        val opsi = arrayOf("Lihat Detail Logistik", "Hapus Total Riwayat")

        AlertDialog.Builder(this)
            .setTitle("Logistik Tanggal: $tanggalTerpilih")
            .setItems(opsi) { dialog, which ->
                when (opsi[which]) {
                    "Lihat Detail Logistik" -> {
                        // Alirkan intent menuju halaman rincian monitor gudang
                        val intent = Intent(this, DetailStokGudangActivity::class.java)
                        intent.putExtra("LIHAT_TANGGAL", tanggalTerpilih)
                        startActivity(intent)
                    }
                    "Hapus Total Riwayat" -> {
                        // Konfirmasi ulang demi keamanan data inventory
                        tampilkanDialogKonfirmasiHapus(tanggalTerpilih)
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .create().show()
    }

    // ────────────────────────────────────────────────────────────────────────
    // FITUR BARU: Dialog Konfirmasi Penghapusan Permanen Dokumen dari Firestore
    // ────────────────────────────────────────────────────────────────────────
    private fun tampilkanDialogKonfirmasiHapus(tanggalTarget: String) {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Hapus Total")
            .setMessage("Apakah Anda yakin ingin menghapus seluruh riwayat produksi & logistik pada tanggal $tanggalTarget secara permanen?")
            .setPositiveButton("Ya, Hapus Permanen") { dialog, _ ->
                val cleanedTanggalId = tanggalTarget.replace("-", "")

                // Eksekusi penghapusan dokumen tunggal berbasis Document ID YYYYMMDD
                mFirestore.collection("stok_gudang").document(cleanedTanggalId)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Riwayat tanggal $tanggalTarget berhasil dihapus total!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Gagal menghapus: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .create().show()
    }

    private fun inisialisasiTanggalBaruDiFirestore(tanggalTarget: String) {
        val cleanedTanggalId = tanggalTarget.replace("-", "")

        if (listTanggal.contains(tanggalTarget)) {
            val intent = Intent(this, DetailStokGudangActivity::class.java)
            intent.putExtra("LIHAT_TANGGAL", tanggalTarget)
            startActivity(intent)
            return
        }

        val refDocGudang = mFirestore.collection("stok_gudang").document(cleanedTanggalId)
        val detailGudangMap = HashMap<String, Any>()

        val dataGudangBaru = hashMapOf(
            "id_gudang" to "GUD_$cleanedTanggalId",
            "tanggal" to tanggalTarget,
            "last_updated" to Timestamp.now(),
            "detail_gudang" to detailGudangMap
        )

        refDocGudang.set(dataGudangBaru)
            .addOnSuccessListener {
                Toast.makeText(this, "Sukses membuat slot tanggal $tanggalTarget!", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, DetailStokGudangActivity::class.java)
                intent.putExtra("LIHAT_TANGGAL", tanggalTarget)
                startActivity(intent)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    inner class TanggalAdapter(
        private val data: List<String>,
        private val clickListener: (String) -> Unit
    ) : RecyclerView.Adapter<TanggalAdapter.ViewHolder>() {
        inner class ViewHolder(val b: ItemTanggalGudangBinding) : RecyclerView.ViewHolder(b.root)
        override fun onCreateViewHolder(p: ViewGroup, vt: Int): ViewHolder {
            return ViewHolder(ItemTanggalGudangBinding.inflate(LayoutInflater.from(p.context), p, false))
        }
        override fun onBindViewHolder(vh: ViewHolder, pos: Int) {
            vh.b.tvItemTanggal.text = data[pos]
            vh.b.root.setOnClickListener { clickListener(data[pos]) }
        }
        override fun getItemCount(): Int = data.size
    }
}