package com.jay.zcy.app;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by fan on 15-12-6.
 */
public class ServerThread extends Thread {
    private BluetoothAdapter adapter;
    private Handler handler;
    private BluetoothServerSocket serverSocket;
    private boolean getSokcet;
    private boolean isStopReading;
    private InputStream in;
    public OutputStream out;
    private BluetoothSocket bluetoothSocket;

    public OutputStream getOut(){
        return this.out;
    }
    public BluetoothSocket getbluetoothSocket(){
        return this.bluetoothSocket;
    }
    public ServerThread(BluetoothAdapter adapter, Handler handler) {
        this.adapter = adapter;
        this.handler = handler;
        try {

            //获得服务端socket
            serverSocket = adapter.listenUsingRfcommWithServiceRecord("MyApp", UUID.fromString(Config.UUID));
        } catch (IOException e) {
            Log.e("TAG","serverSoket create failed");
            handler.sendEmptyMessage(Config.STATUES_LISTEN_FAILED);
        }
        if (serverSocket != null) {

            handler.sendEmptyMessage(Config.STATUES_LISTEN_SUCCESS);
            Log.e("TAG", "running");
        }
        else
            handler.sendEmptyMessage(Config.STATUES_LISTEN_FAILED);
    }

    @Override
    public void run() {
        while (!getSokcet) {
            try {
                //等待客户端链接
                bluetoothSocket = serverSocket.accept();
                getSokcet = true;
                handler.sendEmptyMessage(Config.STATUES_CONNECT_SUCCESS);
            } catch (IOException e) {
                e.printStackTrace();
                handler.sendEmptyMessage(Config.STATUES_CONNECT_FAILED);
            }
        }
        //开始读取数据
       // startRead(bluetoothSocket);
    }

   /* private void startRead(final BluetoothSocket socket) {
        System.out.println("sout->>reading");
        while (!isStopReading) {
            if(socket==null) {

                handler.sendEmptyMessage(Config.STATUES_READ_FAILED);
                Log.e("TAG","连接丢失");
                continue;
            }
            try {
                sleep(100);
                if (in == null)
                    in = socket.getInputStream();
                byte[] bytes=new byte[in.available()];
                if(in.read(bytes,0,in.available())>0){

                    //通过handle将读取到的数据发出去
                    Message msg=new Message();
                    msg.what=Config.STATUES_READ_SUCCESS;
                    msg.obj=new String(bytes);
                    handler.sendMessage(msg);

                }
            } catch (IOException e) {
                e.printStackTrace();
                handler.sendEmptyMessage(Config.STATUES_READ_FAILED);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }*/

    public void write(String str){
        if(bluetoothSocket!=null){
            Message msg=new Message();
            try{
                if(out==null)
                    out=bluetoothSocket.getOutputStream();
                byte[] bytes=str.getBytes();
                out.write(bytes,0,bytes.length);
                out.flush();


                msg.what=Config.STATUES_WRITE_SUCCESS;
                msg.obj=str;
                handler.sendMessage(msg);
            } catch (IOException e) {
                e.printStackTrace();
                handler.sendEmptyMessage(Config.STATUES_WRITE_FAILED);
            }
        }else{
            Log.e("TAG","bluetoothSocket null");
            handler.sendEmptyMessage(Config.STATUES_WRITE_FAILED);
        }
    }

    public void cancel() {
        isStopReading = true;
        getSokcet=true;
        if(serverSocket!=null)
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }
}