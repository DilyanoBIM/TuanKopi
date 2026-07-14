package com.example.tuankopi

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.tuankopi.databinding.FragmentRiderDetailKonfirmasiBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class RiderDetailKonfirmasiFragment : Fragment() {

    private var _binding: FragmentRiderDetailKonfirmasiBinding? = null
    private val binding get() = _binding!!

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mFirestore: FirebaseFirestore
    private var detailListener: ListenerRegistration? = null

    private var tanggalTarget = ""
    private var docIdStokTarget = ""
    private var sesiDistribusiKe = 1L

    private val listPerluKonfirmasi = ArrayList<String>()
    private val petaVerifikasiLokal = HashMap<String, Boolean>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRiderDetailKonfirmasiBinding.inflate(inflater, container, false)
        mAuth = FirebaseAuth.getInstance()
        mFirestore = FirebaseFirestore.getInstance()

        binding.btnBackDetail.setOnClickListener {
            (activity as? RiderDashboardActivity)?.gantiRiderFragment(RiderKonfirmasiStokFragment())
        }

        tanggalTarget = arguments?.getString("KEY_TANGGAL") ?: ""
        val cleanTgl = tanggalTarget.replace("-", "")
        docIdStokTarget = "${cleanTgl}_${mAuth.currentUser?.uid ?: ""}"

        dengarkanLivePendingStok()

        binding.btnTerimaStokFinal.setOnClickListener { eksekusiTerimaStokSesi() }
        return binding.root
    }

    private fun dengarkanLivePendingStok() {
        detailListener = mFirestore.collection("stok_harian").document(docIdStokTarget)
            .addSnapshotListener { snapshot, error ->
                if (error != null || !isAdded) return@addSnapshotListener
                if (snapshot == null || !snapshot.exists() || !snapshot.contains("pending_suplai")) {
                    binding.containerDetailItemStok.removeAllViews()
                    binding.tvStatusKunciStok.text = "Tidak ada distribusi baru dari Owner."
                    binding.tvStatusKunciStok.visibility = View.VISIBLE
                    binding.btnTerimaStokFinal.visibility = View.GONE
                    return@addSnapshotListener
                }

                binding.containerDetailItemStok.removeAllViews()
                listPerluKonfirmasi.clear()

                val pendingMap = snapshot.get("pending_suplai") as? Map<*, *> ?: return@addSnapshotListener
                sesiDistribusiKe = pendingMap["sesi_ke"] as? Long ?: 1L
                val modalBaru = pendingMap["modal_baru"] as? Long ?: 0L
                val itemsPending = pendingMap["items"] as? Map<*, *> ?: HashMap<String, Any>()

                val tanggalIndo = formatKeTanggalIndo(tanggalTarget)
                binding.tvTanggalTerpilih.text = "Tanggal: $tanggalIndo (Distribusi Ke-$sesiDistribusiKe)"
                binding.btnTerimaStokFinal.text = "TERIMA STOK KE-$sesiDistribusiKe"

                // Menampilkan Modal Jika Ada
                if (modalBaru > 0) {
                    listPerluKonfirmasi.add("KEY_MODAL")
                    if (!petaVerifikasiLokal.containsKey("KEY_MODAL")) petaVerifikasiLokal["KEY_MODAL"] = false
                    val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
                    tampilkanBarisVerifikasiGenerik("KEY_MODAL", "💰 Modal Cash Baru", formatter.format(modalBaru).replace(",00", ""))
                }

                // Menampilkan Produk
                for ((k, v) in itemsPending) {
                    val idProd = k.toString()
                    val data = v as? Map<*, *> ?: continue
                    val nm = data["nama_produk"] as? String ?: "Kopi"
                    val qty = data["qty"] as? Long ?: 0L

                    listPerluKonfirmasi.add(idProd)
                    if (!petaVerifikasiLokal.containsKey(idProd)) petaVerifikasiLokal[idProd] = false
                    tampilkanBarisVerifikasiGenerik(idProd, nm, "$qty Cup")
                }

                evaluasiStateTombolKeputusan()
            }
    }

    private fun tampilkanBarisVerifikasiGenerik(idKey: String, namaLabel: String, valueLabel: String) {
        val ctx = context ?: return
        val cardItem = CardView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dpToPx(ctx, 12)) }
            radius = dpToPx(ctx, 8).toFloat()
            cardElevation = dpToPx(ctx, 2).toFloat()
            setCardBackgroundColor(Color.WHITE)
        }

        val susunanKonten = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val p = dpToPx(ctx, 16)
            setPadding(p, p, p, p)
        }

        susunanKonten.addView(TextView(ctx).apply {
            text = "$namaLabel — $valueLabel"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#191C1E"))
        })

        val btnSesuai = Button(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(ctx, 40)).apply { topMargin = dpToPx(ctx, 10) }
            text = "SESUAI"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            if (petaVerifikasiLokal[idKey] == true) {
                setBackgroundColor(Color.parseColor("#2E7D32"))
                setTextColor(Color.WHITE)
            } else {
                setBackgroundColor(Color.parseColor("#E0E0E0"))
                setTextColor(Color.BLACK)
            }
        }

        btnSesuai.setOnClickListener {
            petaVerifikasiLokal[idKey] = true
            btnSesuai.setBackgroundColor(Color.parseColor("#2E7D32"))
            btnSesuai.setTextColor(Color.WHITE)
            evaluasiStateTombolKeputusan()
        }

        susunanKonten.addView(btnSesuai)
        cardItem.addView(susunanKonten)
        binding.containerDetailItemStok.addView(cardItem)
    }

    private fun evaluasiStateTombolKeputusan() {
        if (listPerluKonfirmasi.isEmpty()) return

        var semuaDiklik = true
        for (id in listPerluKonfirmasi) {
            if (petaVerifikasiLokal[id] != true) {
                semuaDiklik = false
                break
            }
        }

        binding.tvStatusKunciStok.visibility = View.GONE
        binding.btnTerimaStokFinal.visibility = if (semuaDiklik) View.VISIBLE else View.GONE
    }

    private fun eksekusiTerimaStokSesi() {
        val refRider = mFirestore.collection("stok_harian").document(docIdStokTarget)

        mFirestore.runTransaction { transaction ->
            val snapshot = transaction.get(refRider)
            if (!snapshot.exists()) throw FirebaseFirestoreException("Dokumen tidak ditemukan!", FirebaseFirestoreException.Code.NOT_FOUND)

            val pendingMap = snapshot.get("pending_suplai") as? Map<*, *> ?: throw FirebaseFirestoreException("Tidak ada pending suplai!", FirebaseFirestoreException.Code.NOT_FOUND)
            val modalBaru = pendingMap["modal_baru"] as? Long ?: 0L
            val itemsPending = pendingMap["items"] as? Map<*, *> ?: HashMap<String, Any>()

            val detailStokLama = snapshot.get("detail_stok") as? Map<*, *> ?: HashMap<String, Any>()
            val detailStokBaru = HashMap<String, Any>()
            for ((k, v) in detailStokLama) detailStokBaru[k.toString()] = v!!

            for ((k, v) in itemsPending) {
                val idProd = k.toString()
                val dataP = v as Map<*, *>
                val qty = dataP["qty"] as Long
                val hrg = dataP["harga_jual"] as Long
                val nm = dataP["nama_produk"] as String

                val itemLama = detailStokBaru[idProd] as? Map<*, *>
                if (itemLama != null) {
                    val stokAwal = itemLama["stok_awal"] as Long
                    val stokTamb = (itemLama["stok_tambahan"] as? Long ?: 0L) + qty
                    val total = stokAwal + stokTamb
                    val terjual = itemLama["terjual"] as? Long ?: 0L

                    val itemB = HashMap<String, Any>()
                    for ((sk, sv) in itemLama) itemB[sk.toString()] = sv!!
                    itemB["stok_tambahan"] = stokTamb
                    itemB["total_stok"] = total
                    itemB["sisa_stok"] = total - terjual
                    detailStokBaru[idProd] = itemB
                } else {
                    detailStokBaru[idProd] = hashMapOf(
                        "nama_produk" to nm, "stok_awal" to qty, "stok_tambahan" to 0L,
                        "total_stok" to qty, "terjual" to 0L, "sisa_stok" to qty,
                        "harga_jual" to hrg, "diterima" to true // Flag Diterima
                    )
                }
            }

            val modalLama = snapshot.getLong("modal_kembalian") ?: 0L
            transaction.update(refRider, "modal_kembalian", modalLama + modalBaru)
            transaction.update(refRider, "detail_stok", detailStokBaru)
            transaction.update(refRider, "sesi_terakhir", sesiDistribusiKe)
            transaction.update(refRider, "pending_suplai", FieldValue.delete())
            transaction.update(refRider, "status_stok", "AKTIF")

            null
        }.addOnSuccessListener {
            Toast.makeText(context, "Distribusi Ke-$sesiDistribusiKe Berhasil Diterima!", Toast.LENGTH_SHORT).show()
            petaVerifikasiLokal.clear()
            (activity as? RiderDashboardActivity)?.gantiRiderFragment(RiderDashboardFragment())
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Gagal: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun formatKeTanggalIndo(tanggal: String): String {
        return try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(tanggal)
            if (date != null) SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")).format(date) else tanggal
        } catch (e: Exception) { tanggal }
    }
    private fun dpToPx(c: Context, dp: Int): Int = (dp * c.resources.displayMetrics.density).toInt()
    override fun onDestroyView() { super.onDestroyView(); detailListener?.remove(); _binding = null }
}