package com.example.ivandimitrov.myapplication;

import android.app.Activity;
import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.users.FullAccount;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Ivan Dimitrov on 1/3/2017.
 */

public class MyService extends Service implements DropBoxConnection.FileReceivedListener {
    private static final String ACCESS_TOKEN = "ZXhz50v7KsAAAAAAAAAADmTbW-e_UBCDw4KY_16Z7VcI2G0WoMi7bBbiqLjV04Rm";

    static final int MSG_REGISTER_CLIENT   = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_SET_INT_VALUE     = 3;
    static final int MSG_SET_STRING_VALUE  = 4;
    static final int MSG_SET_OBJECT_VALUE  = 5;

    private ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    private int progressStep;
    private int curretProgress = 0;
    private int listSize;

    private DropBoxConnection.FileReceivedListener mListener;
    final Messenger mMessenger = new Messenger(new IncomingHandler());


    class IncomingHandler extends Handler { // Handler of incoming messages from clients.
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case MSG_SET_INT_VALUE:
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    public MyService() {
    }


    @Override
    public void onCreate() {
        super.onCreate();
    }


    private void sendMessageToUI(int intvaluetosend) {
        for (int i = mClients.size() - 1; i >= 0; i--) {
            try {
                mClients.get(i).send(Message.obtain(null, MSG_SET_INT_VALUE, intvaluetosend, 0));
            } catch (RemoteException e) {
                mClients.remove(i);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ArrayList<String> list = intent.getExtras().getStringArrayList("data");
        ArrayList<File> selectedFiles = new ArrayList<>();
        mListener = this;
        for (String path : list) {
            selectedFiles.add(new File(path));
        }
        progressStep = 100 / selectedFiles.size();
        listSize = selectedFiles.size();
        new DropBoxConnection(mListener, selectedFiles).execute("");
        return START_STICKY; // run until explicitly stopped.
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onFileReceived(int itemIndex) {
        if (itemIndex == listSize) {
            sendMessageToUI(100);
        } else {
            curretProgress += progressStep;
            sendMessageToUI(curretProgress);
        }
    }
}
