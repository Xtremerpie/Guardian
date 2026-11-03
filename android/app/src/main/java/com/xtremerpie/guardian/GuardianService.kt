package com.xtremerpie.guardian

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationManager
import android.os.*
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class GuardianService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var active = false
    private var lastLocationSent = 0L
    private val prefs by lazy { getSharedPreferences("guardian", MODE_PRIVATE) }
    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            thread {
                try {
                    val base = prefs.getString("url", "") ?: ""
                    val key = prefs.getString("token", "") ?: ""
                    if (base.isNotBlank() && key.isNotBlank()) {
                        val config = request("$base/api/phone-config", key, null)
                        active = config.optBoolean("active", false)
                        if (active) {
                            val battery = (getSystemService(BATTERY_SERVICE) as android.os.BatteryManager)
                                .getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0,100)
                            request("$base/api/heartbeat", key, JSONObject().put("battery", battery))
                            if (prefs.getBoolean("share", false)) sendLocation(base, key)
                        }
                    }
                } catch (_: Exception) { /* dashboard detects missed check-ins */ }
                handler.postDelayed(this, 30_000)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val channel = "guardian_service"
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(channel, "Guardian monitoring", NotificationManager.IMPORTANCE_LOW))
        }
        val notification = NotificationCompat.Builder(this, channel)
            .setContentTitle("Xtremerpie Guardian is running")
            .setContentText("Visible safety check-ins are enabled. Open the app to stop.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true).build()
        if (Build.VERSION.SDK_INT >= 29) {
            var types = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            val hasLocation = prefs.getBoolean("share", false) &&
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (hasLocation) types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            startForeground(77, notification, types)
        } else startForeground(77, notification)
        running = true
        thread {
            try {
                val base = prefs.getString("url", "") ?: ""
                val key = prefs.getString("token", "") ?: ""
                if (base.isNotBlank() && key.isNotBlank()) request("$base/api/event", key, JSONObject().put("type","Phone service started"))
            } catch (_: Exception) {}
        }
        handler.post(tick)
    }

    private fun sendLocation(base: String, key: String) {
        val fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        val loc: Location? = try {
            (if (fine) listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
             else listOf(LocationManager.NETWORK_PROVIDER))
                .mapNotNull { provider -> try { lm.getLastKnownLocation(provider) } catch (_: SecurityException) { null } }
                .maxByOrNull { it.time }
        } catch (_: Exception) { null }
        if (loc != null && System.currentTimeMillis() - lastLocationSent > 60_000) {
            request("$base/api/location", key, JSONObject()
                .put("latitude", loc.latitude).put("longitude", loc.longitude).put("accuracy", loc.accuracy))
            lastLocationSent = System.currentTimeMillis()
        }
    }

    private fun request(url: String, key: String, body: JSONObject?): JSONObject {
        val conn = (URL(url).openConnection() as HttpURLConnection)
        conn.connectTimeout = 10_000; conn.readTimeout = 10_000
        conn.setRequestProperty("X-Device-Key", key)
        if (body != null) {
            conn.requestMethod = "POST"; conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        }
        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        val text = stream.bufferedReader().use { it.readText() }
        val code = conn.responseCode; conn.disconnect()
        if (code !in 200..299) throw IllegalStateException("HTTP $code")
        return if (text.isBlank()) JSONObject() else JSONObject(text)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onDestroy() {
        running = false; handler.removeCallbacksAndMessages(null)
        thread {
            try {
                val base = prefs.getString("url", "") ?: ""
                val key = prefs.getString("token", "") ?: ""
                if (base.isNotBlank() && key.isNotBlank()) request("$base/api/event", key, JSONObject().put("type","Phone service stopped"))
            } catch (_: Exception) {}
        }
        super.onDestroy()
    }
    override fun onBind(intent: Intent?) = null
}