package tw.com.flag.TENS_bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

/**
 * Created by ZRD on 2019/3/31.
 */
public class DeviceList extends AppCompatActivity {
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    private BluetoothAdapter mBtAdapter;

    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;

    private IntentFilter filter = new IntentFilter();

    private ProgressBar progressBar;
    private Button scanButton;
    private ListView pairedListView;
    private ListView newDeviceListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_list);
        setResult(Activity.RESULT_CANCELED);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        scanButton = (Button)findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {//监听按键扫描事件
            @Override
            public void onClick(View view) {
                doDiscovery();
                view.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
            }
        });
        mPairedDevicesArrayAdapter=new ArrayAdapter<String>(this,R.layout.list_item);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this,R.layout.list_item);

        pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        newDeviceListView = (ListView)findViewById(R.id.new_devices);
        newDeviceListView.setAdapter(mNewDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickedListen);
        newDeviceListView.setOnItemClickListener(mDeviceClickedListen);

        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice>pairedDevices = mBtAdapter.getBondedDevices();
        if(pairedDevices.size()>0){
            for (BluetoothDevice Device : pairedDevices){
                mPairedDevicesArrayAdapter.add(Device.getName()+"\n"+Device.getAddress());
            }
        }else{
            mPairedDevicesArrayAdapter.add("No Devices!");
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if (mBtAdapter!=null){
            mBtAdapter.cancelDiscovery();
        }
        this.unregisterReceiver(mReceiver);
    }
    private void doDiscovery(){
        if (mBtAdapter.isDiscovering()){
            mBtAdapter.cancelDiscovery();
        }
        mBtAdapter.startDiscovery();
    }

    private AdapterView.OnItemClickListener mDeviceClickedListen = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            mBtAdapter.cancelDiscovery();
            String info =((TextView)view).getText().toString();
            String address = info.substring(info.length()-17);
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS,address);
            setResult(Activity.RESULT_OK,intent);
            finish();
        }
    };

    private  final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getBondState() != BluetoothDevice.BOND_BONDED){
                    mNewDevicesArrayAdapter.add(device.getName()+"\n"+device.getAddress());
                }
            }else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                progressBar.setVisibility(View.GONE);
                scanButton.setVisibility(View.VISIBLE);
                Toast.makeText(DeviceList.this,"搜索完毕",Toast.LENGTH_SHORT).show();
                if (mNewDevicesArrayAdapter.getCount() == 0){
                    mNewDevicesArrayAdapter.add("No Devices!");
                }
            }
        }
    };
   private static final String TAG = Activity.class.getSimpleName();
    @Override
    public void onBackPressed() {
        Log.i(TAG, "onBackPressed");
        setResult(Activity.RESULT_CANCELED);
        super.onBackPressed();
    }
}
