package com.widecode.kontrol

import android.app.ProgressDialog
import android.bluetooth.*
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_control.*
import org.jetbrains.anko.toast
import java.io.IOException

class ControlActivity : AppCompatActivity() {

    companion object {
        lateinit var progressDialog: ProgressDialog
        lateinit var btAdapter: BluetoothAdapter
        var isConnected: Boolean = false
        lateinit var deviceAddress: String
        var btHidDevice: BluetoothHidDevice? = null
        lateinit var currentDevice: BluetoothDevice
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control)
        deviceAddress = intent.getStringExtra(MainActivity.EXTRA_ADDRESS).toString()
        ConnectToDevice(this).execute()

        control_led_on.setOnClickListener { sendCommand("a") }
        control_led_off.setOnClickListener { sendCommand("b") }
        control_disconnect.setOnClickListener { disconnect() }
    }

    private fun sendCommand(input: String) {
        try {
            val bytes: ByteArray = ByteArray(3) {0}
            var word: ArrayList<Int> = ArrayList()
            word.add(10)
            word.add(21)
            word.add(8)
            word.add(10)
            word.add(18)

        val i =1
            for (letter in word) {
            bytes[2] = letter.toByte()
            if (btHidDevice!!.sendReport(currentDevice, i, bytes)) {
                Log.i("COM", "Sent bytes to " + currentDevice.name + " " + i + " " + bytes.toString())
            } else {
                Log.e("COM", "Error sending bytes")
            }
                }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun disconnect() {
        try {
            ConnectToDevice(this).cancel(true)
            btHidDevice?.disconnect(currentDevice)
            isConnected = false
        } catch (e: IOException) {
            e.printStackTrace()
        }

        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        btAdapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, btHidDevice);
    }

    private class BtCallback(c: Context): BluetoothHidDevice.Callback() {

        private val context: Context = c


        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            super.onConnectionStateChanged(device, state)
            if (device == currentDevice) {
                Thread(Runnable {
                    if (state == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.i("hid", context.getText(R.string.status_disconnected).toString())
                    } else if (state == BluetoothProfile.STATE_CONNECTING) {
                        Log.i("hid", context.getText(R.string.status_connecting).toString())

//                        context.toast(R.string.status_connecting)
                    } else if (state == BluetoothProfile.STATE_CONNECTED) {
                        Log.i("hid", context.getText(R.string.status_connected).toString())

//                        context.toast(R.string.status_connected)
                    } else if (state == BluetoothProfile.STATE_DISCONNECTING) {
                        Log.i("hid", context.getText(R.string.status_disconnecting).toString())

//                        context.toast(R.string.status_disconnecting)
                    }
                }).start()
            }
        }

    }

    private class ConnectToDevice(c: Context) : AsyncTask<Void, Void, String>() {

        private var connectSuccess: Boolean = true
        private val context: Context = c

        private val profileListener = object : BluetoothProfile.ServiceListener {

            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile != BluetoothProfile.HID_DEVICE) {
                    Log.i("hid", "WTF? $profile")
                    return
                }

                btHidDevice = proxy as? BluetoothHidDevice
                if (btHidDevice == null) {
                    Log.wtf("hid", "WTF? Proxy received but it's not BluetoothHidDevice")

                    return
                }
                btHidDevice?.registerApp(sdpRecord, null, qosOut, {it.run()}, BtCallback(context))//--
//                btAdapter.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, 300000)
            }

            override fun onServiceDisconnected(profile: Int) {
                Log.e("hid", "onServiceDisconnected::Service disconnected!")
                if (profile == BluetoothProfile.HID_DEVICE)
                    btHidDevice = null
            }
        }


        private val sdpRecord by lazy {
            BluetoothHidDeviceAppSdpSettings(
                "Pixel HID1",
                "Mobile BController",
                "bla",
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

        override fun onPreExecute() {
            super.onPreExecute()
            progressDialog = ProgressDialog.show(context, "Connecting...", "please wait")
        }

        override fun doInBackground(vararg params: Void?): String? {

            try {
                if (btHidDevice == null || !isConnected) {
                    btAdapter = BluetoothAdapter.getDefaultAdapter()
                    btAdapter.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE)
                    currentDevice = btAdapter.getRemoteDevice(deviceAddress)
                    btAdapter.cancelDiscovery()
                    if (btHidDevice != null) {
                        if (false == btHidDevice!!.connect(currentDevice)) {
                            throw IOException()
                        }
                    }
                }
            } catch (e: IOException) {
                connectSuccess = false
                e.printStackTrace()
            }

            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (!connectSuccess) {
                Log.e("data", "Couldn't connect")
                context.toast("Couldn't connect")
            } else {
                isConnected = true
            }
            progressDialog.dismiss()
        }

    }
}
