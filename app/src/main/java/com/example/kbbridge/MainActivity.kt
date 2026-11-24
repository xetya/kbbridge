package com.example.kbbridge

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val ex = Executors.newSingleThreadExecutor()
    @Volatile private var out: PrintWriter? = null
    private var sock: Socket? = null
    private lateinit var status: TextView
    private lateinit var ipField: EditText

    private fun lockInputToBridge() {
        ipField.clearFocus()
        ipField.isFocusable = false
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(ipField.windowToken, 0)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
        )
    }

    private fun unlockInput() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        ipField.isFocusable = true
        ipField.isFocusableInTouchMode = true
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val prefs = getSharedPreferences("kb", MODE_PRIVATE)
        ipField = EditText(this).apply {
            hint = "IP laptop, mis. 192.168.43.5"
            setText(prefs.getString("ip", ""))
        }
        val btn = Button(this).apply { text = "Connect" }
        status = TextView(this).apply { text = "Belum terhubung" }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 80, 40, 40)
            addView(ipField); addView(btn); addView(status)
        })
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        btn.setOnClickListener {
            val addr = ipField.text.toString().trim()
            prefs.edit().putString("ip", addr).apply()
            status.text = "Menghubungkan..."
            unlockInput()
            ex.execute {
                try {
                    try { sock?.close() } catch (_: Exception) {}
                    out = null
                    val s = Socket()
                    s.tcpNoDelay = true
                    s.connect(InetSocketAddress(addr, 5005), 3000)
                    sock = s
                    out = PrintWriter(s.getOutputStream(), true)
                    runOnUiThread {
                        status.text = "Terhubung. Ketik di keyboard."
                        lockInputToBridge()
                    }
                } catch (e: Exception) {
                    runOnUiThread { status.text = "Gagal: ${e.message}" }
                }
            }
        }
    }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        val o = out ?: return super.dispatchKeyEvent(e)
        val kc = e.keyCode
        if (kc == KeyEvent.KEYCODE_BACK || kc == KeyEvent.KEYCODE_VOLUME_UP ||
            kc == KeyEvent.KEYCODE_VOLUME_DOWN || kc == KeyEvent.KEYCODE_POWER
        ) return super.dispatchKeyEvent(e)
        if (e.action != KeyEvent.ACTION_DOWN && e.action != KeyEvent.ACTION_UP) return true
        // Abaikan auto-repeat dari Android; OS laptop menangani repeat sendiri
        if (e.action == KeyEvent.ACTION_DOWN && e.repeatCount > 0) return true
        val name = KeyEvent.keyCodeToString(kc).removePrefix("KEYCODE_")
        val act = if (e.action == KeyEvent.ACTION_DOWN) "d" else "u"
        ex.execute {
            try {
                o.println("$act:$name")
                if (o.checkError()) {
                    out = null
                    runOnUiThread {
                        status.text = "Koneksi putus. Tekan Connect lagi."
                        unlockInput()
                    }
                }
            } catch (_: Exception) {}
        }
        return true
    }

    override fun onDestroy() {
        try { sock?.close() } catch (_: Exception) {}
        super.onDestroy()
    }
}
