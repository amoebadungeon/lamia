package com.lamia.assistive.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.lamia.assistive.bluetooth.reports.FeatureReport
import com.lamia.assistive.bluetooth.reports.KeyboardReport

@SuppressLint("MissingPermission")
object BluetoothController : BluetoothHidDevice.Callback(), BluetoothProfile.ServiceListener {

    const val TAG = "BluetoothController"
    private const val PREFS_NAME = "lamia_bluetooth_prefs"
    private const val KEY_LAST_DEVICE_MAC = "last_device_mac"
    private const val KEY_LAST_DEVICE_NAME = "last_device_name"

    val featureReport = FeatureReport()
    var btHid: BluetoothHidDevice? = null
    var hostDevice: BluetoothDevice? = null

    private var appContext: Context? = null
    private var btAdapter: BluetoothAdapter? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isAutoReconnectScheduled = false

    var onConnectionStateListener: ((state: Int, device: BluetoothDevice?) -> Unit)? = null
    var onAppRegisteredListener: ((registered: Boolean) -> Unit)? = null

    private val sdpRecord by lazy {
        BluetoothHidDeviceAppSdpSettings(
            "LAMIA Controller",
            "LAMIA Assistive HID",
            "LAMIA Assistive",
            BluetoothHidDevice.SUBCLASS1_COMBO,
            DescriptorCollection.MOUSE_KEYBOARD_COMBO
        )
    }

    private val qosOut by lazy {
        BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
            800,
            9,
            0,
            11250,
            BluetoothHidDeviceAppQosSettings.MAX
        )
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        btAdapter = bluetoothManager.adapter

        if (btHid != null) {
            Log.d(TAG, "HID profile already connected in memory")
            mainHandler.post {
                onAppRegisteredListener?.invoke(true)
                hostDevice?.let { host ->
                    onConnectionStateListener?.invoke(BluetoothProfile.STATE_CONNECTED, host)
                } ?: run {
                    autoConnectLastDevice()
                }
            }
            return
        }

