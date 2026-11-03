package com.xtremerpie.guardian

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import android.view.ViewGroup
import android.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity() {
    private lateinit var url: EditText
    private lateinit var token: EditText
    private lateinit var share: CheckBox
    private val prefs by lazy { getSharedPreferences("guardian", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 42, 36, 24)
            setBackgroundColor(Color.rgb(10, 18, 31))
        }
        fun label(text: String, size: Float = 16f) {
            layout.addView(TextView(this).apply { this.text = text; textSize = size; setTextColor(Color.WHITE); setPadding(0, 12, 0, 8) })
        }
        label("✦  Xtremerpie Guardian", 24f)
        label("Visible phone check-in service", 14f)
        url = EditText(this).apply { hint = "Vercel URL, e.g. https://your-app.vercel.app"; setSingleLine(); setTextColor(Color.WHITE); setHintTextColor(0xffaab8ca.toInt()); setText(prefs.getString("url", "")) }
        token = EditText(this).apply { hint = "Device key"; setSingleLine(); setTextColor(Color.WHITE); setHintTextColor(0xffaab8ca.toInt()); setText(prefs.getString("token", "")); inputType = 0x81 }
        layout.addView(url, LinearParams())
        layout.addView(token, LinearParams())
        share = CheckBox(this).apply {
            text = "I agree to share my location while monitoring is active"
            setTextColor(Color.WHITE)
            isChecked = prefs.getBoolean("share", false)
        }
        layout.addView(share)
        label("The service displays an ongoing Android notification. Check-ins stop if Android stops the service or the phone loses connectivity. Location is optional.", 13f)
        val start = Button(this).apply { text = "Save & start monitoring service" }
        val stop = Button(this).apply { text = "Stop service" }
        val status = TextView(this).apply { setTextColor(0xff7dd9a8.toInt()); text = "Ready" }
        layout.addView(start); layout.addView(stop); layout.addView(status)
        setContentView(ScrollView(this).apply { addView(layout, ViewGroup.LayoutParams(-1, -2)) })
        start.setOnClickListener {
            val base = url.text.toString().trim().trimEnd('/')
            val key = token.text.toString().trim()
            if (!base.startsWith("https://") || key.length < 16) {
                status.text = "Enter an HTTPS Vercel URL and valid device key (at least 16 characters)."; return@setOnClickListener
            }
            prefs.edit().putString("url", base).putString("token", key).putBoolean("share", share.isChecked).apply()
            val needed = mutableListOf<String>()
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                needed.add(Manifest.permission.POST_NOTIFICATIONS)
            if (share.isChecked) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    needed.add(Manifest.permission.ACCESS_FINE_LOCATION)
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    needed.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            if (needed.isNotEmpty()) ActivityCompat.requestPermissions(this, needed.toTypedArray(), 44)
            val i = Intent(this, GuardianService::class.java)
            if (Build.VERSION.SDK_INT >= 26) ContextCompat.startForegroundService(this, i) else startService(i)
            status.text = "Service start requested. Check the persistent notification."
        }
        stop.setOnClickListener { stopService(Intent(this, GuardianService::class.java)); status.text = "Service stopped." }
    }
    private fun LinearParams() = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 8 }
}