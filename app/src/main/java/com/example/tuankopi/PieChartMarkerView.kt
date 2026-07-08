package com.example.tuankopi

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.text.NumberFormat
import java.util.Locale

class PieChartMarkerView(
    context: Context,
    private val nominalTunai: Float,
    private val nominalQris: Float
) : MarkerView(context, android.R.layout.simple_list_item_1) {

    private val tvContent: TextView = findViewById(android.R.id.text1)

    init {
        tvContent.setBackgroundColor(android.graphics.Color.parseColor("#F8F9FB"))
        tvContent.setTextColor(android.graphics.Color.parseColor("#191C1E"))
        tvContent.textSize = 12f
        tvContent.setPadding(20, 16, 20, 16)
    }

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e == null) return

        val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        val tunaiFormat = formatter.format(nominalTunai.toLong()).replace(",00", "")
        val qrisFormat = formatter.format(nominalQris.toLong()).replace(",00", "")

        val selisih = Math.abs(nominalTunai - nominalQris).toLong()
        val selisihFormat = formatter.format(selisih).replace(",00", "")

        val teksTooltip = "📋 Detail Pendapatan\n" +
                "• Tunai: $tunaiFormat\n" +
                "• QRIS: $qrisFormat\n" +
                "• Perbedaan Selisih: $selisihFormat"

        tvContent.text = teksTooltip
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF((-(width / 2)).toFloat(), (-height).toFloat() - 20f)
    }
}