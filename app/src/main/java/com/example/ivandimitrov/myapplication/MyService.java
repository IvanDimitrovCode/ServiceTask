package com.example.ivandimitrov.myapplication;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Ivan Dimitrov on 1/3/2017.
 */

public class MyService extends Service implements DropBoxConnection.FileReceivedListener {
    static final int MSG_REGISTER_CLIENT   = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_SET_INT_VALUE     = 3;
    static final int MSG_SET_STRING_VALUE  = 4;

    static final int MAX_THREADS = 5;

    private static boolean isRunning = false;

    private ArrayList<DropBoxConnection> mThreadPool    = new ArrayList<>();
    private ArrayList<Messenger>         mClients       = new ArrayList<>();
    private Queue<File>                  mSelectedFiles = new LinkedList<>();

    private int mProgressStep;
    private int mCurrentProgress = 0;
    private int mListSize;

    private final Object LOCK = new Object();

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
        isRunning = true;
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
        mListener = this;
        startForeground();
        for (String path : list) {
            mSelectedFiles.add(new File(path));
        }
        mProgressStep = 100 / mSelectedFiles.size();
        mListSize = mSelectedFiles.size();
        startWorkingThreads();
        return START_REDELIVER_INTENT; // run until explicitly stopped.
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("DESTROY", "destroyed");
        isRunning = false;
    }

    private void startWorkingThreads() {
        for (int i = 0; i < MAX_THREADS; i++) {
            mThreadPool.add(new DropBoxConnection(mListener, i));
        }
        for (DropBoxConnection thread : mThreadPool) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                thread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                thread.execute("");
            }
        }
    }

    private void startForeground() {
        int ID = 1234;
        Intent intent = new Intent(this, ServiceTask.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getBaseContext());
        builder.setContentIntent(pendIntent);
        builder.setTicker("CUSTOM MESSAGE");
        builder.setWhen(System.currentTimeMillis());
        builder.setAutoCancel(false);
        builder.setContentTitle("Test Service");
        builder.setContentText("CUSTOM MESSAGE");
        Notification notification = builder.build();
        startForeground(ID, notification);
    }

    private boolean isThreadPoolFinished() {
        for (DropBoxConnection thread : mThreadPool) {
            if (thread.isRunning()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public synchronized File onTaskFinished(DropBoxConnection currentThread) {
        if (mSelectedFiles.isEmpty()) {
            currentThread.stopThread();
            if (isThreadPoolFinished()) {
                sendMessageToUI(100);
                stopSelf();
            }
            return null;
        } else {
            mCurrentProgress += mProgressStep;
            sendMessageToUI(mCurrentProgress);
            return getNextFile();
        }
    }

    private File getNextFile() {
        return mSelectedFiles.poll();
    }

    public static boolean isRunning() {
        return isRunning;
    }
}
