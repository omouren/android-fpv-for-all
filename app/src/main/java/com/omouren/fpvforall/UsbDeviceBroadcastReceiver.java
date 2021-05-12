package com.omouren.fpvforall;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

public class UsbDeviceBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "FPV4ALL";
    private static final String ACTION_USB_PERMISSION = "com.fpvforall.USB_PERMISSION";
    private final UsbDeviceListener listener;

    public UsbDeviceBroadcastReceiver(UsbDeviceListener listener ){
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "UsbDeviceBroadcastReceiver");
        String action = intent.getAction();
        if (ACTION_USB_PERMISSION.equals(action)) {
            synchronized (this) {
                UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if(device != null){
                        Log.d(TAG, "UsbDeviceBroadcastReceiver : approved");
                        listener.usbDeviceApproved(device);
                    }
                }
            }
        }
    }
}
