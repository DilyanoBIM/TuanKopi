package com.example.tuankopi

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONArray
import java.text.NumberFormat
import java.util.Locale


class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Memeriksa keberadaan Payload Data Khusus transaksi dari backend PHP
        if (remoteMessage.data.isNotEmpty()) {
            val orderId = remoteMessage.data["order_id"] ?: ""
            val namaRider = remoteMessage.data["nama_rider"] ?: "Rider"
            val totalHarga = remoteMessage.data["total_harga"]?.toLong() ?: 0L
            val rawItemsJson = remoteMessage.data["items"] ?: "[]"

            Log.d("FCM_TRANSAKSI_MASUK", "ID Pesanan: $orderId, Oleh: $namaRider, Total: Rp $totalHarga")

            // Contoh Ekstraksi Array Items dari payload data
            try {
                val jsonArray = JSONArray(rawItemsJson)
                for (i in 0 until jsonArray.length()) {
                    val itemObject = jsonArray.getJSONObject(i)
                    val namaProduk = itemObject.getString("nama_produk")
                    val qty = itemObject.getInt("qty")
                    val hargaSatuan = itemObject.getLong("harga_satuan")
                    Log.d("FCM_DETAIL_ITEM", "Membeli: $namaProduk x$qty (@Rp $hargaSatuan)")
                }
            } catch (e: Exception) {
                Log.e("FCM_PARSING_ERROR", "Gagal membaca detail item transaksi", e)
            }

            // Tampilkan push notification di laci sistem perangkat Owner
            tampilkanNotifikasiKeOwner(orderId, namaRider, totalHarga)
        }
    }

    private fun tampilkanNotifikasiKeOwner(orderId: String, namaRider: String, totalHarga: Long) {
        val intent = Intent(this, OwnerDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("TARGET_ORDER_ID", orderId) // Membuka detail transaksi langsung jika diklik
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        val totalFormatted = formatter.format(totalHarga).replace(",00", "")

        val channelId = "SALURAN_OMSET_OWNER"
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stars) // Ganti sesuai aset drawable ikon Anda
            .setContentTitle("Transaksi QRIS Sukses! 🎉")
            .setContentText("Rider $namaRider berhasil menjual kopi senilai $totalFormatted")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "Detail Invoice:\nNo. Order: $orderId\nNama Rider: $namaRider\nTotal Penerimaan: $totalFormatted\n\nData sistem real-time terlah terbarui otomatis."
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Notifikasi Omset Masuk",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Menampilkan data rekap transaksi POS Keliling Rider secara real-time"
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(orderId.hashCode(), builder.build())
    }
}