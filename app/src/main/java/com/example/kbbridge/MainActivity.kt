package com.example.kbbridge

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private val h = Handler(Looper.getMainLooper())
    private lateinit var status: TextView
    private lateinit var btn: Button
    private lateinit var ipField: EditText

    private val tick = object : Runnable {
        override fun run() {
            val s = KbService.instance
            if (s == null) {
                status.text = "Service belum aktif. Tekan 'Aktifkan Service', cari KB Bridge, lalu nyalakan."
                btn.text = "Connect"
            } else {
                status.text = s.status
                btn.text = if (s.isConnected()) "Disconnect" else "Connect"
            }
            h.postDelayed(this, 500)
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val prefs = getSharedPreferences("kb", MODE_PRIVATE)
        ipField = EditText(this).apply {
            hint = "IP laptop, mis. 192.168.43.5"
            setText(prefs.getString("ip", ""))
        }
        btn = Button(this).apply { text = "Connect" }
        val svcBtn = Button(this).apply { text = "Aktifkan Service (Aksesibilitas)" }
        status = TextView(this).apply { text = "..." }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 80, 40, 40)
            addView(svcBtn); addView(ipField); addView(btn); addView(status)
        })

        svcBtn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        btn.setOnClickListener {
            val s = KbService.instance ?: return@setOnClickListener
            if (s.isConnected()) {
                s.disconnect()
            } else {
                val addr = ipField.text.toString().trim()
                prefs.edit().putString("ip", addr).apply()
                s.connect(addr)
            }
        }
    }

    override fun onResume() { super.onResume(); h.post(tick) }
    override fun onPause() { h.removeCallbacks(tick); super.onPause() }
}
