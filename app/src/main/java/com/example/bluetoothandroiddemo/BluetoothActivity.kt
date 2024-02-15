package com.example.bluetoothandroiddemo

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.util.*


class BluetoothActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var listView: ListView
    private lateinit var listView_log: ListView
    private lateinit var scan:Button
    private lateinit var devicesListAdapter: ArrayAdapter<String>
    private lateinit var logListAdapter: ArrayAdapter<String>
    private lateinit var pairedDevices: Set<BluetoothDevice>
    var uuid1="";
    var uuid: UUID? = null
    lateinit var socket: BluetoothSocket
    private val REQUEST_ENABLE_BLUETOOTH = 1


    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        if (ActivityCompat.checkSelfPermission(
                                this@BluetoothActivity,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return
                        }
                        AcceptThread()
                        val device: BluetoothDevice? =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        val deviceName = device!!.name
                        val deviceHardwareAddress = device.address // MAC address
                        if (device.name != null){
                            devicesListAdapter.add(device.name)
                            Log.d("pairedDevices","Discovered devicename====="+device.name)
                            Log.d("pairedDevices","Discovered deviceHardwareAddress====="+deviceHardwareAddress)

                        }
                        else{
                            Log.d("pairedDevices","Discovered devicename====="+device.name)
                        }


                    }
                }
            }
        }
    }


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth)
        checkPermissions()

        //registering reciver
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(bluetoothReceiver, filter)
        scan=findViewById(R.id.scan)
        listView = findViewById(R.id.listView)
        listView_log = findViewById(R.id.listView_log)
        logListAdapter= ArrayAdapter(this,android.R.layout.simple_list_item_1)
        listView_log.adapter=logListAdapter
        devicesListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        listView.adapter = devicesListAdapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            return
        }

//
//        val myList = MutableList(20) { "Item $it" } // Creating a MutableList with 20 items
//
//        println("Initial list:")
//        println(myList)
//
//        // Remove the last item from the list
//        val removedItem = myList.removeAt(myList.size - 1)
//        println("Removed item: $removedItem")
//
//        // Add a new item at the beginning of the list
//        val newItem = "New Item at 0th Position"
//        myList.add(0, newItem)
//
//        println("\nUpdated list:")
//        println(myList)


        val pairedDevices1: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
        pairedDevices1?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address
            logListAdapter.add("past Connection deviceHardwareAddress=====+ $deviceHardwareAddress")
            Log.d("pairedDevices","past Connection deviceHardwareAddress====="+deviceHardwareAddress)


        }



        // Request necessary permissions
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN),
            1
        )

        // Enable Bluetooth if it's not enabled
        if (!bluetoothAdapter.isEnabled) {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH)
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val deviceName = devicesListAdapter.getItem(position)
            val device: BluetoothDevice? = pairedDevices.find { it.name == deviceName }
            device?.let { connectToDevice(it)
                Log.d("pairedDevices",it.name.toString())
            }
        }

        pairedDevices = bluetoothAdapter.bondedDevices
        pairedDevices.forEach { device ->
            devicesListAdapter.add(device.name)
        }

        scan.setOnClickListener {

            devicesListAdapter.clear()
            val requestCode = 1;
//            val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
//                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
//            }
//            startActivityForResult(discoverableIntent, requestCode)
            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }
            bluetoothAdapter.startDiscovery()
        }


    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
         uuid1 = UUID.randomUUID().toString()
        uuid = UUID.fromString(uuid1)
        socket= device.createRfcommSocketToServiceRecord(uuid)
        try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            socket.connect()
            val deviceAddress = device.address
            readData(socket)
            Log.d("pairedDevices","Connection successful deviceAddress====="+deviceAddress)
            // Connection successful, handle further operations here
        } catch (e: IOException) {
            e.printStackTrace()
            try {
                socket.close()
            } catch (closeException: IOException) {
                closeException.printStackTrace()
            }
        }
    }
    private fun readData(socket: BluetoothSocket) {
        val inputStream = socket.inputStream
        val buffer = ByteArray(1024)
        var bytes: Int

        while (true) {
            try {
                bytes = inputStream.read(buffer)
                val data = buffer.copyOfRange(0, bytes)
                val receivedString = String(data)
                // Process the received data here
                runOnUiThread {
                    Toast.makeText(
                        this@BluetoothActivity,
                        "Received: $receivedString",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@BluetoothActivity, "Connection lost", Toast.LENGTH_SHORT)
                        .show()
                }
                break
            }
        }
    }
    private fun checkPermissions() {
        val PERMISSIONS_STORAGE = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_PRIVILEGED
        )
        val PERMISSIONS_LOCATION = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_PRIVILEGED
        )
        val permission1 =
            ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val permission2 =
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
        if (permission1 != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                this,
                PERMISSIONS_STORAGE,
                1
            )
        } else if (permission2 != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                PERMISSIONS_LOCATION,
                1
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)
    }
    @SuppressLint("MissingPermission")
    private inner class AcceptThread : Thread() {

        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord("NAME", uuid)
        }

        override fun run() {
            // Keep listening until exception occurs or a socket is returned.
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    mmServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "Socket's accept() method failed", e)
                    shouldLoop = false
                    null
                }
                socket?.also {
//                    manageMyConnectedSocket(it)
                    mmServerSocket?.close()
                    shouldLoop = false
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        fun cancel() {
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }


}

