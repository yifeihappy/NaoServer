package com.example.yifeihappy.naoserver;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {
    ServerSocket serverSocket = null;
    MediaPlayer cryPlayer = null;//哭闹提示
    MediaPlayer openPlayer = null;//
    MediaPlayer closePlayer = null;//
    Button cancleBtn = null;
    Button continueBtn = null;
    volatile boolean continueState = true;//监听状态

    TextView localIPTxt = null;
    TextView myPORTTxt = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cryPlayer = MediaPlayer.create(this, R.raw.sky);
        openPlayer = MediaPlayer.create(this, R.raw.qopen);
        closePlayer = MediaPlayer.create(this, R.raw.qclose);

        cancleBtn = (Button)findViewById(R.id.cancleBtn);
        continueBtn = (Button)findViewById(R.id.continueBtn);

        localIPTxt = (TextView)findViewById(R.id.localIPTxt);
        myPORTTxt = (TextView)findViewById(R.id.myPORTTxt);

        localIPTxt.setText(getHostIP());
        if(continueState){
            cancleBtn.setEnabled(true);
            continueBtn.setEnabled(false);
        } else {
            continueBtn.setEnabled(true);
            cancleBtn.setEnabled(false);
        }


        cancleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(cryPlayer.isPlaying()){
                    cryPlayer.pause();
                    try {
                        cryPlayer.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if(closePlayer.isPlaying()){
                    closePlayer.pause();
                    try {
                        closePlayer.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if(openPlayer.isPlaying()){
                    openPlayer.pause();
                    try {
                        openPlayer.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                continueState = false;
                Message msg = new Message();
                msg.what = 0x000;
                handler.sendMessage(msg);
            }
        });

        continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                continueState = true;
                Message msg = new Message();
                msg.what = 0x001;
                handler.sendMessage(msg);
            }
        });
        new NaoReceiveThread().start();
    }

    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 0x000:
                    Toast.makeText(MainActivity.this, "取消监听", Toast.LENGTH_LONG).show();
                    continueBtn.setEnabled(true);
                    cancleBtn.setEnabled(false);
                    break;
                case 0x001:
                    Toast.makeText(MainActivity.this, "开始监听", Toast.LENGTH_LONG).show();
                    continueBtn.setEnabled(false);
                    cancleBtn.setEnabled(true);
                    break;
                case 0x002:
                    Toast.makeText(MainActivity.this, "打开服务器失败!", Toast.LENGTH_LONG).show();
                    break;
                case 0x003:
                    Toast.makeText(MainActivity.this, "读取nao机器人数据失败!", Toast.LENGTH_LONG).show();
            }
        }
    };

    class NaoReceiveThread extends Thread{
        @Override
        public void run() {
            super.run();
            try {
                serverSocket = new ServerSocket(3000);
                while (true){
                    Log.d("dd","server socket open");
                    Socket s = serverSocket.accept();
                    Log.d("dd","收到数据");
                    if(continueState) {
                        new Thread(new naoMessageThread(s)).start();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //读取nao消息线程
    class naoMessageThread implements Runnable{
        private Socket s = null;
        private BufferedReader br = null;
        public naoMessageThread(Socket socket){
            this.s = socket;
            try {
                br = new BufferedReader(new InputStreamReader(s.getInputStream(), "utf-8"));
            } catch (IOException e) {
                e.printStackTrace();
                Message msg = new Message();
                msg.what = 0x002;
                msg.obj = "读取nao机器人数据失败!";
            }
        }
        @Override
        public void run() {
            String content;
//            while((content=readFromClient()) != null){
//                Log.d("", content);
//                if(content.equals("open")){
//                    openPlayer.start();
//                } else if(content.equals("close")){
//                    closePlayer.start();
//                } else if(content.equals("cry")){
//                    cryPlayer.start();
//                } else {
//                    Log.d("dd","收到无效指令："+content);
//                }
//            }
            Log.d("dd", "after while");
            if((content=readFromClient()) != null) {
                Log.d("", content);
                if(content.equals("open")){
                    openPlayer.seekTo(0);
                    openPlayer.start();
                } else if(content.equals("close")){
                    closePlayer.seekTo(0);
                    closePlayer.start();
                } else if(content.equals("cry")){
                    cryPlayer.seekTo(0);
                    cryPlayer.start();
                } else {
                    Log.d("dd","收到无效指令："+content);
                }
            } else {
                Log.d("dd", "content == null");
            }
        }
        private String readFromClient(){
            try {
                return br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("asd", "nao client 退出");
            }
            return null;
        }
    }

    /**
     * 获取ip地址
     * @return
     */
    public static String getHostIP() {

        String hostIp = null;
        try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            InetAddress ia = null;
            while (nis.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements()) {
                    ia = ias.nextElement();
                    if (ia instanceof Inet6Address) {
                        continue;// skip ipv6
                    }
                    String ip = ia.getHostAddress();
                    if (!"127.0.0.1".equals(ip)) {
                        hostIp = ia.getHostAddress();
                        break;
                    }
                }
            }
        } catch (SocketException e) {
            Log.i("yao", "SocketException");
            e.printStackTrace();
        }
        return hostIp;

    }
}
