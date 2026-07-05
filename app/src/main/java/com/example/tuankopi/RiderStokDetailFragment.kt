package com.example.tuankopi

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tuankopi.databinding.FragmentRiderStokDetailBinding
import com.example.tuankopi.databinding.ItemKatalogStokDetailBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

class RiderStokDetailFragment : Fragment() {

    private var _binding: FragmentRiderStokDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var mFirestore: FirebaseFirestore
    private val listDetailKopi = ArrayList<SubDetailKopi>()
    private lateinit var mAdapter: DetailKopiAdapter

    private var targetDocId = ""
    private var stringTanggal = ""
    private var nominalModal = 0L

    companion object {
        fun newInstance(docId: String, tanggal: String, modal: Long): RiderStokDetailFragment {
            val fragment = RiderStokDetailFragment()
            val args = Bundle().apply {
                putString("ARG_DOC_ID", docId)
                putString("ARG_TANGGAL", tanggal)
                putLong("ARG_MODAL", modal)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            targetDocId = it.getString("ARG_DOC_ID") ?: ""
            stringTanggal = it.getString("ARG_TANGGAL") ?: ""
            nominalModal = it.getLong("ARG_MODAL")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRiderStokDetailBinding.inflate(inflater, container, false)
        mFirestore = FirebaseFirestore.getInstance()

        setupHeaderInformasi()
        setupRecyclerViewDetail()
        muatSpesifikDataDetailStokHarian()

        binding.btnBackKeDaftarTanggal.setOnClickListener {
            (activity as? RiderDashboardActivity)?.gantiRiderFragment(RiderStokAktifFragment())
        }

        return binding.root
    }

    private fun setupHeaderInformasi() {
        binding.tvDetailHeaderTanggal.text = "Muatan: $stringTanggal"
        val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        binding.tvDetailHeaderModal.text = "Modal Pecahan Kembalian: " + formatter.format(nominalModal).replace(",00", "")
    }

    private fun setupRecyclerViewDetail() {
        mAdapter = DetailKopiAdapter(listDetailKopi)
        binding.rvKatalogProdukDetail.layoutManager = LinearLayoutManager(context)
        binding.rvKatalogProdukDetail.adapter = mAdapter
    }

    private fun muatSpesifikDataDetailStokHarian() {
        if (targetDocId.isEmpty()) return

        mFirestore.collection("stok_harian").document(targetDocId).get()
            .addOnSuccessListener { doc ->
                if (!isAdded || doc == null || !doc.exists()) return@addOnSuccessListener

                val rawMap = doc.get("detail_stok") as? Map<*, *>
                val listRender = ArrayList<SubDetailKopi>()

                if (rawMap != null) {
                    for ((_, value) in rawMap) {
                        val dataMap = value as? Map<*, *> ?: continue
                        val isDiterima = dataMap["diterima"] as? Boolean ?: false

                        if (isDiterima) {
                            val nama = dataMap["nama_produk"] as? String ?: "Menu"
                            val awal = dataMap["stok_awal"] as? Long ?: 0L
                            val laku = dataMap["terjual"] as? Long ?: 0L
                            val sisa = dataMap["sisa_stok"] as? Long ?: 0L
                            listRender.add(SubDetailKopi(nama, awal, laku, sisa))
                        }
                    }
                }

                listDetailKopi.clear()
                listDetailKopi.addAll(listRender.sortedBy { it.namaProduk })
                mAdapter.notifyDataSetChanged()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class DetailKopiAdapter(private val data: List<SubDetailKopi>) :
        RecyclerView.Adapter<DetailKopiAdapter.ViewHolder>() {

        inner class ViewHolder(val b: ItemKatalogStokDetailBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(ItemKatalogStokDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val kopi = data[position]
            holder.b.tvKatalogNamaKopi.text = kopi.namaProduk
            holder.b.tvKatalogInfoQty.text = "Bawa: ${kopi.stokAwal}  |  Laku: ${kopi.terjual}  |  Sisa: ${kopi.sisaStok}"
        }

        override fun getItemCount(): Int = data.size
    }
}