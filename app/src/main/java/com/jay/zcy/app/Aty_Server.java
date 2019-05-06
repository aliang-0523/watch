package com.jay.zcy.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;

/**
 * Created by asus on 2015/12/16.
 */
public class Aty_Server extends Activity {
    public int number;
    private static Sensor sensor1;
    private static Sensor sensor2;
    private static Sensor sensor3;
    private SensorManager sensorManager = null;
    private TextView tvContent;
    private boolean isConnecting;
    private boolean connected;
    private BluetoothAdapter bluetoothAdapter;
    private ServerThread serverThread;
    private Button button;
    public int speedTemp;

    public String str1=null;
    WriteThread writeThread=new WriteThread(str1);
    WriteThread writeThread2=new WriteThread(str1);
    WriteThread writeThread3=new WriteThread(str1);
    public LocationManager mLocationManager;//位置管理器
    private Location getLocation() {
        //查找服务信息
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE); //定位精度: 最高
        criteria.setAltitudeRequired(false); //海拔信息：不需要
        criteria.setBearingRequired(false); //方位信息: 不需要
        criteria.setCostAllowed(true);//是否允许付费
        criteria.setPowerRequirement(Criteria.POWER_LOW); //耗电量: 低功耗
        criteria.setSpeedRequired(true);        // 对速度是否关注
        String provider = mLocationManager.getBestProvider(criteria, true); //获取GPS信息
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    public void requestPermissions(@NonNull String[] permissions, int requestCode)
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for Activity#requestPermissions for more details.
                return null;
            }
        }
        Location location = mLocationManager.getLastKnownLocation(provider);
        mLocationManager.requestLocationUpdates(provider, 200, 1, locationListener);
        return location;
    }
    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            // TODO Auto-generated method stub
            Toast.makeText(Aty_Server.this, "位置更新", Toast.LENGTH_LONG).show();
            if (location != null)
                System.out.println("GPS定位信息:");
            updata(location);
        }

        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub
        }

        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            // TODO Auto-generated method stub

        }
    };
    private void updata(Location location) {
            if (location != null) {
                Toast.makeText(Aty_Server.this, "位置更新", Toast.LENGTH_LONG).show();
                StringBuilder sb = new StringBuilder();
                sb.append("实时的位置信息:\n");
                sb.append("经度:");
                sb.append(location.getLongitude());
                sb.append("\n纬度:");
                sb.append(location.getLatitude());
                sb.append("\b高度:");
                sb.append(location.getAltitude());
                sb.append("\n速度：");
                sb.append(location.getSpeed());
                sb.append("\n方向：");
                sb.append(location.getBearing());
                System.out.println(sb);
                speedTemp = Math.round(location.getSpeed());
            }
        }
    public Aty_Server() {
    }

    private void startGPS() {
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //获取到GPS_PROVIDER
        Location location = getLocation();
        //更新位置信息显示到TextView
        updata(location);
    }
    public int addFileNumber() {
        SharedPreferences sp = Aty_Server.this.getSharedPreferences("sensor", Context.MODE_PRIVATE);
        number = sp.getInt("number", 1);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("number", number + 1);
        editor.commit();
        return number;
    }
    @SuppressLint("MissingPermission")
    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.aty_server);
        tvContent = (TextView) findViewById(R.id.tvContent);
        addFileNumber();
        //获取传感器管理者
        this.sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        sensor1 = this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensor2 = this.sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensor3 = this.sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        listener();
        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                serverThread.cancel();
                Aty_Server.this.finish();
            }
        });


//        File directory = Environment.getExternalStorageDirectory();
//        File file = new File(directory, "or_perceptron.nnet");
//        nnet = NeuralNetwork.load(file.getPath());

    }

