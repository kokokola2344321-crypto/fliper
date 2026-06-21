package com.flipperdroid.modules.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import kotlinx.coroutines.*

class BLESpammer {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var isRunning = false
    private var spamJob: Job? = null
    private var scanCallback: ScanCallback? = null

    private val iOSDevices = listOf(
        "iPhone", "iPad", "Apple TV", "MacBook Pro", "MacBook Air",
        "iMac", "AirPods", "AirPods Pro", "AirPods Max", "Apple Watch",
        "HomePod", "HomePod mini", "Apple Pencil", "Magic Keyboard",
        "Magic Mouse", "Magic Trackpad", "iPhone 15", "iPhone 16",
        "iPad Pro", "iPad Air", "Apple Vision Pro"
    )

    private val androidDevices = listOf(
        "Samsung Galaxy S24", "Samsung Galaxy S25", "Google Pixel 9",
        "Xiaomi 14", "Xiaomi 15", "OnePlus 13", "OnePlus 13R",
        "OPPO Find X8", "Vivo X200", "Realme GT 7",
        "Galaxy Tab S10", "Pixel Tablet", "Xiaomi Pad 7"
    )

    private val samsungDevices = listOf(
        "Samsung Galaxy S24", "Samsung Galaxy S25", "Samsung Galaxy S25 Ultra",
        "Samsung Galaxy Z Fold 6", "Samsung Galaxy Z Flip 6",
        "Samsung Galaxy Tab S10 Ultra", "Samsung Galaxy Watch 7",
        "Samsung Galaxy Buds 3 Pro", "Samsung Galaxy Ring",
        "Samsung Galaxy Book 4"
    )

    fun init(context: Context) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    }

    fun startScan(onDeviceFound: (BluetoothDevice) -> Unit) {
        if (bluetoothLeScanner == null) return

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                onDeviceFound(result.device)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { onDeviceFound(it.device) }
            }

            override fun onScanFailed(errorCode: Int) {}
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        bluetoothLeScanner?.startScan(null, settings, scanCallback)
    }

    fun startSpam(targets: List<String>, context: Context? = null) {
        if (bluetoothAdapter == null) return
        isRunning = true

        spamJob = CoroutineScope(Dispatchers.IO).launch {
            val advertiser = bluetoothAdapter?.bluetoothLeAdvertiser
            if (advertiser == null) return@launch

            val names = when {
                targets.contains("iOS") && targets.size == 1 -> iOSDevices
                targets.contains("Android") && targets.size == 1 -> androidDevices
                targets.contains("Samsung") && targets.size == 1 -> samsungDevices
                else -> iOSDevices + androidDevices + samsungDevices
            }

            var index = 0
            while (isRunning) {
                try {
                    val name = names[index % names.size]
                    val advertiseData = AdvertiseData.Builder()
                        .setIncludeDeviceName(true)
                        .setIncludeTxPowerLevel(true)
                        .build()

                    val settings = AdvertiseSettings.Builder()
                        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                        .setConnectable(false)
                        .build()

                    advertiser.startAdvertising(settings, advertiseData, object : AdvertiseCallback() {
                        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {}
                        override fun onStartFailure(errorCode: Int) {}
                    })

                    delay(50)
                    advertiser.stopAdvertising()
                    index++
                } catch (e: Exception) {
                    // Если ошибка - пробуем следующий
                }
                delay(30)
            }
        }
    }

    fun stop() {
        isRunning = false
        spamJob?.cancel()
        spamJob = null
        try {
            bluetoothLeScanner?.stopScan(scanCallback)
        } catch (_: Exception) {}
        try {
            bluetoothAdapter?.bluetoothLeAdvertiser?.stopAdvertising(object : AdvertiseCallback() {})
        } catch (_: Exception) {}
    }
}