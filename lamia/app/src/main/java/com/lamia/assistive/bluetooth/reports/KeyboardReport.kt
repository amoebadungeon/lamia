package com.lamia.assistive.bluetooth.reports

import kotlin.experimental.and
import kotlin.experimental.or

class KeyboardReport(
    val bytes: ByteArray = ByteArray(3) { 0 }
) {
    var leftControl: Boolean
        get() = bytes[0] and 0b1 != 0.toByte()
        set(value) {
            bytes[0] = if (value)
                bytes[0] or 0b1
            else
                bytes[0] and 0b11111110.toByte()
        }

    var leftShift: Boolean
        get() = bytes[0] and 0b10 != 0.toByte()
        set(value) {
            bytes[0] = if (value)
                bytes[0] or 0b10
            else
                bytes[0] and 0b11111101.toByte()
        }

    var leftAlt: Boolean
        get() = bytes[0] and 0b100 != 0.toByte()
        set(value) {
            bytes[0] = if (value)
                bytes[0] or 0b100
            else
                bytes[0] and 0b11111011.toByte()
        }

    var leftGui: Boolean
        get() = bytes[0] and 0b1000 != 0.toByte()
        set(value) {
            bytes[0] = if (value)
                bytes[0] or 0b1000
            else
                bytes[0] and 0b11110111.toByte()
        }

    var rightControl: Boolean
        get() = bytes[0] and 0b10000 != 0.toByte()
        set(value) {
            bytes[0] = if (value)
                bytes[0] or 0b10000
            else
                bytes[0] and 0b11101111.toByte()
        }

    var rightShift: Boolean
        get() = bytes[0] and 0b100000 != 0.toByte()
        set(value) {
            bytes[0] = if (value)
                bytes[0] or 0b100000
            else
                bytes[0] and 0b11011111.toByte()
        }

    var rightAlt: Boolean
        get() = bytes[0] and 0b1000000 != 0.toByte()
        set(value) {
            bytes[0] = if (value)
                bytes[0] or 0b1000000
            else
                bytes[0] and 0b10111111.toByte()
        }

    var rightGui: Boolean
        get() = bytes[0] and 0b10000000.toByte() != 0.toByte()
        set(value) {
            bytes[0] = if (value)
                bytes[0] or 0b10000000.toByte()
            else
                bytes[0] and 0b01111111
        }

    var key1: Byte
        get() = bytes[2]
        set(value) {
            bytes[2] = value
        }

    fun reset() = bytes.fill(0)

    companion object {
        const val ID = 8

        // Standard USB HID Usage codes for D-Pad navigation
        const val KEY_UP: Byte = 0x52.toByte()
        const val KEY_DOWN: Byte = 0x51.toByte()
        const val KEY_LEFT: Byte = 0x50.toByte()
        const val KEY_RIGHT: Byte = 0x4F.toByte()
        const val KEY_ENTER: Byte = 0x28.toByte()
        const val KEY_TAB: Byte = 0x2B.toByte()
        const val KEY_ESCAPE: Byte = 0x29.toByte()
        const val KEY_BACKSPACE: Byte = 0x2A.toByte()
        const val KEY_SPACE: Byte = 0x2C.toByte()
        const val KEY_HOME: Byte = 0x4A.toByte()
    }
}