//    NeuralNetwork nnet;

    void listener() {
        if (!connected && !isConnecting) {
            Toast.makeText(Aty_Server.this, "开始监听连接", Toast.LENGTH_LONG).show();
            startListen();
            tvContent.append(CurentTimeString.getTime() + "->>" + "正在连接...\n");
        } else if (!connected && isConnecting) {
            Toast.makeText(Aty_Server.this, "正在等待客户端连接", Toast.LENGTH_LONG).show();
        } else if (connected) {
            Toast.makeText(Aty_Server.this, "客户端连接已连接", Toast.LENGTH_LONG).show();
        }
    }


    private void startListen() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled())
            bluetoothAdapter.enable();
        if (!bluetoothAdapter.isDiscovering()) {
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            startActivity(i);
        }

        bluetoothAdapter.startDiscovery();
        //创建一个服务线程  获得服务端socket  如果服务端socket不为空  发送消息:开始监听
        serverThread = new ServerThread(BluetoothAdapter.getDefaultAdapter(), handler);
        //调用start方法 开启一个新的线程 并执行对应的run方法
        //等待客户端链接  获取BluetoothSocket  发送消息：连接成功   sendData();
        serverThread.start();
        isConnecting = true;
    }


    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Config.STATUES_LISTEN_SUCCESS:
                    tvContent.append(CurentTimeString.getTime() + "->>" + "开始监听\n");
                    break;
                case Config.STATUES_LISTEN_FAILED:
                    tvContent.append(CurentTimeString.getTime() + "->>" + "监听失败\n");
                    break;
                case Config.STATUES_CONNECT_SUCCESS:
                    startGPS();
                    connected = true;
                    tvContent.append(CurentTimeString.getTime() + "->>" + "连接成功\n");
                    sendData();
                    break;
                case Config.STATUES_CONNECT_FAILED:
                    connected = true;
                    tvContent.append(CurentTimeString.getTime() + "->>" + "连接失败\n");
                    break;
                case Config.STATUES_READ_FAILED:
                    tvContent.append(CurentTimeString.getTime() + "->>" + "读取失败\n");
                    break;
                case Config.STATUES_READ_SUCCESS:
                    tvContent.append(CurentTimeString.getTime() + "->>接收：" + msg.obj.toString() + "\n");
                    break;
                case Config.STATUES_WRITE_FAILED:
                    //tvContent.append(CurentTimeString.getTime() + "->>" + "写入失败\n");
                    break;
                case Config.STATUES_WRITE_SUCCESS:
                    tvContent.setText(CurentTimeString.getTime() + "->>发送：" + msg.obj.toString() +
                            "    ---->>>>发送成功\n");
                    System.out.println(msg.obj.toString());
                    break;
            }
        }
    };

    private void sendData() {
        this.sensorManager.registerListener(this.listener, sensor1, SensorManager.SENSOR_DELAY_NORMAL);
        this.sensorManager.registerListener(this.listener, sensor2, SensorManager.SENSOR_DELAY_NORMAL);
        this.sensorManager.registerListener(this.listener, sensor3, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public class WriteThread extends Thread{
        public String str;
        public void setText(String text){
            this.str=text;
        }
        WriteThread(String str){
            this.str=str;
        }
        @Override
        public void run() {
            if(serverThread.getbluetoothSocket()!=null){
                Message msg=new Message();
                try{
                    if(serverThread.out==null)
                        serverThread.out=serverThread.getbluetoothSocket().getOutputStream();
                    byte[] bytes=this.str.getBytes();
                    serverThread.out.write(bytes,0,bytes.length);
                    serverThread.out.flush();
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
    }
    private SensorEventListener listener = new SensorEventListener() {

        @SuppressLint("MissingPermission")
        public void onSensorChanged(SensorEvent e) {
            if (e.sensor.equals(sensor1)) {
                //相当于一个string  空的字符串 处理字符串时stringBuffer优于String
                //StringBuffer类中的方法主要偏重于对于字符串的变化，例如追加、插入和删除等，这个也是StringBuffer和String类的主要区别。


                //  s.append("sensor1 " + CurentTimeString.getTime() + " " +e.values[0] + " " + e.values[1] + " "+e.values[2] +"\n");

                writeThread.setText("sensor1 " + CurentTimeString.getTime() + " " + Math.round(e.values[0] * 100) + " " + Math.round(e.values[1] * 100) + " " + Math.round(e.values[2] * 100) +" "+"\n");
                //向客户端发送数据
                //ClientThread clientThread=new ClientThread(Aty_Server.this,s.toString());
                //clientThread.run();
                writeThread.run();
            }
            if (e.sensor.equals(sensor2)) {
                //相当于一个string  空的字符串 处理字符串时stringBuffer优于String
                //StringBuffer类中的方法主要偏重于对于字符串的变化，例如追加、插入和删除等，这个也是StringBuffer和String类的主要区别。


                //  s.append("sensor1 " + CurentTimeString.getTime() + " " +e.values[0] + " " + e.values[1] + " "+e.values[2] +"\n");

                writeThread2.setText("sensor2 " + CurentTimeString.getTime() + " " + Math.round(e.values[0] * 100) + " " + Math.round(e.values[1] * 100) + " " + Math.round(e.values[2] * 100) +" "+"\n");
                //向客户端发送数据
                //ClientThread clientThread=new ClientThread(Aty_Server.this,s.toString());
                //clientThread.run();
                writeThread2.run();
            }
            if (e.sensor.equals(sensor3)) {
                //相当于一个string  空的字符串 处理字符串时stringBuffer优于String
                //StringBuffer类中的方法主要偏重于对于字符串的变化，例如追加、插入和删除等，这个也是StringBuffer和String类的主要区别。


                //  s.append("sensor1 " + CurentTimeString.getTime() + " " +e.values[0] + " " + e.values[1] + " "+e.values[2] +"\n");

                writeThread3.setText("sensor3 " + CurentTimeString.getTime() + " " + Math.round(e.values[0] * 100) + " " + Math.round(e.values[1] * 100) + " " + Math.round(e.values[2] * 100) +" "+"\n");
                //向客户端发送数据
                //ClientThread clientThread=new ClientThread(Aty_Server.this,s.toString());
                //clientThread.run();
                writeThread3.run();
            }

            //            else if (e.sensor.equals(sensor2)) {
            //
            //                StringBuffer s = new StringBuffer();
            //                s.append("sensor2 " + e.values[0] +  " " + e.values[1]  + " " + e.values[2] +"\n");
            //               serverThread.write(s.toString());
            //            }else if (e.sensor.equals(sensor3)) {
            //
            //                StringBuffer s = new StringBuffer();
            //                s.append("sensor3 " + e.values[0] + " "  + e.values[1] + " " + " " + e.values[2] + "\n");
            //                serverThread.write(s.toString());
            //            }
        }

        public void onAccuracyChanged(Sensor arg0, int arg1) {
        }
    };
}