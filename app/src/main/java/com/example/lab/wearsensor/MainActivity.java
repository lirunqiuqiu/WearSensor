package com.example.lab.wearsensor;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleIndicateCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.example.lab.operation.BleServiceListActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.widget.QMUITabSegment;

import org.greenrobot.greendao.database.Database;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author lirunqiu
 * @date 20180609
 * @version V1.0.0
 */
public class MainActivity extends Activity {
    public final static UUID HT_SERVICE_UUID = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb");
    /** Health Thermometer Measurement characteristic UUID */
    private static final UUID HT_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A1C-0000-1000-8000-00805f9b34fb");
    private final static int HIDE_MSB_8BITS_OUT_OF_32BITS = 0x00FFFFFF;
    private final static int HIDE_MSB_8BITS_OUT_OF_16BITS = 0x00FF;
    private final static int SHIFT_LEFT_8BITS = 8;
    private final static int SHIFT_LEFT_16BITS = 16;
    private final static int GET_BIT24 = 0x00400000;
    private final static int FIRST_BIT_MASK = 0x01;

    private LineChart lineChart;
    private BluetoothAdapter mBluetoothAdapter;
    private QMUITabSegment mTabSegment;
    private static final int REQUEST_CODE_OPEN_GPS = 1;
    private static final int REQUEST_CONNECT_BLE_DEVICE= 2;
    private static final String TAG = "MainActivity";

    private static final int REQUEST_CODE_PERMISSION_LOCATION = 2;
    private BleDevice mBleDevice;

    private boolean isFinish = true;

    private DaoMaster.DevOpenHelper mHelper;
    private Database db;
    private DaoSession mMaoSession;
    private RecordDao mRecordDao;

    private SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
    private List<String> timeList = new ArrayList<>();
    // 数据
    private LineData lineData;
    private LineDataSet lineDataSet;
    private List<ILineDataSet> lineDataSets = new ArrayList<>();

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button scanBleButton = findViewById(R.id.bt_open_ble);
        mTabSegment = findViewById(R.id.tab_select);
        lineChart = findViewById(R.id.lineChart);

        BleManager.getInstance().init(getApplication());
        BleManager.getInstance()
                .enableLog(true)
                .setReConnectCount(1, 5000)
                .setConnectOverTime(20000)
                .setOperateTimeout(5000);


      scanBleButton.setOnClickListener(new View.OnClickListener(){
          @Override
          public void onClick(View view) {
              addEntry((float)20);
          }
      });
//        insertVale();
//        readVale();

        mTabSegment.setMode(QMUITabSegment.MODE_FIXED);

        int iconShowSize = QMUIDisplayHelper.dp2px(this, 20);
        Drawable normalDrawable = ContextCompat.getDrawable(this, R.drawable.thermometer1);
        normalDrawable.setBounds(0, 0, iconShowSize, iconShowSize);
        Drawable selectDrawable = ContextCompat.getDrawable(this, R.drawable.thermometer1);
        selectDrawable.setBounds(0, 0, iconShowSize, iconShowSize);

        mTabSegment.addTab(new QMUITabSegment.Tab("蓝牙"));

        mTabSegment.addTab(new QMUITabSegment.Tab("监护"));
        mTabSegment.addTab(new QMUITabSegment.Tab("我的"));
        mTabSegment.setDefaultSelectedColor(ContextCompat.getColor(MainActivity.this,R.color.colorGreen));
        mTabSegment.setDefaultTabIconPosition(QMUITabSegment.ICON_POSITION_TOP);

        mTabSegment.selectTab(1);

        mTabSegment.addOnTabSelectedListener(new QMUITabSegment.OnTabSelectedListener(){
            /**
             * 当某个Tab被选中时会被触发
             * @param index 被选中的 Tab 下标
             */
            @Override
            public void onTabSelected(int index) {
               switch (index){
                   case 0:
                       checkPermissions();
                       break;
                   case 1:
                       break;
                   case 2:
                       //addEntry((int) (Math.random() * 100));
                       break;

                   default:
                       break;
               }
            }

            /**
             * 某个Tab被取消选中时会被触发
             * @param index 被取消选中的 Tab 下标
             */
            @Override
            public void onTabUnselected(int index) {

            }

            /**
             * 当某个Tab处于被选中状态再次点击时触发
             * @param index 被再次点击的 Tab 下标
             */
            @Override
            public void onTabReselected(int index) {

            }
            /**
             * 当某个 Tab 被双击时会触发
             * @param index 被双击的 Tab 下标
             */
            @Override
            public void onDoubleTap(int index) {

            }
        });


