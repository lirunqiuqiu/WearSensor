package com.example.lab.operation;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.example.lab.wearsensor.R;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lirunqiu
 * @date 2018/06/18
 * @version V1.0.0
 */
public class BleServiceListActivity extends Activity{
    public static final String KEY_DATA = "key_data";
    private static final String TAG = "BleServiceActivity";

    /**
     * 扫描到的ble设备
     */
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    private List<BleDevice> bleDeviceList;

    private Boolean mScanConnect = false;
    private BleDevice mBleDevice;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_device);

        final Button scanButton = findViewById(R.id.btn_scan);

        mNewDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);
        ListView newDevicesListView = findViewById(R.id.lv_new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);
        bleDeviceList = new ArrayList<>();

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mScanConnect) {
                    startScanBle();
                    mScanConnect = false;
                    scanButton.setVisibility(View.GONE);
                } else {
                    BleManager.getInstance().disconnectAllDevice();
                    scanButton.setText("扫描设备");
                    mScanConnect = true;
                }
            }
        });
                if(isFirstRun()) {
                    startScanBle();
                    scanButton.setVisibility(View.GONE);
                } else {
                    // 自动连接上次设备
                    SharedPreferences sharedPreferences = getSharedPreferences(
                            "share", 0);
                    String mac = sharedPreferences.getString("mac", "");
                    String name = sharedPreferences.getString("name", "");
                    Log.e(TAG, "mac   " + mac);
                    Log.e(TAG, "mac   " + name);

                    scanButton.setText("取消自动连接");
                    scanButton.setVisibility(View.VISIBLE);

                    BleManager.getInstance().connect(mac, new BleGattCallback() {
                        @Override
                        public void onStartConnect() {
                            Toast.makeText(BleServiceListActivity.this, getString(R.string.start_connet), Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onConnectFail(BleDevice bleDevice, BleException exception) {
                            Toast.makeText(BleServiceListActivity.this, getString(R.string.connect_fail), Toast.LENGTH_LONG).show();
                            Toast.makeText(BleServiceListActivity.this, getString(R.string.check_device), Toast.LENGTH_LONG).show();
                            scanButton.setText("扫描设备");
                        }

                        @Override
                        public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                            Toast.makeText(BleServiceListActivity.this, bleDevice.getName() +getString(R.string.connect_success) , Toast.LENGTH_LONG).show();

                            if( mScanConnect) {
                                BleManager.getInstance().disconnectAllDevice();
                            } else {
                                mBleDevice = bleDevice;
                                Intent intent = new Intent();
                                intent.putExtra(KEY_DATA, bleDevice);
                                setResult(Activity.RESULT_OK, intent);
                                finish();
                            }
                        }

                        @Override
                        public void onDisConnected(boolean isActiveDisConnected, BleDevice device, BluetoothGatt gatt, int status) {

                        }
                    });

                }
    }


    /**
     * 扫描Ble设备
     */
    private void startScanBle() {
        //调用scan BleScanCallback方法
        BleManager.getInstance().scan(new BleScanCallback() {
            @Override
            public void onScanStarted(boolean success) {
                clearScanDevice();
                Log.e(TAG, "onLeScanStarted");
                mNewDevicesArrayAdapter.notifyDataSetChanged();

            }

            @Override
            public void onLeScan(BleDevice bleDevice) {
                Log.e(TAG, "onLeScan");
                super.onLeScan(bleDevice);
            }

            @Override
            public void onScanning(BleDevice bleDevice) {
                Log.e(TAG, "onLeScanning");
                addDevice(bleDevice);
                mNewDevicesArrayAdapter.notifyDataSetChanged();
            }

            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                Log.e(TAG, "onScanFinished");
                Log.e(TAG, "onScanFinished"+ bleDeviceList.get(0).getName());

            }
        });

    }

    /**
     * android 应用程序是否安装后第一次运行
     * @return true - 第一次  false - 不是第一次
     */
    public boolean isFirstRun(){
        SharedPreferences sharedPreferences = getSharedPreferences(
                "share", 0);
        boolean isFirstRun = sharedPreferences.getBoolean("isFirstRun", true);

        if (isFirstRun) {
            System.out.println("第一次运行");
            sharedPreferences.edit().putBoolean("isFirstRun",false).commit();
            return true;
        } else {
            System.out.println("不是第一次运行");
            return false;
        }
    }

    /**
     * 添加ble设备
     * @param bleDevice
     */
    public void addDevice(BleDevice bleDevice) {
        removeDevice(bleDevice);
        bleDeviceList.add(bleDevice);
        String name = bleDevice.getName();
        String mac = bleDevice.getMac();
        mNewDevicesArrayAdapter.add(name + "\n" + mac);
    }

    /**
     * 移除设备
     * @param bleDevice
     */
    public void removeDevice(BleDevice bleDevice) {
        for (int i = 0; i < bleDeviceList.size(); i++) {
            BleDevice device = bleDeviceList.get(i);
            if (bleDevice.getKey().equals(device.getKey())) {
                bleDeviceList.remove(i);
            }
        }
    }

    /**
     * 清除已连接设备
     */
    public void clearConnectedDevice() {
        for (int i = 0; i < bleDeviceList.size(); i++) {
            BleDevice device = bleDeviceList.get(i);
            if (BleManager.getInstance().isConnected(device)) {
                bleDeviceList.remove(i);
            }
        }
    }

    /**
     * 清除扫描到设备
     */
    public void clearScanDevice() {
        for (int i = 0; i < bleDeviceList.size(); i++) {
            BleDevice device = bleDeviceList.get(i);
            if (!BleManager.getInstance().isConnected(device)) {
                bleDeviceList.remove(i);

            }
        }
        mNewDevicesArrayAdapter.clear();
    }

    /**
     *
     * @return Ble设备
     */
    public BleDevice getBleDevice() {
        return mBleDevice;
    }
    /**
     * list按下后连接设备
     */
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        //选项点击事件
        @Override
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // 获取MAC地址
            String info[] = ((TextView) v).getText().toString().split("\n");
            final String name = info[0];
            final String address = info[1];
            final int index = arg2;
            Log.e(TAG, "name    "+ name);
            Log.e(TAG, "address    "+ address);

            BleManager.getInstance().connect(bleDeviceList.get(arg2), new BleGattCallback() {
                @Override
                public void onStartConnect() {
                    Toast.makeText(BleServiceListActivity.this, getString(R.string.start_connet), Toast.LENGTH_LONG).show();
                }
                @Override
                public void onConnectFail(BleDevice bleDevice, BleException exception) {
                    Toast.makeText(BleServiceListActivity.this, getString(R.string.connect_fail), Toast.LENGTH_LONG).show();
                }

                @Override
                public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                    Toast.makeText(BleServiceListActivity.this, getString(R.string.connect_success), Toast.LENGTH_LONG).show();
                    mBleDevice = bleDevice;

                    Intent intent = new Intent();
                    intent.putExtra(KEY_DATA, bleDevice);
                    setResult(Activity.RESULT_OK, intent);

                    SharedPreferences sharedPreferences = getSharedPreferences(
                            "share", 0);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("mac", address);
                    editor.putString("name",name);
                    editor.commit();

                    finish();
                }

                @Override
                public void onDisConnected(boolean isActiveDisConnected, BleDevice device, BluetoothGatt gatt, int status) {
                    removeDevice(bleDeviceList.get(index));
                }
            });
        }
    };

}
