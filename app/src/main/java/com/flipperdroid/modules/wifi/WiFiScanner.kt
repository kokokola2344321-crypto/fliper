package com.flipperdroid.modules.wifi

import android.content.Context
import android.net.wifi.WifiManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

data class ScannedNetwork(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequency: Int,
    val encrypted: Boolean
)

object WiFiScanner {

    suspend fun scan(): List<ScannedNetwork> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ScannedNetwork>()
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "iw dev wlan0 scan"))
            val output = process.inputStream.bufferedReader().readText()
            val lines = output.split("\n")

            var currentSsid = ""
            var currentBssid = ""
            var currentRssi = -100
            var currentFreq = 0
            var currentEnc = true

            for (line in lines) {
                when {
                    line.trim().startsWith("BSS ") -> {
                        if (currentSsid.isNotEmpty()) {
                            results.add(ScannedNetwork(currentSsid, currentBssid, currentRssi, currentFreq, currentEnc))
                        }
                        currentSsid = ""
                        currentBssid = line.trim().substringAfter("BSS ").substringBefore(" ")
                        currentEnc = true
                    }
                    line.trim().startsWith("SSID:") -> {
                        currentSsid = line.trim().substringAfter("SSID:").trim()
                    }
                    line.trim().startsWith("signal:") -> {
                        val sigStr = line.trim().substringAfter("signal:").trim().substringBefore(" ")
                        currentRssi = sigStr.toFloatOrNull()?.toInt() ?: -100
                    }
                    line.trim().startsWith("freq:") -> {
                        currentFreq = line.trim().substringAfter("freq:").trim().toIntOrNull() ?: 0
                    }
                    line.trim().contains("WPA") || line.trim().contains("RSN") -> {
                        currentEnc = true
                    }
                    line.trim() == "Group cipher: *CCMP" -> {
                        // open network might have no auth
                    }
                }
            }
            if (currentSsid.isNotEmpty()) {
                results.add(ScannedNetwork(currentSsid, currentBssid, currentRssi, currentFreq, currentEnc))
            }
        } catch (e: Exception) {
            // Fallback to Android WiFi API
            try {
                val ctx = getContext()
                if (ctx != null) {
                    val wifi = ctx.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                    val scanResults = wifi?.scanResults
                    scanResults?.forEach { result ->
                        results.add(ScannedNetwork(
                            ssid = result.SSID,
                            bssid = result.BSSID,
                            rssi = result.level,
                            frequency = result.frequency,
                            encrypted = result.capabilities.contains("WPA") || result.capabilities.contains("WEP")
                        ))
                    }
                }
            } catch (e2: Exception) {
                // silent
            }
        }
        results.distinctBy { it.bssid }
    }

    suspend fun scanPort(host: String, port: Int, timeout: Int = 2000): Boolean = withContext(Dispatchers.IO) {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), timeout)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    private var appContext: Context? = null
    fun init(context: Context) { appContext = context }
    private fun getContext(): Context? = appContext
}