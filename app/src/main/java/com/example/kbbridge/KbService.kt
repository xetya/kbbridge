package com.example.kbbridge

import android.accessibilityservice.AccessibilityService
import android.view.InputDevice
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors

class KbService : AccessibilityService() {
    companion object {
        @Volatile var instance: KbService? = null
    }

    @Volatile var status: String = "Belum terhubung"
    @Volatile private var out: PrintWriter? = null
    private var sock: Socket? = null
    private val ex = Executors.newSingleThreadExecutor()

    private val skip = setOf(
        KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN,
        KeyEvent.KEYCODE_VOLUME_MUTE, KeyEvent.KEYCODE_POWER,
        KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_APP_SWITCH
    )

    override fun onServiceConnected() {
        instance = this
        val info = serviceInfo
        info.flags = info.flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        serviceInfo = info
        status = "Service aktif. Isi IP lalu Connect."
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        disconnect()
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        disconnect()
        instance = null
        ex.shutdown()
        super.onDestroy()
    }

    fun isConnected() = out != null

    fun connect(addr: String) {
        status = "Menghubungkan..."
        ex.execute {
            try {
                try { sock?.close() } catch (_: Exception) {}
                out = null
                val s = Socket()
                s.tcpNoDelay = true
                s.keepAlive = true
                s.connect(InetSocketAddress(addr, 5005), 3000)
                sock = s
                out = PrintWriter(s.getOutputStream(), true)
                status = "Terhubung. Keyboard eksternal sekarang mengetik di laptop."
            } catch (e: Exception) {
                status = "Gagal: ${e.message}"
            }
        }
    }

    fun disconnect() {
        out = null
        try { sock?.close() } catch (_: Exception) {}
        sock = null
        status = "Terputus. Keyboard kembali ke HP."
    }

    override fun onKeyEvent(e: KeyEvent): Boolean {
        val o = out ?: return false              // tidak terhubung: biarkan HP memakai keyboard
        val d = e.device ?: return false
        // hanya keyboard fisik eksternal, bukan tombol bawaan HP / keyboard layar
        if (d.isVirtual || d.keyboardType != InputDevice.KEYBOARD_TYPE_ALPHABETIC) return false
        if (e.keyCode in skip) return false
        if (e.action != KeyEvent.ACTION_DOWN && e.action != KeyEvent.ACTION_UP) return true
        if (e.action == KeyEvent.ACTION_DOWN && e.repeatCount > 0) return true
        val name = KeyEvent.keyCodeToString(e.keyCode).removePrefix("KEYCODE_")
        val act = if (e.action == KeyEvent.ACTION_DOWN) "d" else "u"
        ex.execute {
            try {
                o.println("$act:$name")
                if (o.checkError()) {
                    out = null
                    try { sock?.close() } catch (_: Exception) {}
                    status = "Koneksi putus. Tekan Connect lagi."
                }
            } catch (_: Exception) {}
        }
        return true                               // tombol dikonsumsi, tidak masuk ke HP
    }
}
