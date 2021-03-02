package tw.com.flag.TENS_bluetooth;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
/**
 * Created by ZRD on 2019/4/30.
 */
public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener{
    private TextView txv;
    private int cur_bar_v;
    private int cur_bar_f;
    private int cur_bar_w;
    private int flag_out=0;
    private int flag_adc=0;
    private Button  connect, disconnect;
    private Button open, stop;
    private SeekBar mseekbar_v;
    private SeekBar mseekbar_f;
    private SeekBar mseekbar_w;
    private Button up,down;
    private Button sin,rec,tri,con,uncon;
    private CheckBox mIfcurrent;
    private Button output_ok;

    public int safe_v = 0;
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    public static final int REQUEST_CONNECT_DEVICE = 1;
    public static final int REQUEST_ENABLE_BT = 2;

    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;
    private String mConnectedDeviceName = null;
    private ArrayAdapter<String> mConversationArrayAdapter;
    private StringBuffer mOutStringBuffer;
    private BluetoothAdapter mBluetoothAdapter;
    private MsgService mMsgService = null;

    View view;
    private LinearLayout view_Layout;
    private WaveView mWaveView;

    mODE45 ode45 = new mODE45();
    boolean mMethod1OPen = false;
    private int mMethod_chanel = 0;
    final Handler timer_handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            mWaveView.drawWave(msg.what);
            int tmp = msg.what;
            String tmp_s = 'G'+intToHex(tmp,3)+"+o";
            byte[] send = tmp_s.getBytes();
            mMsgService.write(send);
        }
    };

    private static String intToHex(int n,int size) {
        StringBuffer s = new StringBuffer();
        String a;
        char []b = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        while(n != 0){
            s = s.append(b[n%16]);
            n = n/16;
        }
        a = s.reverse().toString();
        a  = add_zore(a,size);
        return a;
    }
    public static String add_zore(String str, int size){
        if (str.length()<size){
            str= "0"+str;
            str=add_zore(str,size);
            return str;
        }else {
            return str;
        }
    }
    Timer timer = new Timer();
    class MyTask extends TimerTask{
        @Override
        public void run() {
            if (mMethod1OPen){
                ode45.DomODE45();
                double tmp=0.0;
                switch (mMethod_chanel) {
                    case 0:
                        tmp = 4095 - (ode45.x / 40 + 0.5) * 4095;
                        break;
                    case 1:
                        tmp = 4095 - (ode45.y / 50 + 0.5) * 4095;
                        break;
                    case 2:
                        tmp = 4095 - (ode45.z / 50) * 4095;
                        break;
                    default:
                        tmp = 4095 - (ode45.y / 50 + 0.5) * 4095;
                        break;
                }
                int tmp_x = (int) tmp;
                if(tmp_x>4095) tmp_x = 4095;
                if(tmp_x<0) tmp_x = 0;
                timer_handler.sendEmptyMessage(tmp_x);
            }
        }
    }
    MyTask task = new MyTask();
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        SetLocation();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "本设备不支持蓝牙！", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        txv = (TextView) findViewById(R.id.txv);

        connect = (Button) findViewById(R.id.connect);
        disconnect = (Button) findViewById(R.id.disconnect);
        stop = (Button) findViewById(R.id.stop);
        open = (Button) findViewById(R.id.open);
        up = (Button) findViewById(R.id.up);
        down = (Button) findViewById(R.id.down);

        mseekbar_v = (SeekBar)findViewById(R.id.seekBar);
        mseekbar_v.setOnSeekBarChangeListener(this);
        mseekbar_f = (SeekBar)findViewById(R.id.seekBar_F);
        mseekbar_f.setOnSeekBarChangeListener(this);
        mseekbar_w = (SeekBar)findViewById(R.id.seekBar_width);
        mseekbar_w.setOnSeekBarChangeListener(this);

        sin = (Button)findViewById(R.id.button_sin);
        rec = (Button)findViewById(R.id.button_rec);
        tri = (Button)findViewById(R.id.button_tri);
        con = (Button)findViewById(R.id.button_con);
        uncon = (Button)findViewById(R.id.button_uncon);

        sin.setOnClickListener(new ButtonListener());
        rec.setOnClickListener(new ButtonListener());
        tri.setOnClickListener(new ButtonListener());
        con.setOnClickListener(new ButtonListener());
        uncon.setOnClickListener(new ButtonListener());

        mIfcurrent = (CheckBox) findViewById(R.id.checkBox_detect);
        mIfcurrent.setOnCheckedChangeListener(new IsCurrent_listen());


        output_ok = (Button)findViewById(R.id.button_output);
        output_ok.setOnClickListener(new ButtonListener());



        mSendButton = (Button) findViewById(R.id.send_button);

        up.setOnClickListener(new ButtonListener());
        down.setOnClickListener(new ButtonListener());
        stop.setOnClickListener(new ButtonListener());
        open.setOnClickListener(new ButtonListener());
        connect.setOnClickListener(new ButtonListener());
        disconnect.setOnClickListener(new ButtonListener());
        mSendButton.setOnClickListener(new ButtonListener());

        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.list_item);
        mConversationView = (ListView) findViewById(R.id.listView);
        mConversationView.setAdapter(mConversationArrayAdapter);
        mOutEditText = (EditText) findViewById(R.id.editText);
        view = getWindow().getDecorView();
        initView();
        WaveThread mwavethread = new WaveThread();
        mwavethread.start();

        timer.schedule(task, 1000, 10);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }


    private void initView() {
        view.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener(){
                    @Override
                    public void onGlobalLayout(){
                        view_Layout = (LinearLayout) view.findViewById(R.id.graph);
                        int width = view_Layout.getWidth();
                        int height = view_Layout.getHeight();
                        mWaveView = new WaveView(getBaseContext(), width, height);
                        mWaveView.init();
                        view_Layout.addView(mWaveView);
                        view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }
                }
        );
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        switch (seekBar.getId()) {
            case R.id.seekBar:
                cur_bar_v = i;
                break;
            case R.id.seekBar_F:
                cur_bar_f = i;
                break;
            case R.id.seekBar_width:
                cur_bar_w = i;
                break;
            default:
                break;
        }
    }
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        //Toast.makeText(this, "正在修改电压值！", Toast.LENGTH_LONG).show();
    }
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        String tmp;
        switch (seekBar.getId()){
            case R.id.seekBar:
                tmp =Integer.toString(cur_bar_v);
                if (cur_bar_v / 10 == 0){
                    sendMessage("Gamp:20"+tmp+"+o\n");
                }else{
                    sendMessage("Gamp:2"+tmp+"+o\n");
                }
                break;
            case R.id.seekBar_F:
                tmp =Integer.toString(cur_bar_f);
                if (cur_bar_f / 10 == 0){
                    sendMessage("Gfre:20"+tmp+"+o\n");
                }else{
                    sendMessage("Gfre:2"+tmp+"+o\n");
                }
                break;
            case R.id.seekBar_width:
                tmp =Integer.toString(cur_bar_w);
                if (cur_bar_w / 10 == 0){
                    sendMessage("Gwid:20"+tmp+"+o\n");
                }else{
                    sendMessage("Gwid:2"+tmp+"+o\n");
                }
                break;
            default:
                break;
        }
    }

    private class WaveThread extends Thread {
        public WaveThread() {
        }
        @Override
        public void run() {
            //while (true){
                //mWaveView.drawWave(7);//发送参数
            //}  
        }
    }

    public void SetLocation() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 200);
            } else {
                Toast.makeText(this, "成功获取定位权限！", Toast.LENGTH_LONG).show();
            }
        } else {
            LocationManager manager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);
            boolean isGpsProvider = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetWorkProvider = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (isGpsProvider || isNetWorkProvider == true) {
                Toast.makeText(this, "开启系统定位服务成功！", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "无法开启定位服务，请手动设置！", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 200:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "您已同意开启定位权限！", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "未开启定位权限,请手动到设置去开启权限", Toast.LENGTH_LONG).show();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.scan:
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(turnOn, REQUEST_ENABLE_BT);
                }
                Intent serverIntent = new Intent(MainActivity.this, DeviceList.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                return true;
            case R.id.discoverable:
                ensureDiscoverable();
                return true;
            case R.id.BtOpen:
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(turnOn, REQUEST_ENABLE_BT);
                }
                return true;
            case R.id.BtOff:
                if (mMsgService != null)
                    mMsgService.stop();
                mBluetoothAdapter.disable();
                txv.setText("未连接！");
                return true;
        }
        return false;
    }

    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }
    private void delayms(int ms){
        try{
            Thread.currentThread();
            Thread.sleep(ms);
        }catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private class ButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.connect:
                    Intent serverIntent = new Intent(MainActivity.this, DeviceList.class);
                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                    break;
                case R.id.disconnect:
                    sendMessage("Gctl:10+o\n");
                    if (mMsgService != null)
                        mMsgService.stop();
                    txv.setText("未连接！");
                    break;
                case R.id.up:
                    sendMessage("Gamp:299+o\n");
                    mseekbar_v.setProgress(99);
                    delayms(100);
                    sendMessage("Gfre:299+o\n");
                    mseekbar_f.setProgress(99);
                    delayms(100);
                    sendMessage("Gwid:299+o\n");
                    mseekbar_w.setProgress(99);
                    delayms(100);
                    sendMessage("Gadc:10+o\n");
                    mIfcurrent.setChecked(false);
                    break;
                case R.id.down:
                    sendMessage("Gamp:200+o\n");
                    mseekbar_v.setProgress(0);
                    delayms(100);
                    sendMessage("Gfre:200+o\n");
                    mseekbar_f.setProgress(0);
                    delayms(100);
                    sendMessage("Gwid:200+o\n");
                    mseekbar_w.setProgress(0);
                    delayms(100);
                    sendMessage("Gadc:10+o\n");
                    mIfcurrent.setChecked(false);
                    break;
                case R.id.open:
                    sendMessage("Gctl:11+o\n");
                    break;
                case R.id.stop:
                    sendMessage("Gctl:10+o\n");
                    break;
                case R.id.button_sin:
                    sendMessage("Gwav:10+o\n");
                    break;
                case R.id.button_rec:
                    sendMessage("Gwav:11+o\n");
                    break;
                case R.id.button_tri:
                    sendMessage("Gwav:12+o\n");
                    break;
                case R.id.button_con:
                    if (mMethod1OPen == false){
                        sendMessage("Gwav:13+o\n");
                        mMethod1OPen = true;
                    }else {
                        mMethod1OPen = false;
                        sendMessage("Gooo+o\n");
                        sendMessage("Gooo+o\n");
                        sendMessage("Gooo+o\n");
                        sendMessage("Gooo+o\n");
                        sendMessage("Gooo+o\n");
                        sendMessage("Gooo+o\n");
                    }
                    break;
                case R.id.button_uncon:
                    if(mMethod_chanel<2)
                        mMethod_chanel++;
                    else
                        mMethod_chanel=0;
                    break;
                case R.id.button_output:
                    if (flag_out == 0){
                        sendMessage("Gout:11+o\n");
                        output_ok.setText("STOP");
                        flag_out = 1;
                    }else{
                        sendMessage("Gout:10+o\n");
                        delayms(100);
                        sendMessage("Gout:10+o\n");
                        delayms(100);
                        sendMessage("Gout:10+o\n");
                        delayms(100);
                        sendMessage("Gout:10+o\n");
                        delayms(100);
                        sendMessage("Gout:10+o\n");
                        output_ok.setText("OUTPUT");
                        flag_out = 0;
                    }
                    break;
                case R.id.send_button:
                    String message = mOutEditText.getText().toString();
                    sendMessage(message+"+o\n");
                    break;
                default:
                    sendMessage("No define error!");
                    break;
            }
        }
    }

    private class IsCurrent_listen implements CheckBox.OnCheckedChangeListener{
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked){
                sendMessage("Gadc:11+o\n");
                flag_adc = 1;

            }else{
                sendMessage("Gadc:10+o\n");
                flag_adc = 0;
            }
        }
    }

    private void setupChat() {
        mMsgService = new MsgService(this, mHandler);
        mOutStringBuffer = new StringBuffer("");
        mOutEditText.setText("");
    }

    @Override
    public void onStart() {
        super.onStart();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            if (mMsgService == null)
                setupChat();
        }
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://tw.com.flag.TENS_bluetooth/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (mMsgService != null)
            if (mMsgService.getState() == MsgService.STATE_NONE)
                mMsgService.start();
    }

    @Override
    public synchronized void onPause() {//中断
        super.onPause();
    }

    @Override
    public synchronized void onStop() {
        super.onStop();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://tw.com.flag.TENS_bluetooth/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.disconnect();
    }

    @Override
    public synchronized void onDestroy() {
        super.onDestroy();
        if (mMsgService != null)
            mMsgService.stop();
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case MsgService.STATE_CONNECTED:
                            mConversationArrayAdapter.clear();
                            break;
                        case MsgService.STATE_CONNECTING:
                            break;
                        case MsgService.STATE_LISTEN:
                            break;
                        case MsgService.STATE_NONE:
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    String writeMessage = new String(writeBuf);
                    if (mMethod1OPen == false)
                    mConversationArrayAdapter.add("本地：" + writeMessage);
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    try {
                        byte[] msg_byte = readMessage.getBytes("UTF-8");
                        String msg_tmp = new String(msg_byte, "GBK");
                        if(msg_byte[0]=='A' && msg_byte[1]=='D'&& msg_byte[2]=='C' && msg_byte[3]==':'){
                            safe_v = 4096-((msg_byte[4]-'0')*1000+(msg_byte[5]-'0')*100+(msg_byte[6]-'0')*10+(msg_byte[7]));//
                            mWaveView.drawWave(safe_v);
                        }else {
                            mConversationArrayAdapter.add(mConnectedDeviceName + ": " + msg_tmp);
                        }
                    } catch (IOException e) {
                    }

                    break;
                case MESSAGE_DEVICE_NAME:
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "连接到" + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    txv.setText("连接到" + mConnectedDeviceName);
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public void onActivityResult(int requesstCode, int resultCode, Intent data) {
        switch (requesstCode) {
            case REQUEST_CONNECT_DEVICE:
                if (resultCode == Activity.RESULT_OK) {
                    String address = data.getExtras().getString(DeviceList.EXTRA_DEVICE_ADDRESS);
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    mMsgService.connect(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    setupChat();
                } else {
                    Toast.makeText(this, "请先开启蓝牙！", Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    public void sendMessage(String message) {
        if (mMsgService.getState() != MsgService.STATE_CONNECTED) {
            Toast.makeText(this, "未连接！", Toast.LENGTH_SHORT).show();
            txv.setText("未连接！");
            return;
        }
        if (message.length() > 0) {
            byte[] send = "".getBytes();
            try {
                send = message.getBytes();
            } catch (Exception e) {
                System.out.println("转换异常!\n");
            }
            mMsgService.write(send);
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }

}

