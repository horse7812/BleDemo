package com.example.admin.mybledemo.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.admin.mybledemo.Command;
import com.example.admin.mybledemo.LeDeviceListAdapter;
import com.example.admin.mybledemo.R;
import com.example.admin.mybledemo.StaticValue;
import com.example.admin.mybledemo.utils.SPUtils;
import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.com.heaton.blelibrary.BleConfig;
import cn.com.heaton.blelibrary.BleLisenter;
import cn.com.heaton.blelibrary.BleManager;
import cn.com.heaton.blelibrary.BleVO.BleDevice;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class MainActivity extends BaseActivity implements AdapterView.OnItemClickListener {

    private String TAG = MainActivity.class.getSimpleName();

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private ListView lvLeftMenu;
    private String[] lvs = {"设置", "切换主题颜色", "待定", "待定"};
    private ArrayAdapter arrayAdapter;
    private AnimationDrawable mAnimationDrawable;

    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BleManager mManager;
    private ListView mListView;
    private TextView mConnectedNum;
    private Button mSend;
    private String currentAddress;

    private BleLisenter mLisenter = new BleLisenter() {
        @Override
        public void onStart() {
            super.onStart();
            //可以选择性实现该方法   不需要则不用实现
        }

        @Override
        public void onStop() {
            super.onStop();
            //可以选择性实现该方法   不需要则不用实现
            invalidateOptionsMenu();
        }

        @Override
        public void onConnectTimeOut() {
            super.onConnectTimeOut();
            Logger.e("设备连接超时");
        }

        @Override
        public void onLeScan(final BleDevice device, int rssi, byte[] scanRecord) {
            Logger.e("onLeScan");
//                            //可以选择性的根据scanRecord蓝牙广播包进行过滤
//                            如下  此处注释（根据你们产品的广播进行过滤或者根据产品的特定name或者address进行过滤也可以）
//                            if(!BleConfig.matchProduct(scanRecord)){
//                                return;
//                            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLeDeviceListAdapter.addDevice(device);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onConnectionChanged(final BleDevice device) {
            Logger.e("onConnectionChanged" + device.getConnectionState() + device.isConnected());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setConnectedNum();
                    for (int i = 0; i < mLeDeviceListAdapter.getCount(); i++) {
                        if (device.getBleAddress().equals(mLeDeviceListAdapter.getDevice(i).getBleAddress())) {
                            if (device.isConnected()) {
                                mLeDeviceListAdapter.getDevice(i).setConnected(true);
                                Toast.makeText(MainActivity.this, R.string.line_success, Toast.LENGTH_SHORT).show();
                            } else {
                                mLeDeviceListAdapter.getDevice(i).setConnected(false);
                                Toast.makeText(MainActivity.this, R.string.line_disconnect, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt) {
            super.onServicesDiscovered(gatt);
            //可以选择性实现该方法   不需要则不用实现
//                            if (QppApi.qppEnable(mBluetoothGatt, uuidQppService, uuidQppCharWrite)) {}
            //设置notify
            displayGattServices(gatt.getDevice().getAddress(), mManager.getBleService().getSupportedGattServices(gatt.getDevice().getAddress()));
        }

        @Override
        public void onChanged(BluetoothGattCharacteristic characteristic) {
            Logger.e("data===" + Arrays.toString(characteristic.getValue()));
            //可以选择性实现该方法   不需要则不用实现
//                            QppApi.updateValueForNotification(gatt, characteristic);
        }

        @Override
        public void onWrite(BluetoothGatt gatt) {
            //可以选择性实现该方法   不需要则不用实现
        }

        @Override
        public void onRead(BluetoothDevice device) {
            super.onRead(device);
            //可以选择性实现该方法   不需要则不用实现
            Logger.e("onRead");
        }

        @Override
        public void onDescriptorWriter(BluetoothGatt gatt) {
            super.onDescriptorWriter(gatt);
            //可以选择性实现该方法   不需要则不用实现
////                            QppApi.setQppNextNotify(gatt, true);
        }
    };

    public boolean changeLevelInner(String address) {
//        byte[] data = new byte[Command.ComSyncColorLen];
//        System.arraycopy(Command.ComSyncColor, 0, data, 0, Command.ComSyncColor.length);
//        data[4] = (byte) (color & 0xff);
//        data[5] = (byte) ((color >> 8) & 0xff);
//        data[6] = (byte) ((color >> 16) & 0xff);
//        data[7] = (byte) ((color >> 24) & 0xff);
        boolean result = mManager.sendData(address, mWriteCharacteristic, sendData(1));
        Logger.e("result==" + result);
        return result;
    }

    private BluetoothGattCharacteristic mWriteCharacteristic;//ble发送对象

    private void displayGattServices(String address, List<BluetoothGattService> gattServices) {
        if (gattServices == null)
            return;
        currentAddress = address;
        String uuid = null;
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();
            Logger.d("displayGattServices: " + uuid);
            if (uuid.equals(BleConfig.UUID_SERVICE_TEXT)) {
                Logger.d("service_uuid: " + uuid);
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    uuid = gattCharacteristic.getUuid().toString();
                    Logger.e("all_characteristic: " + uuid);
                    if (uuid.equals(BleConfig.UUID_NOTIFY_TEXT)) {
                        Logger.e("2gatt Characteristic: " + uuid);
                        mManager.setCharacteristicNotification(address, gattCharacteristic, true);//
//                        mBluetoothLeService.readCharacteristic(address,gattCharacteristic);//暂时注释
                    } else if (uuid.equals(BleConfig.UUID_CHARACTERISTIC_TEXT)) {
                        mWriteCharacteristic = gattCharacteristic;
                        Logger.e("write_characteristic: " + uuid);
                    }

                }
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //初始化蓝牙
        initBle();
        initView();
//        initMenu();
    }

    private void initMenu() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.dl_left);
        LinearLayout menu_bg = (LinearLayout) findViewById(R.id.drawer_menu);
        lvLeftMenu = (ListView) findViewById(R.id.lv_left_menu);

        //创建返回键，并实现打开关/闭监听
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.open, R.string.close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                //侧滑菜单打开时
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                //侧滑菜单关闭时
            }
        };
        mDrawerToggle.syncState();
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        //设置菜单列表
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, lvs);
        lvLeftMenu.setAdapter(arrayAdapter);
        lvLeftMenu.setOnItemClickListener(this);
    }

    private void initBle() {
        try {
            mManager = BleManager.getInstance(this, mLisenter);
            boolean result = false;
            if (mManager != null) {
                result = mManager.startService();
                if (!mManager.isBleEnable()) {//蓝牙未打开
                    mManager.turnOnBlueTooth(this);
                } else {//已打开
                    requestPermission(new String[]{Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_COARSE_LOCATION}, getString(R.string.ask_permission), new GrantedResult() {
                        @Override
                        public void onResult(boolean granted) {
                            if (!granted) {
                                finish();
                            } else {
                                //开始扫描
                                mManager.scanLeDevice(true);
                            }
                        }
                    });
                }
            }
            if (!result) {
                Logger.e("服务绑定失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //播放音乐
    public byte[] sendData(int play) {
        byte[] data = new byte[Command.qppDataSend.length];
        System.arraycopy(Command.qppDataSend, 0, data, 0, data.length);
        data[6] = 0x03;
        data[7] = (byte) play;
        Logger.e("data:" + Arrays.toString(data));
        return data;
    }

    private void initView() {
        setTitle("扫描界面");
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mListView = (ListView) findViewById(R.id.listView);
        mConnectedNum = (TextView) findViewById(R.id.connected_num);
        mSend = (Button) findViewById(R.id.sendData);
        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentAddress != null) {
                    changeLevelInner(currentAddress);
                }
            }
        });
//        setConnectedNum();
        // Initializes list view adapter.
        if (mManager != null) {
            mLeDeviceListAdapter = new LeDeviceListAdapter(this);
        }
        mListView.setAdapter(mLeDeviceListAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final BleDevice device = mLeDeviceListAdapter.getDevice(position);
                if (device == null) return;
                if (mManager.mScanning) {
                    mManager.scanLeDevice(false);
                }
                if (device.isConnected()) {
                    mManager.disconnect(device.getBleAddress());
                } else {
                    mManager.connect(device.getBleAddress());
                }
            }
        });
    }


    private void setConnectedNum() {
        if (mManager != null) {
            mConnectedNum.setText(getString(R.string.lined_num) + mManager.getConnectedDevices().size());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                Logger.e("点击了扫描按钮");
                if (mManager != null && !mManager.mScanning) {
//                    mLeDeviceListAdapter.clear();
//                    mManager.clear();
                    mManager.scanLeDevice(true);
                }
                break;
            case R.id.menu_stop:
                Logger.e("点击了停止扫描按钮");
                if (mManager != null) {
                    mManager.scanLeDevice(false);
                }
                break;
            case R.id.menu_connect_all:
                Logger.e("点击了连接全部设备按钮");
                if (mManager != null) {
                    for (int i = 0; i < mLeDeviceListAdapter.getCount(); i++) {
                        BleDevice device = mLeDeviceListAdapter.getDevice(i);
                        mManager.connect(device.getBleAddress());
                    }
                }
                break;
            case R.id.menu_disconnect_all:
                Logger.e("点击了断开全部设备按钮");
                if (mManager != null) {
//                    for (int i = 0; i < mManager.getScanBleDevice().size(); i++) {
//                        BleDevice device = mManager.getBleDevice(i);
//                        mManager.disconnect(device.getBleAddress());
//                    }
                    List<BluetoothDevice> list = mManager.getConnectedDevices();
                    for(BluetoothDevice device : list){
                        mManager.disconnect(device.getAddress());
                    }
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == BleManager.REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        } else {
            if (mManager != null) {
                mManager.scanLeDevice(true);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mManager != null) {
            mManager.scanLeDevice(false);
        }
        mLeDeviceListAdapter.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mManager != null) {
            mManager.unService();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//        if(position == 1){
//            int currentMode = SPUtils.get(getApplication(),StaticValue.KEY_THEME,0);
//            //切换主题颜色
//            new AlertDialog.Builder(this).setTitle("选择主题")
//                    .setSingleChoiceItems(new String[]{"标准模式", "夜间模式"}, currentMode, new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            dialog.dismiss();
//                            if(mManager != null){
//                                mManager.unService();
//                                mManager = null;
//                            }
//                            if(which == 0){
//                            SPUtils.put(getApplication(), StaticValue.KEY_THEME,0);
//                                //主题色  白黑 夜间模式
//                                StaticValue.THEME_MODE = 0;
//                                recreate();
//                            }else {
//                            SPUtils.put(getApplication(), StaticValue.KEY_THEME,1);
//                                Log.e(TAG,"切换到了深色主题"+which);
//                                StaticValue.THEME_MODE = 1;
//                                recreate();
//                            }
//                        }
//                    }).setNegativeButton("取消",null).show();
//        }
    }
}