        try {
            val requested = btAdapter?.getProfileProxy(context.applicationContext, this, BluetoothProfile.HID_DEVICE)
            Log.d(TAG, "Requested HID profile proxy: $requested")
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting HID profile proxy", e)
        }
    }

    fun release(context: Context) {
        try {
            btHid?.unregisterApp()
            btAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, btHid)
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing HID profile proxy", e)
        }
        btHid = null
        hostDevice = null
    }

    fun getBondedDevices(): List<BluetoothDevice> {
        return try {
            btAdapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    fun saveLastConnectedDevice(device: BluetoothDevice) {
        val prefs = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
        val name = try { device.name ?: device.address } catch (e: Exception) { device.address }
        prefs.edit()
            .putString(KEY_LAST_DEVICE_MAC, device.address)
            .putString(KEY_LAST_DEVICE_NAME, name)
            .apply()
        Log.i(TAG, "Saved last connected host to prefs: $name (${device.address})")
    }

    fun getLastConnectedDeviceMac(): String? {
        val prefs = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return null
        return prefs.getString(KEY_LAST_DEVICE_MAC, null)
    }

    fun getLastConnectedDeviceName(): String? {
        val prefs = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return null
        return prefs.getString(KEY_LAST_DEVICE_NAME, null)
    }

    fun autoConnectLastDevice(): Boolean {
        val hid = btHid ?: return false
        val lastMac = getLastConnectedDeviceMac() ?: return false
        val bonded = getBondedDevices()
        val target = bonded.find { it.address.equals(lastMac, ignoreCase = true) }
        if (target != null) {
            Log.i(TAG, "Auto-connecting to previously paired host: ${target.name ?: target.address}")
            return hid.connect(target)
        }
        return false
    }

    fun connect(device: BluetoothDevice): Boolean {
        val hid = btHid ?: return false
        Log.d(TAG, "Initiating connect to: ${device.name ?: device.address}")
        saveLastConnectedDevice(device)
        return hid.connect(device)
    }

    fun makeDiscoverable(context: Context, durationSeconds: Int = 300) {
        try {
            val method = btAdapter?.javaClass?.getMethod(
                "setScanMode",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            )
            val result = method?.invoke(btAdapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, durationSeconds) as? Boolean
            if (result == true) {
                Log.d(TAG, "setScanMode CONNECTABLE_DISCOVERABLE reflection success")
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "Reflection setScanMode not supported: ${e.message}")
        }

        try {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, durationSeconds)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch ACTION_REQUEST_DISCOVERABLE", e)
        }
    }

    fun getKeyboardSender(): KeyboardSender? {
        val hid = btHid ?: return null
        val host = hostDevice ?: return null
        return KeyboardSender(hid, host)
    }

    /*****************************************************/
    /** BluetoothProfile.ServiceListener implementation **/
    /*****************************************************/

    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
        Log.i(TAG, "Bluetooth profile connected: $profile")
        if (profile != BluetoothProfile.HID_DEVICE) {
            Log.w(TAG, "Unexpected profile: $profile")
            return
        }

        val hid = proxy as? BluetoothHidDevice
        if (hid == null) {
            Log.e(TAG, "Proxy received is not BluetoothHidDevice")
            return
        }

        btHid = hid
        try {
            val regSuccess = hid.registerApp(sdpRecord, null, qosOut, { it.run() }, this)
            Log.i(TAG, "registerApp called: result=$regSuccess")
            if (!regSuccess) {
                Log.w(TAG, "registerApp returned false, trying unregister first...")
                try {
                    hid.unregisterApp()
                } catch (_: Exception) {}
                val retry = hid.registerApp(sdpRecord, null, qosOut, { it.run() }, this)
                Log.i(TAG, "registerApp retry: $retry")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registering HID app", e)
        }
    }

    override fun onServiceDisconnected(profile: Int) {
        Log.w(TAG, "Bluetooth profile disconnected: $profile")
        if (profile == BluetoothProfile.HID_DEVICE) {
            btHid = null
            hostDevice = null
            mainHandler.post {
                onAppRegisteredListener?.invoke(false)
            }
        }
    }

    /************************************************/
    /** BluetoothHidDevice.Callback implementation **/
    /************************************************/

    override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
        super.onAppStatusChanged(pluggedDevice, registered)
        Log.i(TAG, "onAppStatusChanged: registered=$registered, pluggedDevice=$pluggedDevice")
        mainHandler.post {
            onAppRegisteredListener?.invoke(registered)
        }

        if (registered) {
            try {
                val matchingDevices = btHid?.getDevicesMatchingConnectionStates(
                    intArrayOf(BluetoothProfile.STATE_CONNECTING, BluetoothProfile.STATE_CONNECTED)
                )
                if (!matchingDevices.isNullOrEmpty()) {
                    hostDevice = matchingDevices[0]
                    saveLastConnectedDevice(matchingDevices[0])
                    Log.i(TAG, "Found already connected host: ${hostDevice?.name}")
                    mainHandler.post {
                        onConnectionStateListener?.invoke(BluetoothProfile.STATE_CONNECTED, hostDevice)
                    }
                } else {
                    // Try auto-connecting to the previously saved host first
                    val reconnected = autoConnectLastDevice()
                    if (!reconnected) {
                        // Fallback: If only 1 bonded device exists, connect to it
                        val bonded = getBondedDevices()
                        if (bonded.size == 1) {
                            val only = bonded.first()
                            Log.i(TAG, "Single bonded device found, auto-connecting: ${only.name ?: only.address}")
                            connect(only)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error checking matching devices", e)
            }
        }
    }

    override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
        super.onConnectionStateChanged(device, state)
        val stateName = when (state) {
            BluetoothProfile.STATE_CONNECTED -> "CONNECTED"
            BluetoothProfile.STATE_CONNECTING -> "CONNECTING"
            BluetoothProfile.STATE_DISCONNECTING -> "DISCONNECTING"
            BluetoothProfile.STATE_DISCONNECTED -> "DISCONNECTED"
            else -> state.toString()
        }
        Log.i(TAG, "onConnectionStateChanged: device=${device?.name} (${device?.address}) -> $stateName")

        if (state == BluetoothProfile.STATE_CONNECTED) {
            hostDevice = device
            if (device != null) {
                saveLastConnectedDevice(device)
            }
            isAutoReconnectScheduled = false
        } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
            if (hostDevice == device) {
                hostDevice = null
            }

            // If disconnected, schedule a delayed auto-reconnect attempt
            if (!isAutoReconnectScheduled && btHid != null && getLastConnectedDeviceMac() != null) {
                isAutoReconnectScheduled = true
                mainHandler.postDelayed({
                    isAutoReconnectScheduled = false
                    if (hostDevice == null && btHid != null) {
                        Log.i(TAG, "Attempting background auto-reconnect after drop...")
                        autoConnectLastDevice()
                    }
                }, 2000L)
            }
        }

        mainHandler.post {
            onConnectionStateListener?.invoke(state, device)
        }
    }

    override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
        super.onGetReport(device, type, id, bufferSize)
        Log.i(TAG, "onGetReport: type=$type, id=$id, bufferSize=$bufferSize")

        try {
            if (type == BluetoothHidDevice.REPORT_TYPE_FEATURE) {
                featureReport.wheelResolutionMultiplier = true
                featureReport.acPanResolutionMultiplier = true
                val replied = btHid?.replyReport(device, type, FeatureReport.ID, featureReport.bytes)
                Log.i(TAG, "Replied to FEATURE report: $replied")
            } else if (type == BluetoothHidDevice.REPORT_TYPE_INPUT) {
                // Reply with correct size based on report ID
                val reportSize = when (id.toInt()) {
                    KeyboardReport.ID -> 3   // Keyboard: modifier + reserved + key
                    4 -> 7                   // Mouse: buttons + x(2) + y(2) + wheel + pan
                    else -> bufferSize.coerceAtLeast(1)
                }
                val emptyReport = ByteArray(reportSize) { 0 }
                val replied = btHid?.replyReport(device, type, id, emptyReport)
                Log.i(TAG, "Replied to INPUT report (id=$id, size=$reportSize): $replied")
            } else {
                val replied = btHid?.replyReport(device, type, id, ByteArray(bufferSize.coerceAtLeast(1)) { 0 })
                Log.i(TAG, "Replied generic report: $replied")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onGetReport", e)
        }
    }

    override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
        super.onSetReport(device, type, id, data)
        Log.i(TAG, "onSetReport: type=$type, id=$id, data=${data?.contentToString()}")
        try {
            btHid?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onSetReport", e)
        }
    }

    override fun onSetProtocol(device: BluetoothDevice?, protocol: Byte) {
        super.onSetProtocol(device, protocol)
        Log.i(TAG, "onSetProtocol: protocol=$protocol")
        try {
            btHid?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onSetProtocol", e)
        }
    }

    override fun onVirtualCableUnplug(device: BluetoothDevice?) {
        super.onVirtualCableUnplug(device)
        Log.i(TAG, "onVirtualCableUnplug: device=$device")
        hostDevice = null
        mainHandler.post {
            onConnectionStateListener?.invoke(BluetoothProfile.STATE_DISCONNECTED, device)
        }
    }
}
