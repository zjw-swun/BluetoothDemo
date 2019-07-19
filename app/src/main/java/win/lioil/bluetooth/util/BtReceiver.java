package win.lioil.bluetooth.util;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;

/**
 * 监听蓝牙广播-各种状态
 */
public class BtReceiver extends BroadcastReceiver {
    private static final String TAG = BtReceiver.class.getSimpleName();
    private final Listener mListener;

    public BtReceiver(Context cxt, Listener listener) {
        mListener = listener;
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);//蓝牙开关状态
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);//蓝牙开始搜索
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);//蓝牙搜索结束

        filter.addAction(BluetoothDevice.ACTION_FOUND);//蓝牙发现新设备(未配对的设备)
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);//在系统弹出配对框之前(确认/输入配对码)
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);//设备配对状态改变
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);//最底层连接建立
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);//最底层连接断开

        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED); //BluetoothAdapter连接状态
        filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED); //BluetoothHeadset连接状态
        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED); //BluetoothA2dp连接状态
        cxt.registerReceiver(this, filter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null)
            return;
        Log.i(TAG, "===" + action);
        BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (dev != null)
            Log.i(TAG, "BluetoothDevice: " + dev.getName() + ", " + dev.getAddress());
        switch (action) {
            case BluetoothAdapter.ACTION_STATE_CHANGED:
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                Log.i(TAG, "STATE: " + state);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        //蓝牙关闭
                        break;
                    case BluetoothAdapter.STATE_ON:
                        //蓝牙开启
                        break;
                    default:
                        break;
                }
                break;
            case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                break;
            case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                Toast.makeText(context, "蓝牙扫描完毕", Toast.LENGTH_SHORT).show();
                break;

            case BluetoothDevice.ACTION_FOUND:
                //蓝牙扫描发现设备(多次调用 一次发现一个 发现所有附近蓝牙设备大概持续12S左右)
                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MAX_VALUE);
                Log.i(TAG, "EXTRA_RSSI:" + rssi);
                mListener.foundDev(dev);
                break;
            case BluetoothDevice.ACTION_PAIRING_REQUEST: //在系统弹出配对框之前，实现自动配对，取消系统配对框
                /*try {
                    abortBroadcast();//终止配对广播，取消系统配对框
                    boolean ret = dev.setPin("1234".getBytes()); //设置PIN配对码(必须是固定的)
                } catch (Exception e) {
                    e.printStackTrace();
                }*/
                break;
            case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                Log.i(TAG, "BOND_STATE: " + intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1));
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                switch (bondState) {
                    case BluetoothDevice.BOND_NONE:
                        Log.d(TAG, "BOND_NONE 删除配对");
                        mListener.unBondDev(dev);
                        break;
                    case BluetoothDevice.BOND_BONDING:
                        Log.d(TAG, "BOND_BONDING 正在配对");
                        mListener.bondingDev(dev);
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        Log.d(TAG, "BOND_BONDED 配对成功");
                        //建立连接
                        mListener.bondDev(dev);
                        break;
                }
                break;
            case BluetoothDevice.ACTION_ACL_CONNECTED:
                //已连接
                //mListener.connected(dev);
                break;
            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                //断开连接
                //mListener.disConnected(dev);
                break;

            case BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED:
                Log.i(TAG, "CONN_STATE: " + intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, 0));
                break;
            case BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED:
                Log.i(TAG, "CONN_STATE: " + intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, 0));
                break;
            case BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED:
                Log.i(TAG, "CONN_STATE: " + intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, 0));
                break;
        }
    }

    public interface Listener {
        void foundDev(BluetoothDevice dev);

        void bondDev(BluetoothDevice dev);

        void unBondDev(BluetoothDevice dev);

        void bondingDev(BluetoothDevice dev);
    }

    public class BtStatusChangeAdapter implements Listener {

        @Override
        public void foundDev(BluetoothDevice dev) {

        }

        @Override
        public void bondDev(BluetoothDevice dev) {

        }

        @Override
        public void unBondDev(BluetoothDevice dev) {

        }

        @Override
        public void bondingDev(BluetoothDevice dev) {

        }
    }
}