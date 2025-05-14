package com.example.cipherlock;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.IBinder;

public class UsbAuthService extends Service {
    private USBReceiver usbReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        // Registrar receptor para detectar conexiones USB incluso cuando la app está cerrada
        usbReceiver = new USBReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        registerReceiver(usbReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Servicio reiniciable si es terminado
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(usbReceiver);
    }
}