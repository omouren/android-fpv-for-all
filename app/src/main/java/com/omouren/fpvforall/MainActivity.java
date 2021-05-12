package com.omouren.fpvforall;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;


import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements UsbDeviceListener {
    private static final String TAG = "FPV4ALL";
    private static final String MAGIC_BYTES = "524d5654";
    private static final String ACTION_USB_PERMISSION = "com.fpvforall.USB_PERMISSION";
    private static final int VENDOR_ID = 11427;
    private static final int PRODUCT_ID = 31;
    private PendingIntent permissionIntent;
    private UsbDeviceBroadcastReceiver usbDeviceBroadcastReceiver;
    private UsbManager usbManager;
    private UsbDevice usbDevice;
    private UsbDeviceConnection usbDeviceConnection;
    private UsbInterface usbInterface;
    private boolean runUsbDataReceiver = true;
    private SurfaceView surfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Hide top bar and status bar
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();


        this.surfaceView = findViewById(R.id.surfaceView);

        this.usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        this.permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        this.usbDeviceBroadcastReceiver = new UsbDeviceBroadcastReceiver(this);

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(this.usbDeviceBroadcastReceiver, filter);

        this.init();
    }


    @Override
    public void usbDeviceApproved(UsbDevice device) {
        this.usbDevice = device;
        this.handleUsbDevice();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.runUsbDataReceiver = false;

        if (this.usbInterface != null) {
            this.usbDeviceConnection.releaseInterface(this.usbInterface);
        }

        if (this.usbDeviceConnection != null) {
            this.usbDeviceConnection.close();
        }

        unregisterReceiver(this.usbDeviceBroadcastReceiver);
    }

    public void init() {
        if (searchDevice()) {
            handleUsbDevice();
        }
    }

    private boolean searchDevice() {
        HashMap<String, UsbDevice> deviceList = this.usbManager.getDeviceList();
        if (deviceList.size() <= 0) {
            this.usbDevice = null;
            return false;
        }

        for(UsbDevice device : deviceList.values()) {
            if (device.getVendorId() == VENDOR_ID && device.getProductId() == PRODUCT_ID) {
                if (this.usbManager.hasPermission(device)) {
                    this.usbDevice = device;
                    return true;
                }

                this.usbManager.requestPermission(device, permissionIntent);
            }
        }

        return false;
    }

    private void handleUsbDevice() {
        if ((this.usbDeviceConnection = this.usbManager.openDevice(this.usbDevice)) != null) {
            if (this.usbDevice.getInterfaceCount() <= 0) {
                Log.d(TAG, "Could't find usb interface !!");
                return;
            }

            this.usbInterface = this.usbDevice.getInterface(3);
            this.usbDeviceConnection.claimInterface(this.usbInterface, true);

            new Thread(() -> {
                Log.d(TAG, "Send magic bytes");

                byte[] buffer = hexStringToByteArray(MAGIC_BYTES);
                int result = this.usbDeviceConnection.bulkTransfer(this.usbInterface.getEndpoint(0), buffer, buffer.length, 100);

                Log.d(TAG, result >= 0 ? "Magic bytes sent successfully" : "Error sending magic bytes (maybe already sent)");
            }).start();

            new Thread(() -> {
                while(this.runUsbDataReceiver) {
                    byte[] buffer = new byte[this.usbInterface.getEndpoint(1).getMaxPacketSize()];
                    int result = this.usbDeviceConnection.bulkTransfer(this.usbInterface.getEndpoint(1), buffer, buffer.length, 100);


                    Log.d(TAG, result >= 0 ? "Data stream received" : "Data stream empty");

                    if (result >= 0) {
                        Log.d(TAG, String.valueOf(buffer.length));
                    }
                }
            }).start();
        }
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}