package com.flipperdroid.modules.wifi

import com.flipperdroid.modules.root.RootShell
import java.io.File

object PasswordCracker {

    /**
     * Crack WPA/WPA2 handshake using aircrack-ng
     * Requires: aircrack-ng binary installed on device or in assets
     */
    fun crack(hccapFile: String, wordlistFile: String): String {
        return try {
            // Check if aircrack exists
            val check = RootShell.execute("which aircrack-ng || echo not_found")
            if (check.output.any { it.contains("not_found") }) {
                // Try built-in binary from assets if available
                val result = RootShell.execute("""
                    aircrack-ng -w $wordlistFile -b ${getBSSID()} $hccapFile 2>&1
                """.trimIndent())

                if (result.success) {
                    val keyFound = result.output.find { it.contains("KEY FOUND!") }
                    if (keyFound != null) {
                        "✅ Пароль найден: ${keyFound.substringAfter("[ ").substringBefore(" ]")}"
                    } else {
                        "❌ Пароль не найден в словаре"
                    }
                } else {
                    "❌ aircrack-ng не найден. Установите: pkg install aircrack-ng"
                }
            } else {
                "❌ Aircrack-ng не найден на устройстве"
            }
        } catch (e: Exception) {
            "❌ Ошибка: ${e.message}"
        }
    }

    /**
     * Alternative: PMKID cracking without handshake
     */
    fun crackPMKID(pmkidFile: String, wordlistFile: String): String {
        return try {
            val result = RootShell.execute("""
                hcxpcaptool -z pmkid.16800 $pmkidFile && hashcat -m 16800 pmkid.16800 $wordlistFile --force 2>&1
            """.trimIndent())

            if (result.output.any { it.contains("Cracked") || it.contains("Found") }) {
                val pass = result.output.find { it.contains(":") && !it.contains(" ") }
                "✅ PMKID взломан! Пароль: ${pass ?: "см. вывод"}"
            } else {
                "❌ Пароль не найден"
            }
        } catch (e: Exception) {
            "❌ Ошибка: ${e.message}"
        }
    }

    /**
     * Save wordlist to device
     */
    fun saveWordlist(context: android.content.Context, filename: String = "wordlist.txt") {
        try {
            val file = File(context.filesDir, filename)
            if (!file.exists()) {
                context.assets.open("wordlists/common.txt").use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                // Copy to /sdcard for aircrack
                RootShell.execute("cp ${file.absolutePath} /sdcard/$filename")
            }
        } catch (e: Exception) {
            // silent
        }
    }

    private var targetBSSID: String = ""

    fun setTargetBSSID(bssid: String) {
        targetBSSID = bssid
    }

    private fun getBSSID(): String = targetBSSID.ifEmpty { "00:11:22:33:44:55" }
}