        //initLineChart();
        initLineDataSet(null,ContextCompat.getColor(MainActivity.this,R.color.colorGreen));

    }

    /**
     * 初始化折线图
     */
    void initLineChart() {
        lineChart.setScaleEnabled(false);
        lineChart.setBackgroundColor(Color.WHITE);
//        Description description = new Description();
//        //description.setText("t/s");
//        // 是否显示右下角文字信息
//        description.setEnabled(false);
 //       lineChart.setDescription(description);

        XAxis xAxis = lineChart.getXAxis();
        // x轴上的数值是否显示
        xAxis.setDrawLabels(true);
        // 是否绘制X轴
        xAxis.setDrawAxisLine(true);
        // 是否绘制X轴的网格线
        xAxis.setDrawGridLines(false);
        //xAxis.setSpaceBetweenLabels(4);

        xAxis.setLabelCount(6);
        xAxis.setDrawScale(true);
        // X轴的位置
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        //
        xAxis.setTextColor(ContextCompat.getColor(MainActivity.this,R.color.colorGreen));
        xAxis.setAxisLineColor(ContextCompat.getColor(MainActivity.this,R.color.colorGreen));
        xAxis.setAxisLineWidth(2.5f);

        //自定义x轴标签数据
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                Log.e(TAG,"form" + value);
                return timeList.get((int) value * 5);
            }
        });

        // 取消y轴的右侧
        lineChart.getAxisRight().setEnabled(false);
        // 是否绘制Y轴的网格线
        lineChart.getAxisLeft().setDrawGridLines(false);

        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.setDrawScale(true);

        yAxis.setTextColor(ContextCompat.getColor(MainActivity.this,R.color.colorGreen));
        yAxis.setAxisLineColor(ContextCompat.getColor(MainActivity.this,R.color.colorGreen));
        yAxis.setAxisLineWidth(2.5f);
        yAxis.setTextSize(10);
        yAxis.setStartAtZero(false);
