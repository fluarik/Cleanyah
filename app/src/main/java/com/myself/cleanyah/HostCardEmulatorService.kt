package com.myself.cleanyah


import android.nfc.cardemulation.HostApduService
import android.os.Bundle


class HostCardEmulatorService: HostApduService() {
    companion object {
        private const val SAMPLE_LOYALTY_CARD_AID = "F222222222"
        private val SELECT_OK_SW = hexStringToByteArray("9000")
        private val UNKNOWN_CMD_SW = hexStringToByteArray("0000")
        private val SELECT_APDU = buildSelectApdu(SAMPLE_LOYALTY_CARD_AID)
        private fun hexStringToByteArray(s: String): ByteArray {
            val len = s.length
            if (len % 2 == 1) {
                throw IllegalArgumentException("Hex string must have even number of characters")
            }
            val data = ByteArray(len / 2)
            for (i in 0 until len step 2) {
                data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            }
            return data
        }
        private fun buildSelectApdu(aid: String): ByteArray {
            // Format: [CLASS | INSTRUCTION | PARAMETER 1 | PARAMETER 2 | LENGTH | DATA]
            return hexStringToByteArray("00A40400" + String.format("%02X", aid.length / 2) + aid)
        }
    }
    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        return if (SELECT_APDU.contentEquals(commandApdu)) SELECT_OK_SW else UNKNOWN_CMD_SW
    }
    override fun onDeactivated(reason: Int) { }
}