package fr.coppernic.teste_idgpio;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Switch;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import fr.coppernic.sdk.hdk.cone.GpioPort;
import fr.coppernic.sdk.utils.core.CpcDefinitions;
import fr.coppernic.sdk.utils.core.CpcResult;
import fr.coppernic.sdk.utils.helpers.CpcPrefs;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "TestEidGpio";
    private static final int VID_USB_GPIO = 0x0403;
    private static final int PID_USB_GPIO = 0x6010;
    private static final String USB_ACTION_PERMISSION = "fr.coppernic.usb";
    private GpioPort gpioPort;
    private UsbManager usbManager;
    private D2xxManager d2xxManager;
    private FT_Device ftDevice;

    @BindView(R.id.swGpio3)
    Switch swGpio3;
    @BindView(R.id.swExternalEn)
    Switch swExternalEn;
    @BindView(R.id.swUsbEn)
    Switch swUsbEn;
    @BindView(R.id.swUsbId)
    Switch swUsbId;
    @BindView(R.id.swUsbGpio1)
    Switch swUsbGpio1;
    @BindView(R.id.swUsbGpio2)
    Switch swUsbGpio2;
    @BindView(R.id.swUsbGpio3)
    Switch swUsbGpio3;
    @BindView(R.id.swUsbGpio4)
    Switch swUsbGpio4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        GpioPort.GpioManager.get()
                .getGpioSingle(this)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<GpioPort>() {
                    @Override
                    public void accept(GpioPort g) throws Exception {
                        gpioPort = g;
                        enableAskeyGpios(true);
                    }
                });

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        enableAskeyGpios(false);
        enableUsbGpios(false);

        try {
            d2xxManager = D2xxManager.getInstance(this);
        } catch (D2xxManager.D2xxException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(usbReceiver, new IntentFilter(USB_ACTION_PERMISSION));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(usbReceiver);
        if (ftDevice != null) {
            if (ftDevice.isOpen()) {
                ftDevice.close();
            }
        }
    }

    @OnCheckedChanged(R.id.swGpio3)
    void onGpio3Changed(boolean state) {
        gpioPort.setPin3(state);
    }

    @OnCheckedChanged(R.id.swExternalEn)
    void onExternalEnChanged(boolean state) {
        gpioPort.setPinEn(state);
    }

    @OnCheckedChanged(R.id.swUsbEn)
    void onUsbEnChanged(boolean state) {
        gpioPort.setPinUsbEn(state);
    }

    @OnCheckedChanged(R.id.swUsbId)
    void onUsbIdChanged(boolean state) {
        gpioPort.setPinUsbIdSw(state);
    }

    @OnCheckedChanged(R.id.swUsbGpio1)
    void onUsbGpio1Changed(boolean state) {
        setGpio(this, CpcDefinitions.BYTE_GPIO_1, state);
    }

    @OnCheckedChanged(R.id.swUsbGpio2)
    void onUsbGpio2Changed(boolean state) {
        setGpio(this, CpcDefinitions.BYTE_GPIO_2, state);
    }

    @OnCheckedChanged(R.id.swUsbGpio3)
    void onUsbGpio3Changed(boolean state) {
        setGpio(this, CpcDefinitions.BYTE_GPIO_3, state);
    }

    @OnCheckedChanged(R.id.swUsbGpio4)
    void onUsbGpio4Changed(boolean state) {
        setGpio(this, CpcDefinitions.BYTE_GPIO_4, state);
    }

    @OnClick(R.id.btnPermission)
    void onPermissionRequested() {
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();

        if (!usbDevices.isEmpty()) {

            for (UsbDevice usbDevice:usbDevices.values()) {
                Log.d(TAG, String.format("VID: 0x%04X - PID: 0x%04X", usbDevice.getVendorId(), usbDevice.getProductId()));

                if (usbDevice.getVendorId() == VID_USB_GPIO && usbDevice.getProductId() == PID_USB_GPIO) {
                    usbManager.requestPermission(usbDevice, PendingIntent.getBroadcast(this, 0, new Intent(USB_ACTION_PERMISSION), 0));
                }
            }
        }
    }

    private BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (USB_ACTION_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null){
                            int nbDevices = d2xxManager.createDeviceInfoList(MainActivity.this);
                            Log.d(TAG, String.format("Nb FTDI devices available: %d", nbDevices));

                            ftDevice = d2xxManager.openByUsbDevice(MainActivity.this, device);

                            //call method to set up device communication
                            enableUsbGpios(true);
                        }
                    }
                    else {
                        Log.d(TAG, "permission denied for device " + device);
                    }
                }
            }

        }
    };

    private void enableUsbGpios(final boolean enable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                swUsbGpio1.setEnabled(enable);
                swUsbGpio2.setEnabled(enable);
                swUsbGpio3.setEnabled(enable);
                swUsbGpio4.setEnabled(enable);
            }
        });
    }

    private void enableAskeyGpios(final boolean enable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                swGpio3.setEnabled(enable);
                swUsbEn.setEnabled(enable);
                swUsbId.setEnabled(enable);
                swExternalEn.setEnabled(enable);
            }
        });
    }

    private CpcResult.RESULT setGpio(Context ctx, byte gpioNumber, boolean state) {

        CpcResult.RESULT res;
        if ((gpioNumber != CpcDefinitions.BYTE_GPIO_1)
                && (gpioNumber != CpcDefinitions.BYTE_GPIO_2)
                && (gpioNumber != CpcDefinitions.BYTE_GPIO_3)
                && (gpioNumber != CpcDefinitions.BYTE_GPIO_4)) {
            return CpcResult.RESULT.INVALID_PARAM;
        }

        short gpioSate = ftDevice.getBitMode();

        if (state) {
            gpioSate = (short) (gpioSate | gpioNumber);
        } else {
            gpioSate = (short) (gpioSate & ~gpioNumber);
        }

        if (ftDevice.setBitMode((byte) gpioSate, D2xxManager.FT_BITMODE_CBUS_BITBANG)) {
            final CpcPrefs p = new CpcPrefs(ctx);
            res = p.setPowerState(TAG, String.valueOf(gpioNumber), state ? 1 : 0);
        } else {
            res = CpcResult.RESULT.ERROR;
        }

        return res;
    }
}
