package tw.com.flag.TENS_bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Created by ZRD on 2019/3/31.
 */
public class MsgService{
    private static final String NAME="MainActivity";
    private static final UUID MY_UUID=UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    public MsgService(Context context, Handler handler){
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
    }

    private synchronized void setState(int state){
        mState = state;
        mHandler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE,state,-1).sendToTarget();
    }

    public synchronized int getState(){//获取状态
        return mState;
    }

    public synchronized void start(){
        if(mConnectThread !=null){
            mConnectThread.cancel();
            mConnectThread=null;
        }
        if(mConnectedThread !=null){
            mConnectedThread.cancel();
            mConnectedThread=null;
        }
        if (mAcceptThread==null){
            mAcceptThread=new AcceptThread();
            mAcceptThread.start();
        }
        setState(STATE_LISTEN);
    }

    public synchronized void connect(BluetoothDevice device){
        if(mState == STATE_CONNECTED){
            if(mConnectThread !=null){
                mConnectThread.cancel();
                mConnectThread=null;
            }
        }
        if(mConnectedThread !=null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if(mAcceptThread != null){
            mAcceptThread.cancel();
            mAcceptThread =null;
        }
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device){
        if(mConnectedThread !=null){
            mConnectedThread.cancel();
            mConnectedThread=null;
        }
        if(mAcceptThread !=null){
            mAcceptThread.cancel();
            mAcceptThread=null;
        }
        mConnectedThread=new ConnectedThread(socket);
        mConnectedThread.start();

        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.DEVICE_NAME,device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        setState(STATE_CONNECTED);
    }

    public synchronized void stop(){
        if(mConnectThread !=null){
            mConnectThread.cancel();
            mConnectThread=null;
        }

        if(mConnectedThread !=null){
            mConnectedThread.cancel();
            mConnectedThread=null;
        }
        if(mAcceptThread !=null){
            mAcceptThread.cancel();
            mAcceptThread=null;
        }
        setState(STATE_NONE);
    }

    public void write(byte[]out){
        ConnectedThread r;
        synchronized (this){
            if(mState != STATE_CONNECTED)
                return;
            r = mConnectedThread;
        }
        r.write(out);
    }

    private void connectionFailed(){
        setState(STATE_LISTEN);
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle=new Bundle();
        bundle.putString(MainActivity.TOAST,"连接不到设备");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        MsgService.this.start();
    }

    private void connectionLost(){
        setState(STATE_LISTEN);

        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle=new Bundle();
        bundle.putString(MainActivity.TOAST,"设备连接中断了！！！");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        MsgService.this.start();
    }

    private class AcceptThread extends Thread{
        private final BluetoothServerSocket mmServerSocket;
        public AcceptThread(){
            BluetoothServerSocket tmp = null;
            try{
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME,MY_UUID);
            }catch (IOException e){}
            mmServerSocket = tmp;
        }
        public void run(){
            BluetoothSocket socket= null;
            while(mState != STATE_CONNECTED){
                try{
                    socket = mmServerSocket.accept();
                }catch (IOException e) {
                    break;
                }
                if(socket != null){
                    connected(socket,socket.getRemoteDevice());
                    try{
                        mmServerSocket.close();
                    }catch (IOException e){}
                }
            }
        }
        public void cancel(){
            try{
                mmServerSocket.close();
            }catch (IOException e){}
        }
    }

    private class ConnectThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device){
            mmDevice=device;
            BluetoothSocket tmp = null;
            try {
                Method m = device.getClass().getMethod("createRfcommSocket",new Class[]{int.class});
                try{
                    tmp = (BluetoothSocket)m.invoke(device,1);
                }catch (IllegalArgumentException e){
                    e.printStackTrace();
                }catch (IllegalAccessException e){
                    e.printStackTrace();
                }catch (InvocationTargetException e){
                    e.printStackTrace();
                }catch (SecurityException e){
                    e.printStackTrace();
                }
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            mmSocket = tmp;
        }
        public void run(){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mAdapter.cancelDiscovery();
                    try{
                        mmSocket.connect();
                    }catch (IOException e){
                        String TAG = Activity.class.getSimpleName();
                        Log.e(TAG,"!Connection failed::::",e);
                        connectionFailed();
                        try{
                            mmSocket.close();
                        }catch (IOException e2){}
                        return;
                    }
                    synchronized(MsgService.this){
                        mConnectedThread = null;
                    }
                    connected(mmSocket,mmDevice);
                }
            }).start();

        }

        public void cancel(){
           try{
                mmSocket.close();
            }catch (IOException e){}
        }
    }

    private class ConnectedThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket){
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut=null;
            try{
                tmpIn=mmSocket.getInputStream();
                tmpOut=mmSocket.getOutputStream();
            }catch (IOException e){}
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run(){
            ByteBuffer byte_buffer = ByteBuffer.allocate(2048);
            byte_buffer.clear();
            while (true){
                try{
                    byte[] tmp_byte = new byte[mmInStream.available()];
                    byte_buffer.put(tmp_byte);
                    int pos = byte_buffer.position();
                    if( pos>=2 && byte_buffer.get(pos-1) == '\n' && byte_buffer.get(pos-2) == '\r'){
                        byte_buffer.flip();
                        byte[] get_bag = new byte[pos];
                        byte_buffer.get(get_bag);
                        byte_buffer.clear();
                        mHandler.obtainMessage(MainActivity.MESSAGE_READ,pos,-1, get_bag).sendToTarget();
                     }
                }catch (IOException e){
                    connectionLost();
                    break;
                }
            }
        }

        public void write(byte[]buffer){
            try{
                String a = new String(buffer,"GBK");
                mmOutStream.write(a.getBytes("GBK"));
            }catch (IOException e){
                Log.d("MainActivity","Send Fail");
            }
            mHandler.obtainMessage(MainActivity.MESSAGE_WRITE,buffer).sendToTarget();
        }

        public void cancel(){
            try{
                mmSocket.close();
            }catch (IOException e){}
        }
    }
}
