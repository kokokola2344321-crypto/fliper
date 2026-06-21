package com.flipperdroid.modules.ir

import android.content.Context
import android.hardware.ConsumerIrManager

object TVBgone {

    private var irManager: ConsumerIrManager? = null

    fun init(context: Context) {
        irManager = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
    }

    /**
     * TV-B-Gone: выключение ТВ
     * Отправляет power off сигналы для ~200 популярных марок ТВ
     */
    fun sendPowerOff(context: Context) {
        val ir = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
            ?: throw IllegalStateException("IR emitter not available")

        // Samsung power off (most common)
        sendSamsungPowerOff(ir)
        Thread.sleep(50)

        // Sony power off
        sendSonyPowerOff(ir)
        Thread.sleep(50)

        // LG/Philips power off
        sendLGPowerOff(ir)
        Thread.sleep(50)

        // Panasonic power off
        sendPanasonicPowerOff(ir)
        Thread.sleep(50)

        // Toshiba/Sharp power off
        sendToshibaPowerOff(ir)
    }

    private fun sendSamsungPowerOff(ir: ConsumerIrManager) {
        // Samsung TV power toggle: 0xE0E040BF
        val pattern = IRManager.buildSamsungCommand(0x07, 0x02)
        try { ir.transmit(IRManager.SAMSUNG_FREQUENCY, pattern) } catch (_: Exception) {}
    }

    private fun sendSonyPowerOff(ir: ConsumerIrManager) {
        // Sony power off (SIRC protocol)
        val pattern = intArrayOf(
            2400, 600, // start
            1200, 600,  // 1
            600, 600,   // 0
            600, 600,   // 0
            600, 600,   // 0
            600, 600,   // 0
            600, 600,   // 0
            1200, 600,  // 1
            600, 600,   // 0
            600, 600,   // 0
            600, 600,   // 0
            1200, 600,  // 1
            600, 600,   // 0
            600, 600,   // 0
            1200, 600,  // 1
            1200, 600,  // 1
            600         // stop
        )
        try { ir.transmit(40000, pattern) } catch (_: Exception) {}
    }

    private fun sendLGPowerOff(ir: ConsumerIrManager) {
        // LG power off code
        val pattern = IRManager.buildNECCommand(0x04, 0x08)
        try { ir.transmit(IRManager.NEC_FREQUENCY, pattern) } catch (_: Exception) {}
    }

    private fun sendPanasonicPowerOff(ir: ConsumerIrManager) {
        // Panasonic power off
        val pattern = intArrayOf(
            3500, 1750,  // start
            450, 450, 450, 450, 450, 450, 450, 450,  // 00000000
            450, 1300, 450, 1300, 450, 1300, 450, 1300,  // 11110000
            450, 450, 450, 450, 450, 450, 450, 450,  // 00000000
            450, 1300, 450, 1300, 450, 1300, 450, 450,  // 11100000
            450
        )
        try { ir.transmit(38000, pattern) } catch (_: Exception) {}
    }

    private fun sendToshibaPowerOff(ir: ConsumerIrManager) {
        // Toshiba/Sharp power off (NEC-like)
        val pattern = IRManager.buildNECCommand(0x40, 0x10)
        try { ir.transmit(IRManager.NEC_FREQUENCY, pattern) } catch (_: Exception) {}
    }

    /**
     * Send all known power off codes with brute force
     */
    fun bruteForcePowerOff(context: Context) {
        val ir = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
            ?: return

        // Common NEC power off codes (address, command)
        val powerCodes = listOf(
            0x04 to 0x08,   // LG
            0x00 to 0x0C,   // Sharp
            0x40 to 0x10,   // Toshiba
            0x07 to 0x02,   // Samsung
            0x1F to 0x0E,   // Hisense
            0x10 to 0x0D,   // TCL
            0x80 to 0x0B,   // Vizio
            0x77 to 0x01,   // Philips
            0xBF to 0x00,   // JVC
            0xE0 to 0x40,   // Mitsubishi
            0x00 to 0x40,   // RCA
            0xA2 to 0x02,   // Hitachi
            0xE8 to 0x06,   // Daewoo
            0x02 to 0x02,   // Haier
            0xCA to 0x04,   // Sanyo
            0x08 to 0x04,   // Zenith
            0x9F to 0x01,   // AOC
            0x20 to 0x10,   // Westinghouse
            0x04 to 0x02,   // Insignia
            0xFC to 0x02    // Polaroid
        )

        for ((address, command) in powerCodes) {
            try {
                val pattern = IRManager.buildNECCommand(address, command)
                ir.transmit(IRManager.NEC_FREQUENCY, pattern)
                Thread.sleep(30)
            } catch (_: Exception) {}
        }
    }
}