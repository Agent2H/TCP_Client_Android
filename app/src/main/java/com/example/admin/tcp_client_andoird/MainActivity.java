package com.example.admin.tcp_client_andoird;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {
    /*Variables*/
    private static final int ROVER_PORT = 4445;
    private static final String HOST_NAME = "gpsfutureuse.ddns.net";
    private Socket socket;
    private boolean serverConnected = false;
    private boolean serialConnected = false;
    private static DataOutputStream out;

    private TextView textViewLog;
    private Button btnConnectServer,btnConnectSerial;
    private static D2xxManager ftD2xx = null;
    private FT_Device ftDev;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AnhXa();
        SetDefaultText();

        btnConnectServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new connectServerTask().execute();
            }
        });
        btnConnectSerial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               connectSerial();
            }
        });
    }

    private void SetDefaultText() {
        textViewLog.setText("Status Displayed Here");
    }

    private void AnhXa() {
        textViewLog = findViewById(R.id.textLogger);
        btnConnectServer = findViewById(R.id.btnConnectServer);
        btnConnectSerial = findViewById(R.id.btnConnectSerial);
    }

    private void log(String msg) {
        System.out.println(textViewLog.getText());
        LogTask task = new LogTask(textViewLog,msg);
        task.execute();
    }

    private void connectSerial()
    {
        try {
            ftD2xx = D2xxManager.getInstance(this);
        } catch (D2xxManager.D2xxException ex) {
            log(ex.toString());
        }
        int devCount = ftD2xx.createDeviceInfoList(getBaseContext());

        log("Device number : " + Integer.toString(devCount));

        D2xxManager.FtDeviceInfoListNode[] deviceList = new D2xxManager.FtDeviceInfoListNode[devCount];
        ftD2xx.getDeviceInfoList(devCount, deviceList);

        if (devCount <= 0) {
            return;
        }

        if (ftDev == null) {
            ftDev = ftD2xx.openByIndex(getBaseContext(), 0);
            log("Open serial port");
        }

        if (ftDev.isOpen()) {

            ftDev.setBaudRate(9600);
            ftDev.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET);
            ftDev.setDataCharacteristics(D2xxManager.FT_DATA_BITS_8, D2xxManager.FT_STOP_BITS_1, D2xxManager.FT_PARITY_NONE);
            ftDev.setFlowControl(D2xxManager.FT_FLOW_NONE, (byte) 0x0b, (byte) 0x0d);
            log("Set parameters");
            serialConnected = true;
        }
    }





    public class connectServerTask extends AsyncTask<Void,Void,Void>{
        @Override
        protected Void doInBackground(Void... voids) {

            try {
                InetAddress serverAddr = InetAddress.getByName("192.168.100.105");
                socket = new Socket(serverAddr,ROVER_PORT);
                out = new DataOutputStream(socket.getOutputStream());

                log("Connected to Server");
                serialConnected = true;

                /*Add communication thread*/
                (new Thread(new CommunicationThread(socket))).start();

            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e){
                serverConnected = false;
                try{
                    if(out!=null)
                        out.close();
                    if(socket!=null)
                        socket.close();
                } catch (IOException e1){
                    e1.printStackTrace();
                } finally {
                    log(e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    public class CommunicationThread implements Runnable{
        private Socket clientSocket;
        private DataInputStream in;
        private int bytesRead;
        private static final int BUFFER_LENGTH = 4000;
        private byte[] buffer = new byte[BUFFER_LENGTH];

        public CommunicationThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
            try {
                this.in = new DataInputStream(this.clientSocket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while(!Thread.currentThread().isInterrupted()){
                try {
                    System.out.println("Thread is running");
                    bytesRead = in.read(buffer);
                    log("Read"+bytesRead+"bytes");
                    log(new String(buffer,0,bytesRead));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

class LogTask extends AsyncTask<Void,Void,Void>{
    TextView view;
    String msg;

    public LogTask(TextView view, String msg) {
        this.view = view;
        this.msg = msg;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        System.out.println(msg);
        String currentLog = view.getText().toString();
        String newLog = msg + "\n" + currentLog;
        view.setText(newLog);
        super.onPostExecute(aVoid);
    }
}
