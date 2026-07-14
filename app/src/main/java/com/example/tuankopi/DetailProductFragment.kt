package com.example.tuankopi

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.tuankopi.databinding.FragmentDetailProductBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

class DetailProductFragment : Fragment() {

    private var _binding: FragmentDetailProductBinding? = null
    private val binding get() = _binding!!
    private lateinit var mFirestore: FirebaseFirestore

    private var idProduk = ""
    private var namaProduk = ""
    private var hargaProduk = 0L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailProductBinding.inflate(inflater, container, false)
        mFirestore = FirebaseFirestore.getInstance()

        // Ambil data dari argument Bundle yang dikirim
        idProduk = arguments?.getString("PROD_ID") ?: "-"
        namaProduk = arguments?.getString("PROD_NAMA") ?: "-"
        hargaProduk = arguments?.getLong("PROD_HARGA", 0) ?: 0L

        updateTampilanUI()

        binding.btnEditProduk.setOnClickListener { tampilkanDialogEdit() }
        binding.btnHapusProduk.setOnClickListener { tampilkanDialogHapus() }

        return binding.root
    }

    private fun updateTampilanUI() {
        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        val hargaFormat = format.format(hargaProduk).replace(",00", "")

        binding.tvDetailIdProduk.text = "ID Menu: $idProduk"
        binding.tvDetailNamaProduk.text = "Varian: $namaProduk"
        binding.tvDetailHargaProduk.text = "Harga Jual: $hargaFormat"
    }

    private fun tampilkanDialogEdit() {
        val context = requireContext()
        val dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder.setTitle("Edit Menu Kopi")

        // Buat Layout untuk input dialog
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(60, 40, 60, 10)

        val etNama = EditText(context)
        etNama.hint = "Nama Varian Menu"
        etNama.setText(namaProduk)
        layout.addView(etNama)

        val etHarga = EditText(context)
        etHarga.hint = "Harga Jual (Rp)"
        etHarga.inputType = InputType.TYPE_CLASS_NUMBER
        etHarga.setText(hargaProduk.toString())
        layout.addView(etHarga)

        dialogBuilder.setView(layout)

        dialogBuilder.setPositiveButton("Simpan") { dialog, _ ->
            val namaBaru = etNama.text.toString().trim()
            val hargaBaruStr = etHarga.text.toString().trim()

            if (namaBaru.isEmpty() || hargaBaruStr.isEmpty()) {
                Toast.makeText(context, "Data tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val hargaBaru = hargaBaruStr.toLong()

            mFirestore.collection("products").document(idProduk)
                .update(
                    "nama_produk", namaBaru,
                    "harga_jual", hargaBaru
                )
                .addOnSuccessListener {
                    Toast.makeText(context, "Produk berhasil diupdate!", Toast.LENGTH_SHORT).show()
                    namaProduk = namaBaru
                    hargaProduk = hargaBaru
                    updateTampilanUI()
                    dialog.dismiss()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Gagal update: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
        dialogBuilder.setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
        dialogBuilder.create().show()
    }

    private fun tampilkanDialogHapus() {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Menu")
            .setMessage("Apakah Anda yakin ingin menghapus $namaProduk dari katalog secara permanen?")
            .setPositiveButton("Hapus") { dialog, _ ->

                mFirestore.collection("products").document(idProduk)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "$namaProduk berhasil dihapus!", Toast.LENGTH_SHORT).show()
                        requireActivity().supportFragmentManager.popBackStack()
                        dialog.dismiss()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Gagal menghapus: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .create().show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}