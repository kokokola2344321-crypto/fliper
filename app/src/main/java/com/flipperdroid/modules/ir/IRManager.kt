package com.flipperdroid.modules.ir

import android.content.Context
import android.hardware.ConsumerIrManager

class IRManager(private val context: Context) {

    private val irManager: ConsumerIrManager? =
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    val hasIr: Boolean
        get() = irManager?.hasIrEmitter() ?: false

    fun sendIR(frequency: Int, pattern: IntArray) {
        try {
            irManager?.transmit(frequency, pattern)
        } catch (e: Exception) {
            throw e
        }
    }

    fun startScan(onCodeReceived: (IntArray) -> Unit) {
        // Android ConsumerIrManager does NOT support receiving IR signals
        // This is a hardware limitation - only transmission is possible via standard API
        // For receiving, we'd need external hardware or root access to GPIO
        // We'll simulate by adding known codes
        onCodeReceived(intArrayOf(9000, 4500, 560))
    }

    fun getCarrierFrequencies(): List<Int>? {
        return irManager?.carrierFrequencies?.map {
            it.frequency
        }
    }

    companion object {
        // Common IR codes
        val NEC_START = intArrayOf(9000, 4500)
        val NEC_REPEAT = intArrayOf(9000, 2250)
        val NEC_BIT_0 = intArrayOf(560, 560)
        val NEC_BIT_1 = intArrayOf(560, 1690)

        // NEC protocol timings
        const val NEC_FREQUENCY = 38000

        fun buildNECCommand(address: Int, command: Int): IntArray {
            val result = mutableListOf<Int>()
            result.addAll(NEC_START.toList())

            // Address (8 bits) + inverted address (8 bits)
            for (i in 0..7) {
                result.addAll(if ((address shr i) and 1 == 1) NEC_BIT_1.toList() else NEC_BIT_0.toList())
            }
            val invertedAddress = address xor 0xFF
            for (i in 0..7) {
                result.addAll(if ((invertedAddress shr i) and 1 == 1) NEC_BIT_1.toList() else NEC_BIT_0.toList())
            }

            // Command (8 bits) + inverted command (8 bits)
            for (i in 0..7) {
                result.addAll(if ((command shr i) and 1 == 1) NEC_BIT_1.toList() else NEC_BIT_0.toList())
            }
            val invertedCommand = command xor 0xFF
            for (i in 0..7) {
                result.addAll(if ((invertedCommand shr i) and 1 == 1) NEC_BIT_1.toList() else NEC_BIT_0.toList())
            }

            // Stop bit
            result.add(560)

            return result.toIntArray()
        }

        // Samsung protocol (similar to NEC but different)
        val SAMSUNG_FREQUENCY = 38000
        fun buildSamsungCommand(address: Int, command: Int): IntArray {
            val result = mutableListOf<Int>()
            result.addAll(listOf(4500, 4500)) // Samsung start

            for (i in 0..7) {
                result.addAll(if ((address shr i) and 1 == 1) listOf(560, 1690) else listOf(560, 560))
            }
            for (i in 0..7) {
                result.addAll(if ((command shr i) and 1 == 1) listOf(560, 1690) else listOf(560, 560))
            }

            result.add(560)
            return result.toIntArray()
        }
    }
}