package com.omouren.fpvforall;

import android.hardware.usb.UsbDevice;

public interface UsbDeviceListener {
    void usbDeviceApproved(UsbDevice device);
}
