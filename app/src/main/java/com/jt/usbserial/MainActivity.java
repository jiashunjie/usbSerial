package com.jt.usbserial;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        register();
    }

    private Button btn_search;
    private TextView tv_connect_state;
    private TextView tv_usb;
    private TextView tv_receive;
    private ListView lv_device;
    private List<String> adapterData;
    private ArrayAdapter adapter;
    private List<UsbSerialPort> usbDevicesList;
    private UsbSerialPort mUsbSerialPort;

    private void initView() {
        adapterData = new ArrayList<>();
        usbDevicesList = new ArrayList<>();
        btn_search = findViewById(R.id.btn_search);
        btn_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                search();
            }
        });
        tv_usb = findViewById(R.id.tv_usb);
        tv_receive = findViewById(R.id.tv_receive);
        tv_connect_state = findViewById(R.id.tv_connect_state);
        lv_device = findViewById(R.id.lv_device);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, adapterData);
        lv_device.setAdapter(adapter);
        lv_device.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mUsbSerialPort = usbDevicesList.get(position);
                RequestNormalPermission(usbManager, mUsbSerialPort.getDriver().getDevice());

            }
        });
    }

    private String USB_PERMISSION = "com.jt.usbserial.usb.permission";
    private PendingIntent mPrtPermissionIntent; //获取外设权限的意图

    /**
     * 动态注册usb广播，拔插动作，注册动作
     */
    private void register() {
        //注册在此service下的receiver的监听的action
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        intentFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        intentFilter.addAction(USB_PERMISSION);
        registerReceiver(usbReceiver, intentFilter);//注册receiver

        //通知监听外设权限注册状态
        //PendingIntent：连接外设的intent
        //ask permission
        mPrtPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(USB_PERMISSION), 0);
    }

    private BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            String action = intent.getAction();

            // USB注册动作
            if (USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) != null) {
                            // usbDev = (UsbDevice)
                            // intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                            // after request dev permission and granted
                            // pemission , connect printer by usb
                            Log.i(TAG, "after request dev permission and granted pemission , connect printer by usb.");
                            getUsbInfo(mUsbSerialPort.getDriver().getDevice());
                        } else {
                            Log.e(TAG, "usb device suddenly disappera.");
                            Toast.makeText(MainActivity.this, "USB外设意外消失。", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "usb permission granted fail.");
                        Toast.makeText(MainActivity.this, "USB权限注册失败。", Toast.LENGTH_SHORT).show();
                    }
                }
            } else if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action) || UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                Log.i(TAG, "USB 插入...");
                Toast.makeText(MainActivity.this, "USB 插入...", Toast.LENGTH_SHORT).show();
                search();
            } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action) || UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Log.i(TAG, "USB 拔出...");
                Toast.makeText(MainActivity.this, "USB 拔出...", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private UsbManager usbManager;


    /**
     * 查询设备
     */
    private void search() {
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        if (drivers.size() <= 0) {
            //To use your own set of rules, create and use a custom prober:
            // https://github.com/mik3y/usb-serial-for-android
            ProbeTable customTable = new ProbeTable();
            customTable.addProduct(1155, 22336, CdcAcmSerialDriver.class);
            UsbSerialProber prober = new UsbSerialProber(customTable);
            drivers = prober.findAllDrivers(usbManager);
        }
        //try get enable printer dev
        if (drivers.size() > 0) {
            usbDevicesList.clear();
            adapterData.clear();
            for (UsbSerialDriver driver : drivers) {
                usbDevicesList.addAll(driver.getPorts());
            }
            for (UsbSerialPort usbSerialPort : usbDevicesList) {
                UsbDevice usbDevice = usbSerialPort.getDriver().getDevice();
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("VID:%04X  PID:%04X  ManuFN:%s  PN:%s ",
                        usbDevice.getVendorId(),
                        usbDevice.getProductId(),
                        usbDevice.getManufacturerName(),
                        usbDevice.getProductName()
                ));
                adapterData.add(sb.toString());
            }
            adapter.notifyDataSetChanged();
        } else {
            Toast.makeText(this, "not find device", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "not find device.");
            return;
        }
    }

    /**
     * 请求权限：一般来说有弹框
     */
    public void RequestNormalPermission(UsbManager usbManager, UsbDevice device) {
        if (!usbManager.hasPermission(device)) {
            // ask has dev permission,pop dialog
            Log.d(TAG, "printer dev has no permission,try request it.");
            Toast.makeText(this, "printer dev has no permission,try request it.", Toast.LENGTH_SHORT).show();
            usbManager.requestPermission(device, mPrtPermissionIntent);// will recall mReceiver
        } else {
            // when dev has granted permission , connect printer by usb
            Log.d(TAG, "printer dev has granted permission,connect usb.");
            Toast.makeText(this, "USB权限注册成功。", Toast.LENGTH_SHORT).show();
            getUsbInfo(mUsbSerialPort.getDriver().getDevice());
        }
    }

    /**
     * 获得授权USB的基本信息
     * 1、USB接口，一般是第一个
     * 2、USB设备的输入输出端
     */
    private void getUsbInfo(UsbDevice usbDevice) {
        StringBuilder sb = new StringBuilder();
        if (Build.VERSION.SDK_INT >= 23) {
            sb.append(String.format("VID:%04X  PID:%04X  ManuFN:%s  PN:%s V:%s",
                    usbDevice.getVendorId(),
                    usbDevice.getProductId(),
                    usbDevice.getManufacturerName(),
                    usbDevice.getProductName(),
                    usbDevice.getVersion()
            ));
        } else if (Build.VERSION.SDK_INT >= 21) {
            sb.append(String.format("VID:%04X  PID:%04X  ManuFN:%s  PN:%s",
                    usbDevice.getVendorId(),
                    usbDevice.getProductId(),
                    usbDevice.getManufacturerName(),
                    usbDevice.getProductName()
            ));
        } else {
            sb.append(String.format("VID:%04X  PID:%04X",
                    usbDevice.getVendorId(),
                    usbDevice.getProductId()
            ));
        }


        tv_usb.setText(sb.toString());

        connect();//连接
    }

    private UsbDeviceConnection usbDeviceConnection;

    /**
     * usb设备的连接
     */
    private void connect() {
        Log.i(TAG, "usb connect...");
        Toast.makeText(this, "usb connect...", Toast.LENGTH_SHORT).show();
        usbDeviceConnection = null;
        usbDeviceConnection = usbManager.openDevice(mUsbSerialPort.getDriver().getDevice());
        if (usbDeviceConnection == null) {
//            ll_usb_connect.setVisibility(View.GONE);
            tv_usb.setText(tv_usb.getText().toString() + " 连接失败！！！");
        } else {
            tv_usb.setText(tv_usb.getText().toString() + " 连接成功。");
//            ll_usb_connect.setVisibility(View.VISIBLE);
            tv_connect_state.setText("USB通信");
            tv_receive.setText("");
//            et_send.setText("01 03 00 01 00 01 D5 CA");
            setConnectionParam();
            onDeviceStateChange();
        }

    }

    /**
     * 设置通讯参数
     */
    private void setConnectionParam() {
        try {
            if (mUsbSerialPort.getDriver().getDevice().getInterfaceCount() > 0) {

                mUsbSerialPort.open(usbDeviceConnection);
                mUsbSerialPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                StringBuilder sb = new StringBuilder();

                sb.append("\n CD  - Carrier Detect = " + mUsbSerialPort.getCD());
                sb.append("\n CTS - Clear To Send = " + mUsbSerialPort.getCTS());
                sb.append("\n DSR - Data Set Ready = " + mUsbSerialPort.getDSR());
                sb.append("\n DTR - Data Terminal Ready = " + mUsbSerialPort.getDTR());
                sb.append("\n DSR - Data Set Ready = " + mUsbSerialPort.getDSR());
                sb.append("\n RI  - Ring Indicator = " + mUsbSerialPort.getRI());
                sb.append("\n RTS - Request To Send = " + mUsbSerialPort.getRTS());

                tv_connect_state.setText(sb.toString());

                try {
                    mUsbSerialPort.setDTR(true);
                } catch (IOException x) {
                    Log.e(TAG, "IOException DTR: " + x.getMessage());
                }
                try {
                    mUsbSerialPort.setRTS(true);
                } catch (IOException x) {
                    Log.e(TAG, "IOException RTS: " + x.getMessage());
                }
            }
            //无通讯接口
            else {
                tv_connect_state.setText("该USB无通讯接口");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error setting up device: " + e.getMessage());
            tv_connect_state.setText("Error opening device: " + e.getMessage());
            try {
                mUsbSerialPort.close();
            } catch (IOException e2) {
                // Ignore.
            }
            mUsbSerialPort = null;
            return;
        }

    }


    public boolean sendData(byte[] data) {
        Log.i(TAG, "sendData:" + Arrays.toString(data));
        if (mSerialIoManager != null) {
            mSerialIoManager.writeAsync(data);
        }
        return false;
    }

    String text = "";
    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

                @Override
                public void onRunError(Exception e) {
                    Log.d(TAG, "Runner stopped.");
                }

                @Override
                public void onNewData(final byte[] data) {
                    Log.i(TAG, "onReceiver data=" + Arrays.toString(data));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            text += HexUtil.bytesToHexString(data) + "\n";
                            tv_receive.setText(text);
                        }
                    });
                }
            };


    /**
     * usb设备断开连接
     */
    private void disconnect() {
        Log.e(TAG, "usb disconnect...");
        stopIoManager();
        if (mUsbSerialPort != null) {
            try {
                mUsbSerialPort.close();
            } catch (IOException e) {
                // Ignore.
            }
            mUsbSerialPort = null;
        }
    }

    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private SerialInputOutputManager mSerialIoManager;
    private ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private void startIoManager() {
        if (mSerialIoManager == null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(mUsbSerialPort, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(usbReceiver);
        usbReceiver = null;
        disconnect();

        super.onDestroy();
    }

}
