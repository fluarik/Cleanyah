package com.myself.cleanyah.card_emu


import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import java.util.*


/**
 * Created by mhamdaoui on 2017-10-27.
 */
class HostCardEmulatorService: HostApduService() {

    /*
    companion object {
        val TAG = "Host Card Emulator"
        val STATUS_SUCCESS = "9000"
        val STATUS_FAILED = "6F00"
        val CLA_NOT_SUPPORTED = "6E00"
        val INS_NOT_SUPPORTED = "6D00"
        val AID = "A0000002471001"
        val SELECT_INS = "A4"
        val DEFAULT_CLA = "00"
        val MIN_APDU_LENGTH = 12
    }

    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "Deactivated: " + reason)
    }

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null) {
            return Utils.hexStringToByteArray(STATUS_FAILED)
        }

        val hexCommandApdu = Utils.toHex(commandApdu)
        if (hexCommandApdu.length < MIN_APDU_LENGTH) {
            return Utils.hexStringToByteArray(STATUS_FAILED)
        }

        if (hexCommandApdu.substring(0, 2) != DEFAULT_CLA) {
            return Utils.hexStringToByteArray(CLA_NOT_SUPPORTED)
        }

        if (hexCommandApdu.substring(2, 4) != SELECT_INS) {
            return Utils.hexStringToByteArray(INS_NOT_SUPPORTED)
        }

        if (hexCommandApdu.substring(10, 24) == AID)  {
            return Utils.hexStringToByteArray(STATUS_SUCCESS)
        } else {
            return Utils.hexStringToByteArray(STATUS_FAILED)
        }
    }

     */
    /*
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

     */

    companion object {
        private val COMMAND_SELECT = byteArrayOf(
            0x00.toByte(),
            0xA4.toByte(),
            0x04.toByte(),
            0x00.toByte(),
            0x07.toByte(),
            0xF1.toByte(),
            0x14.toByte(),
            0x51.toByte(),
            0x41.toByte(),
            0x91.toByte(),
            0x98.toByte(),
            0x10.toByte()
        )

        private val COMMAND_VIBRATE =
            byteArrayOf(0x00.toByte(), 0x11.toByte(), 0x45.toByte(), 0x14.toByte())

        private val RESPONSE_OK = byteArrayOf(0x90.toByte(), 0x00.toByte())
    }
    override fun processCommandApdu(command: ByteArray?, extras: Bundle?): ByteArray {
        if (Arrays.equals(command, COMMAND_SELECT)) {
            Log.i("NFCTest", "AID Selected")
            Toast.makeText(this, "AID Selected",Toast.LENGTH_LONG).show()
            //MapsActivity.showMessage("NFC読み取り")
        } else if (Arrays.equals(command, COMMAND_VIBRATE)) {
            Log.i("NFCTest", "Vibrate")
            Toast.makeText(this, "Vibrate",Toast.LENGTH_LONG).show()
        }
        return RESPONSE_OK
    }

    override fun onDeactivated(p0: Int) {    }
}