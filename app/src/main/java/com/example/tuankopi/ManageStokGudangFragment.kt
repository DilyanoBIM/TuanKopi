package com.example.tuankopi

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tuankopi.databinding.FragmentManageStokGudangBinding
import com.example.tuankopi.databinding.ItemTanggalGudangBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ManageStokGudangFragment : Fragment() {

    private var _binding: FragmentManageStokGudangBinding? = null
    private val binding get() = _binding!!
    private lateinit var mFirestore: FirebaseFirestore
    private var listTanggal = ArrayList<String>()
    private lateinit var mAdapter: TanggalAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageStokGudangBinding.inflate(inflater, container, false)
        mFirestore = FirebaseFirestore.getInstance()

        setupRecyclerView()

        binding.fabAddStokGudang.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                val formattedMonth = String.format("%02d", month + 1)
                val formattedDay = String.format("%02d", dayOfMonth)
                val tanggalPilihan = "$year-$formattedMonth-$formattedDay"

                inisialisasiTanggalBaruDiFirestore(tanggalPilihan)

            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        muatDaftarTanggalGudang()
        return binding.root
    }

    private fun setupRecyclerView() {
        mAdapter = TanggalAdapter(listTanggal) { tgl ->
            tampilkanDialogPilihanAksi(tgl)
        }
        binding.rvTanggalGudang.layoutManager = LinearLayoutManager(requireContext())
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
                    listTanggal.addAll(setTanggal.sortedDescending())
                    mAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun tampilkanDialogPilihanAksi(tanggalTerpilih: String) {
        val tanggalIndo = formatKeTanggalIndo(tanggalTerpilih)
        val opsi = arrayOf("📦 Lihat Detail Logistik", "🗑️ Hapus Total Riwayat")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logistik: $tanggalIndo")
            .setItems(opsi) { dialog, which ->
                when (which) {
                    0 -> bukaDetailStok(tanggalTerpilih)
                    1 -> tampilkanDialogKonfirmasiHapus(tanggalTerpilih)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun tampilkanDialogKonfirmasiHapus(tanggalTarget: String) {
        val tanggalIndo = formatKeTanggalIndo(tanggalTarget)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Konfirmasi Hapus Total")
            .setMessage("Apakah Anda yakin ingin menghapus seluruh riwayat produksi & logistik pada tanggal $tanggalIndo secara permanen?")
            .setPositiveButton("Ya, Hapus Permanen") { dialog, _ ->
                val cleanedTanggalId = tanggalTarget.replace("-", "")

                mFirestore.collection("stok_gudang").document(cleanedTanggalId)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Riwayat tanggal $tanggalIndo berhasil dihapus total!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Gagal menghapus: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun inisialisasiTanggalBaruDiFirestore(tanggalTarget: String) {
        val cleanedTanggalId = tanggalTarget.replace("-", "")

        if (listTanggal.contains(tanggalTarget)) {
            bukaDetailStok(tanggalTarget)
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
                val tanggalIndo = formatKeTanggalIndo(tanggalTarget)
                Toast.makeText(requireContext(), "Sukses membuat slot tanggal $tanggalIndo!", Toast.LENGTH_SHORT).show()
                bukaDetailStok(tanggalTarget)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Gagal: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun bukaDetailStok(tanggal: String) {
        val fragment = DetailStokGudangFragment().apply {
            arguments = Bundle().apply { putString("LIHAT_TANGGAL", tanggal) }
        }
        (requireActivity() as OwnerDashboardActivity).bukaHalaman(fragment, "Detail Logistik Gudang")
    }

    private fun formatKeTanggalIndo(tanggal: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = parser.parse(tanggal)
            val formatter = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
            if (date != null) formatter.format(date) else tanggal
        } catch (e: Exception) {
            tanggal
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    inner class TanggalAdapter(
        private val data: List<String>,
        private val clickListener: (String) -> Unit
    ) : RecyclerView.Adapter<TanggalAdapter.ViewHolder>() {

        inner class ViewHolder(val b: ItemTanggalGudangBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(p: ViewGroup, vt: Int): ViewHolder {
            return ViewHolder(ItemTanggalGudangBinding.inflate(LayoutInflater.from(p.context), p, false))
        }

        override fun onBindViewHolder(vh: ViewHolder, pos: Int) {
            val tanggalAsli = data[pos]

            vh.b.tvItemTanggal.text = formatKeTanggalIndo(tanggalAsli)

            vh.b.root.setOnClickListener { clickListener(tanggalAsli) }
        }

        override fun getItemCount(): Int = data.size
    }
}