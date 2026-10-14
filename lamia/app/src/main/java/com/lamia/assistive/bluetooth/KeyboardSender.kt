package com.lamia.assistive.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.lamia.assistive.bluetooth.reports.KeyboardReport

class KeyboardSender(
    val hidDevice: BluetoothHidDevice,
    val host: BluetoothDevice
) {
    private val handler = Handler(Looper.getMainLooper())
    // Shared empty report for key release — safe because it's always all-zeros
    private val releaseReport = KeyboardReport()

    fun sendKeyPressAndRelease(
        hidKeyCode: Byte,
        alt: Boolean = false,
        ctrl: Boolean = false,
        shift: Boolean = false,
        gui: Boolean = false,
        callback: ((Boolean) -> Unit)? = null
    ) {
        // Create a dedicated report for this keypress to prevent race conditions
        val report = KeyboardReport()
        if (alt) report.leftAlt = true
        if (ctrl) report.leftControl = true
        if (shift) report.leftShift = true
        if (gui) report.leftGui = true
        report.key1 = hidKeyCode

        val pressSent = hidDevice.sendReport(host, KeyboardReport.ID, report.bytes)
        Log.d(TAG, "Sent key press 0x${Integer.toHexString(hidKeyCode.toInt() and 0xFF)} (alt=$alt), success=$pressSent")

        // Release after 50ms using a fresh zeroed report
        handler.postDelayed({
            val releaseSent = hidDevice.sendReport(host, KeyboardReport.ID, releaseReport.bytes)
            Log.d(TAG, "Sent key release, success=$releaseSent")
            callback?.invoke(pressSent && releaseSent)
        }, 50)
    }

    /**
     * Smart Back for Web Browsers & Streaming Services (Unifi TV, YouTube, Netflix):
     * 1. Sends ESC (to exit fullscreen / dismiss player overlays)
     * 2. Then sends Alt + Left Arrow after 60ms (browser back to catalog)
     */
    fun sendSmartBack(callback: ((Boolean) -> Unit)? = null) {
        // Step 1: Send ESC
        val escReport = KeyboardReport()
        escReport.key1 = KeyboardReport.KEY_ESCAPE
        hidDevice.sendReport(host, KeyboardReport.ID, escReport.bytes)

        handler.postDelayed({
            hidDevice.sendReport(host, KeyboardReport.ID, releaseReport.bytes)

            // Step 2: Send Alt + Left Arrow after 60ms
            handler.postDelayed({
                val altLeftReport = KeyboardReport()
                altLeftReport.leftAlt = true
                altLeftReport.key1 = KeyboardReport.KEY_LEFT
                val pressSent = hidDevice.sendReport(host, KeyboardReport.ID, altLeftReport.bytes)

                handler.postDelayed({
                    val releaseSent = hidDevice.sendReport(host, KeyboardReport.ID, releaseReport.bytes)
                    callback?.invoke(pressSent && releaseSent)
                }, 50)
            }, 60)
        }, 50)
    }

    fun sendNullKeys() {
        hidDevice.sendReport(host, KeyboardReport.ID, releaseReport.bytes)
    }

    companion object {
        const val TAG = "KeyboardSender"
    }
}
