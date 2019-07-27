package win.lioil.bluetooth.bt;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dave.wifirealtimespeeker.audio.SpeexTalkPlayer;
import com.dave.wifirealtimespeeker.audio.SpeexTalkRecorder;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import win.lioil.bluetooth.APP;
import win.lioil.bluetooth.R;
import win.lioil.bluetooth.util.BtReceiver;

public class BtClientActivity extends Activity implements BtBase.Listener, BtReceiver.Listener, BtDevAdapter.Listener {
    private TextView mTips;
    private EditText mInputMsg;
    private EditText mInputFile;
    private TextView mLogs;
    private BtReceiver mBtReceiver;
    private final BtDevAdapter mBtDevAdapter = new BtDevAdapter(this);
    private final BtClient mClient = new BtClient(this);

    private static final int DISCOVERABLE_TIMEOUT_TWO_MINUTES = 120;
    private static final int DISCOVERABLE_TIMEOUT_FIVE_MINUTES = 300;
    private static final int DISCOVERABLE_TIMEOUT_ONE_HOUR = 3600;
    static final int DISCOVERABLE_TIMEOUT_NEVER = 0;
    private SpeexTalkRecorder mSpeexTalkRecorder;
    private SpeexTalkPlayer mSpeexTalkPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_btclient);
        RecyclerView rv = findViewById(R.id.rv_bt);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(mBtDevAdapter);
        mTips = findViewById(R.id.tv_tips);
        mInputMsg = findViewById(R.id.input_msg);
        mInputFile = findViewById(R.id.input_file);
        mLogs = findViewById(R.id.tv_log);
        mBtReceiver = new BtReceiver(this, this);//注册蓝牙广播
        //setDiscoverableTimeout(DISCOVERABLE_TIMEOUT_TWO_MINUTES);
        BluetoothAdapter.getDefaultAdapter().startDiscovery();

        mSpeexTalkRecorder = new SpeexTalkRecorder(new SpeexTalkRecorder.onRecorderListener() {
            @Override
            public void handleRecordData(byte[] recordData) {
                mClient.sendAudio(recordData);
              //  mSpeexTalkPlayer.play(recordData);
            }
        });

        mSpeexTalkPlayer = new SpeexTalkPlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBtReceiver);
        mSpeexTalkPlayer.release();
        mSpeexTalkRecorder.release();
        mClient.unListener();
        mClient.close();
    }

    @Override
    public void onItemClick(BluetoothDevice dev) {
        //点击配对
        if (dev.getBondState() == BluetoothDevice.BOND_NONE) {
            dev.createBond();
        } else {
            connect(dev);
        }
    }

    @Override
    public void onItemLongClick(BluetoothDevice dev) {
        //长按取消配对或者取消连接
        if (dev.getBondState() == BluetoothDevice.BOND_NONE) {
            mClient.close();
        } else {
            unBondDevice(dev);
            mClient.close();
        }
    }


    private void connect(BluetoothDevice dev) {
        if (mClient.isConnected(dev)) {
            APP.toast("已经连接了", 0);
            return;
        }
        mClient.connect(dev);
        APP.toast("正在连接...", 0);
        mTips.setText("正在连接...");
    }

    @Override
    public void foundDev(BluetoothDevice dev) {
        mBtDevAdapter.add(dev);
    }

    @Override
    public void bondDev(BluetoothDevice dev) {
        APP.toast("成功配对" + dev.getName(), Toast.LENGTH_SHORT);
        mBtDevAdapter.notifyItem(dev);
        connect(dev);
    }

    @Override
    public void unBondDev(BluetoothDevice dev) {
        APP.toast("解除配对" + dev.getName(), Toast.LENGTH_SHORT);
        mBtDevAdapter.notifyItem(dev);
    }

    @Override
    public void bondingDev(BluetoothDevice dev) {

    }

    // 重新扫描
    public void reScan(View view) {
        mBtDevAdapter.reScan();
    }

    public void sendMsg(View view) {
        if (mClient.isConnected(null)) {
            String msg = mInputMsg.getText().toString();
            if (TextUtils.isEmpty(msg))
                APP.toast("消息不能空", 0);
            else
                mClient.sendMsg(msg);
        } else
            APP.toast("没有连接", 0);
    }

    public void sendFile(View view) {
        if (mClient.isConnected(null)) {
            String filePath = mInputFile.getText().toString();
            if (TextUtils.isEmpty(filePath) || !new File(filePath).isFile())
                APP.toast("文件无效", 0);
            else
                mClient.sendFile(filePath);
        } else
            APP.toast("没有连接", 0);
    }

    public void sendAudio(View view) {
        if (mClient.isConnected(null)) {
            mSpeexTalkRecorder.start();
        } else {
            APP.toast("没有连接", 0);
        }
    }

    public void endAudio(View view) {
        mSpeexTalkRecorder.stop();
    }

    @Override
    public void socketNotify(int state, final Object obj) {
        if (isDestroyed())
            return;
        String msg = null;
        switch (state) {
            case BtBase.Listener.CONNECTED:
                BluetoothDevice dev = (BluetoothDevice) obj;
                msg = String.format("与%s(%s)连接成功", dev.getName(), dev.getAddress());
                mTips.setText(msg);
                break;
            case BtBase.Listener.DISCONNECTED:
                msg = "连接断开";
                mTips.setText(msg);
                break;
            case BtBase.Listener.AUDIO:
                mSpeexTalkPlayer.play((byte[]) obj);
                break;
            case BtBase.Listener.MSG:
                msg = String.format("\n%s", obj);
                mLogs.append(msg);
                break;
            default:
                break;
        }
        if (!TextUtils.isEmpty(msg)) {
            APP.toast(msg, 0);
        }
    }

    //得到配对的设备列表，清除已配对的设备
    public void removePairDevice() {
        if (BluetoothAdapter.getDefaultAdapter() != null) {
            Set<BluetoothDevice> bondedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
            for (BluetoothDevice device : bondedDevices) {
                unBondDevice(device);
            }
        }
    }

    //反射来调用BluetoothDevice.removeBond取消设备的配对
    private void unBondDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass()
                    .getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {
        }
    }


    //设置蓝牙可见性 timeout值并没有起到作用，可见性是一直保持的
    public void setDiscoverableTimeout(int timeout) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        try {
            Method setDiscoverableTimeout = BluetoothAdapter.class.getMethod("setDiscoverableTimeout", int.class);
            setDiscoverableTimeout.setAccessible(true);
            Method setScanMode = BluetoothAdapter.class.getMethod("setScanMode", int.class, int.class);
            setScanMode.setAccessible(true);

            setDiscoverableTimeout.invoke(adapter, timeout);
            setScanMode.invoke(adapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, timeout);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //关闭蓝牙可见性
    public void closeDiscoverableTimeout() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        try {
            Method setDiscoverableTimeout = BluetoothAdapter.class.getMethod("setDiscoverableTimeout", int.class);
            setDiscoverableTimeout.setAccessible(true);
            Method setScanMode = BluetoothAdapter.class.getMethod("setScanMode", int.class, int.class);
            setScanMode.setAccessible(true);

            setDiscoverableTimeout.invoke(adapter, 1);
            setScanMode.invoke(adapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}