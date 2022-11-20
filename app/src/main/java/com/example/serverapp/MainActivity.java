package com.example.serverapp;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.serverapp.databinding.ActivityMainBinding;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    public static String SERVER_IP;
    public static final int SERVER_PORT = 9090;
    String message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        try {
            SERVER_IP = getLocalIpAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        Thread thread1 = new Thread(new Thread1());
        thread1.start();
        binding.btnSend.setOnClickListener(view -> {
            message = binding.etMessage.getText().toString().trim();
            if(!message.isEmpty()) {
                new Thread(new Thread3(message)).start();
            }
        });
    }

    private String getLocalIpAddress() throws UnknownHostException {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipInt = wifiInfo.getIpAddress();
        return InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()).getHostAddress();
    }

    private PrintWriter output;
    private BufferedReader input;
    private class Thread1 implements Runnable {

        @Override
        public void run() {
            Socket socket;
            ServerSocket serverSocket;
            try {
                serverSocket = new ServerSocket(SERVER_PORT);
                runOnUiThread(() -> {
                    binding.tvMessages.setText(getText(R.string.not_connected));
                    binding.tvIP.setText(getString(R.string.IP, SERVER_IP));
                    binding.tvPort.setText(getString(R.string.ports, SERVER_PORT));
                });
                socket = serverSocket.accept();
                output = new PrintWriter(socket.getOutputStream());
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                runOnUiThread(() -> binding.tvMessages.setText(getText(R.string.connected)));
                new Thread(new Thread2()).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class Thread2 implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    final String message = input.readLine();
                    if(message != null) {
                        runOnUiThread(() -> binding.tvMessages.append(getString(R.string.client, message)));
                    } else {
                        Thread thread1 = new Thread(new Thread1());
                        thread1.start();
                        return;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class Thread3 implements Runnable {
        private final String message;
        public Thread3(String message) {
            this.message = message;
        }

        @Override
        public void run() {
            output.write(message + "\n");
            output.flush();
            runOnUiThread(() -> {
                binding.tvMessages.append(getString(R.string.server, message));
                binding.etMessage.setText("");
            });
        }
    }
}