//        yAxis.setAxisMaxValue(45f);
 //       yAxis.setAxisMinValue(20f);
    }



    private void initLineDataSet(String name, int color) {

        lineDataSet = new LineDataSet(null, name);
        lineDataSet.setLineWidth(1.5f);
        lineDataSet.setCircleRadius(2f);
        lineDataSet.setColor(color);
        lineDataSet.setCircleColor(color);
        lineDataSet.setHighLightColor(color);
        lineDataSet.setCircleHoleColor(color);
        lineDataSet.setDrawValues(false);
//        lineDataSet.setValueTextSize(10f);
//        lineDataSet.setValueTextColor(color);

        lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        //添加一个空的 LineData
        lineData = new LineData();
        lineChart.setData(lineData);
        lineChart.invalidate();
    }

    public void addEntry(float number) {
        if (lineDataSet.getEntryCount() == 0) {
            lineData.addDataSet(lineDataSet);
            Log.e(TAG,"addnewLine");
        }
        lineChart.setData(lineData);
        lineChart.invalidate();


        //避免集合数据过多，及时清空（做这样的处理，并不知道有没有用，但还是这样做了）
//        if (timeList.size() > 11) {
//            timeList.clear();
//        }

        timeList.add(df.format(System.currentTimeMillis()));

        Entry entry = new Entry(lineDataSet.getEntryCount(), number);
        lineData.addEntry(entry,0);
        //通知数据已经改变
        lineData.notifyDataChanged();
        lineChart.notifyDataSetChanged();
        //设置在曲线图中显示的最大数量
        lineChart.setVisibleXRangeMaximum(5);
        //移到某个位置
        Log.e(TAG,"lineDataSet" + lineDataSet.getEntryCount());
        Log.e(TAG,"lineData" + lineData.getEntryCount());
        lineChart.moveViewToX(lineData.getEntryCount() - 5);
    }

    /**
     * 写数据库
     */
    void insertVale(){
        mHelper = new DaoMaster.DevOpenHelper(this, "zouxiaomin");
        db = mHelper.getWritableDb();
        mMaoSession = new DaoMaster(db).newSession();
        mRecordDao = mMaoSession.getRecordDao();

//        for (int a = 0; a < 36; a++) {
//            Record record = new Record(null,a,new Date());
//            mRecordDao.insert(record);
//        }


    }

    /**
     * 读数据库
     */
    void readVale() {
//        List<Record> list = mRecordDao.queryBuilder()
//                .where(RecordDao.Properties.TempValue.between(1,10)).limit(6).build().list();
//        for (int i = 0; i < list.size(); i++) {
//            Log.d(TAG, "search: " + list.get(i).toString());
//        }
        List<Record> list = mRecordDao.loadAll();
        for (int i = 0; i < list.size(); i++) {
            Log.d(TAG, "search: " + list.get(i).getTempValue());
        }
    }


    /**
     * 检测权限
     */
    private void checkPermissions() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth is not available",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // 允许一个权限访问位置，GPS
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
        List<String> permissionDeniedList = new ArrayList<>();

        for (String permission : permissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, permission);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted(permission);
            } else {
                permissionDeniedList.add(permission);
            }
            Log.e(TAG, "permissionCheck");
        }

        if (!permissionDeniedList.isEmpty()) {
            String[] deniedPermissions = permissionDeniedList.toArray(new String[permissionDeniedList.size()]);
            ActivityCompat.requestPermissions(this, deniedPermissions, REQUEST_CODE_PERMISSION_LOCATION);
        }
    }

    /**
     *
     * @param permission
     */
    private void onPermissionGranted(String permission) {
       switch (permission) {
           case Manifest.permission.ACCESS_FINE_LOCATION:
               // 系统版本大于版本23
               if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkGPSIsOpen()) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.notifyTitle)
                            .setMessage(R.string.gpsNotifyMsg)
                            .setNegativeButton(R.string.cancel,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                        }
                                    })
                            .setPositiveButton(R.string.setting,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                            startActivityForResult(intent, REQUEST_CODE_OPEN_GPS);
                                        }
                                    })
                            .setCancelable(false)
                            .show();
                   Log.e(TAG, "checkGPSIsnitOpen");
                }
                else {

                   Intent serverIntent = new Intent(MainActivity.this,BleServiceListActivity.class);
                   startActivityForResult(serverIntent, REQUEST_CONNECT_BLE_DEVICE);
                   Log.e(TAG, "checkGPSIsOpen");
                }
               break;
            default:
               break;
        }
    }

    /**
     *
     * @return
     */
        private boolean checkGPSIsOpen() {
           // 获取manager
            LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager == null) {
                return false;
            }
            return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
        }

    @Override
    public void onStart() {

        super.onStart();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BleManager.getInstance().disconnectAllDevice();
        BleManager.getInstance().removeIndicateCallback(mBleDevice,HT_MEASUREMENT_CHARACTERISTIC_UUID.toString());
        BleManager.getInstance().destroy();
    }

    /**
     *
     * @param data
     * @return
     * @throws Exception
     */
    private double decodeTemperature(byte[] data) throws Exception {
        double temperatureValue;
        byte flag = data[0];
        byte exponential = data[4];
        short firstOctet = convertNegativeByteToPositiveShort(data[1]);
        short secondOctet = convertNegativeByteToPositiveShort(data[2]);
        short thirdOctet = convertNegativeByteToPositiveShort(data[3]);
        int mantissa = ((thirdOctet << SHIFT_LEFT_16BITS) | (secondOctet << SHIFT_LEFT_8BITS)
                | (firstOctet)) & HIDE_MSB_8BITS_OUT_OF_32BITS;

        mantissa = getTwosComplimentOfNegativeMantissa(mantissa);
        temperatureValue = (mantissa * Math.pow(10, exponential));

        /*
         * Conversion of temperature unit from Fahrenheit to Celsius if unit is in Fahrenheit
         * Celsius = (Fahrenheit -32) 5/9
         */
        if ((flag & FIRST_BIT_MASK) != 0) {
            temperatureValue = (float) ((temperatureValue - 32) * (5 / 9.0));
        }
        return temperatureValue;
    }

    /**
     *
     * @param octet
     * @return
     */
    private short convertNegativeByteToPositiveShort(byte octet) {
        if (octet < 0) {
            return (short) (octet & HIDE_MSB_8BITS_OUT_OF_16BITS);
        } else {
            return octet;
        }
    }

    /**
     *
     * @param mantissa
     * @return
     */
    private int getTwosComplimentOfNegativeMantissa(int mantissa) {
        if ((mantissa & GET_BIT24) != 0) {
            return ((((~mantissa) & HIDE_MSB_8BITS_OUT_OF_32BITS) + 1) * (-1));
        } else {
            return mantissa;
        }
    }

    /**
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){

            case REQUEST_CODE_OPEN_GPS:
                break;
            case REQUEST_CONNECT_BLE_DEVICE:
                mTabSegment.selectTab(1);
                Log.e(TAG, "onReadSuccess");
                if (resultCode == Activity.RESULT_OK) {
                    mBleDevice = data.getParcelableExtra(BleServiceListActivity.KEY_DATA);
                    if(mBleDevice != null) {
                        Log.e(TAG, "onReadSuccess");
                        BleManager.getInstance().indicate(
                                mBleDevice,
                                "00001809-0000-1000-8000-00805f9b34fb",
                                "00002A1C-0000-1000-8000-00805f9b34fb",
                                new BleIndicateCallback() {
                                    @Override
                                    public void onIndicateSuccess() {
                                        Log.e(TAG, "IndicateSUCCESS");
                                    }

                                    @Override
                                    public void onIndicateFailure(BleException exception) {
                                        Log.e(TAG, "IndicateFailure");
                                    }

                                    @Override
                                    public void onCharacteristicChanged(byte[] data) {
                                        try {
                                            final double tempValue = decodeTemperature(data);
                                            //points.add(new PointValue(x,(float) tempValue));
                                            //
                                            isFinish = false;
                                            Log.e(TAG, "tempValue"+ tempValue);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }

                                    }
                                });
                    }
                }
                break;
                default:
                    break;
        }
    }
}
