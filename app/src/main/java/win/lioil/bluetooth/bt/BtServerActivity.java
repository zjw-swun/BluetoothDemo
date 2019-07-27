package win.lioil.bluetooth.bt;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.dave.wifirealtimespeeker.audio.SpeexTalkPlayer;
import com.dave.wifirealtimespeeker.audio.SpeexTalkRecorder;

import java.io.File;

import win.lioil.bluetooth.APP;
import win.lioil.bluetooth.R;

public class BtServerActivity extends Activity implements BtBase.Listener {
    private TextView mTips;
    private EditText mInputMsg;
    private EditText mInputFile;
    private TextView mLogs;
    private BtServer mServer;
    private SpeexTalkRecorder mSpeexTalkRecorder;
    private SpeexTalkPlayer mSpeexTalkPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_btserver);
        mTips = findViewById(R.id.tv_tips);
        mInputMsg = findViewById(R.id.input_msg);
        mInputFile = findViewById(R.id.input_file);
        mLogs = findViewById(R.id.tv_log);
        mServer = new BtServer(this);

        mSpeexTalkRecorder = new SpeexTalkRecorder(new SpeexTalkRecorder.onRecorderListener() {
            @Override
            public void handleRecordData(byte[] recordData) {
                mServer.sendAudio(recordData);
            }
        });

        mSpeexTalkPlayer = new SpeexTalkPlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSpeexTalkPlayer.release();
        mSpeexTalkRecorder.release();
        mServer.unListener();
        mServer.close();
    }

    public void sendMsg(View view) {
        if (mServer.isConnected(null)) {
            String msg = mInputMsg.getText().toString();
            if (TextUtils.isEmpty(msg))
                APP.toast("消息不能空", 0);
            else
                mServer.sendMsg(msg);
        } else
            APP.toast("没有连接", 0);
    }

    public void sendFile(View view) {
        if (mServer.isConnected(null)) {
            String filePath = mInputFile.getText().toString();
            if (TextUtils.isEmpty(filePath) || !new File(filePath).isFile())
                APP.toast("文件无效", 0);
            else
                mServer.sendFile(filePath);
        } else
            APP.toast("没有连接", 0);
    }

    public void sendAudio(View view) {
        if (mServer.isConnected(null)) {
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
                mServer.listen();
                msg = "连接断开,正在重新监听...";
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
        APP.toast(msg, 0);
    }
}