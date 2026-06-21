package com.flipperdroid.modules.root

import java.io.BufferedReader
import java.io.InputStreamReader

object RootShell {

    private var hasRoot: Boolean? = null

    fun checkRoot(): Boolean {
        if (hasRoot != null) return hasRoot!!

        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo root_test"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            val exitCode = process.waitFor()
            hasRoot = exitCode == 0 && line == "root_test"
            hasRoot!!
        } catch (e: Exception) {
            hasRoot = false
            false
        }
    }

    fun execute(command: String): ShellResult {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val stdout = BufferedReader(InputStreamReader(process.inputStream))
            val stderr = BufferedReader(InputStreamReader(process.errorStream))

            val output = mutableListOf<String>()
            val errors = mutableListOf<String>()

            stdout.useLines { lines -> lines.forEach { output.add(it) } }
            stderr.useLines { lines -> lines.forEach { errors.add(it) } }

            val exitCode = process.waitFor()

            ShellResult(
                exitCode = exitCode,
                output = output,
                errors = errors,
                success = exitCode == 0
            )
        } catch (e: Exception) {
            ShellResult(
                exitCode = -1,
                output = emptyList(),
                errors = listOf(e.message ?: "Unknown error"),
                success = false
            )
        }
    }

    fun executeWithTimeout(command: String, timeoutMs: Long = 30000): ShellResult {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val stdout = BufferedReader(InputStreamReader(process.inputStream))
            val stderr = BufferedReader(InputStreamReader(process.errorStream))

            val output = mutableListOf<String>()
            val errors = mutableListOf<String>()

            val thread = Thread {
                stdout.useLines { lines -> lines.forEach { output.add(it) } }
                stderr.useLines { lines -> lines.forEach { errors.add(it) } }
            }
            thread.start()
            thread.join(timeoutMs)

            val exitCode = process.exitValue()
            ShellResult(
                exitCode = exitCode,
                output = output,
                errors = errors,
                success = exitCode == 0
            )
        } catch (e: Exception) {
            ShellResult(
                exitCode = -1,
                output = emptyList(),
                errors = listOf(e.message ?: "Timeout or error"),
                success = false
            )
        }
    }

    fun enableMonitorMode(interfaceName: String = "wlan0"): Boolean {
        val result = execute("""
            ifconfig $interfaceName down
            iw dev $interfaceName set monitor none
            ifconfig $interfaceName up
        """.trimIndent())
        return result.success
    }

    fun disableMonitorMode(interfaceName: String = "wlan0"): Boolean {
        val result = execute("""
            ifconfig $interfaceName down
            iw dev $interfaceName set type managed
            ifconfig $interfaceName up
        """.trimIndent())
        return result.success
    }

    fun enableSoftAp(ssid: String = "FlipperDroid"): Boolean {
        val result = execute("""
            ifconfig wlan0 down
            iw dev wlan0 set type ap
            ifconfig wlan0 up
            hostapd -B /data/local/tmp/hostapd.conf
            dnsmasq -C /data/local/tmp/dnsmasq.conf
        """.trimIndent())
        return result.success
    }

    fun disableSoftAp(): Boolean {
        val result = execute("""
            killall hostapd
            killall dnsmasq
            ifconfig wlan0 down
            iw dev wlan0 set type managed
            ifconfig wlan0 up
        """.trimIndent())
        return result.success
    }
}

data class ShellResult(
    val exitCode: Int,
    val output: List<String>,
    val errors: List<String>,
    val success: Boolean
)