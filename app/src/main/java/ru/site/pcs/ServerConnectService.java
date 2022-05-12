package ru.site.pcs;

import static android.content.ContentValues.TAG;
import static androidx.core.content.PackageManagerCompat.LOG_TAG;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.NetworkOnMainThreadException;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import ru.site.PCS.R;

public class ServerConnectService extends Service {
    private Socket socket = null;
    private BufferedReader socketIn;
    private MyThread myThread;
    private MyHandler myHandler;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            openConnection();
            sendData(generate_time_token().getBytes());
        } catch (Exception ignored) {
        }

        receiver();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void showNotification(String text) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("PCS Service")
                        .setContentText(text);

        Notification notification = builder.build();

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification);
    }

    public void openConnection() {
        try {
            String ServerIP = "192.168.0.100";
            int ServerPort = 48888;
            socket = new Socket(ServerIP, ServerPort);
        } catch (Exception ignored) {
        }
    }

    public void sendData(byte[] data) throws Exception {
        if (socket == null || socket.isClosed()) {
            throw new Exception("Невозможно отправить данные. Сокет не создан или закрыт");
        }
        try {
            socket.getOutputStream().write(data);
            socket.getOutputStream().flush();
        } catch (IOException e) {
            throw new Exception("Невозможно отправить данные: " + e.getMessage());
        }
    }

    @SuppressLint("SetTextI18n")
    public String generate_time_token() {
        @SuppressLint("SimpleDateFormat")
        String date = new SimpleDateFormat("yyyy--MMdd").format(Calendar.getInstance().getTime());
        String s = date + "$KonVi%";
        return to_md5(s);
    }

    private static String to_md5(String st) {
        MessageDigest messageDigest = null;
        byte[] digest = new byte[0];

        try {
            messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.reset();
            messageDigest.update(st.getBytes());
            digest = messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        BigInteger bigInt = new BigInteger(1, digest);
        StringBuilder md5Hex = new StringBuilder(bigInt.toString(16));

        while (md5Hex.length() < 32) {
            md5Hex.insert(0, "0");
        }

        return md5Hex.toString();
    }

    private void receiver() {
        try {
            socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (Exception ignored) {
        }

        myHandler = new MyHandler();
        myThread = new MyThread();
        myThread.start();
    }


    class MyThread extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    String data = socketIn.readLine();
                    Message msg = myHandler.obtainMessage();
                    msg.obj = data;
                    myHandler.sendMessage(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class MyHandler extends Handler {
        public void handleMessage(Message msg) {
            showNotification(msg.obj.toString());
        }
    }

    @SuppressLint("RestrictedApi")
    public void closeConnection() {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Невозможно закрыть сокет: " + e.getMessage());
            } finally {
                socket = null;
            }
        }
        socket = null;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        closeConnection();
    }
}
