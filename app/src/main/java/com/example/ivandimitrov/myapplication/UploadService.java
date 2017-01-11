package com.example.ivandimitrov.myapplication;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Ivan Dimitrov on 1/3/2017.
 */

public class UploadService extends Service implements DropBoxTask.FileReceivedListener {
    static final int MSG_REGISTER_CLIENT         = 1;
    static final int MSG_UNREGISTER_CLIENT       = 2;
    static final int MSG_SET_PROGRESS_PERCENTAGE = 3;
    static final int MSG_SET_FILE_INDEX          = 4;
    static final int MSG_SET_STRING_VALUE        = 5;

    static final int MAX_THREADS = 3;

    private static boolean isRunning = false;

    private ArrayList<DropBoxTask> mThreadPool        = new ArrayList<>();
    private ArrayList<Messenger>   mClients           = new ArrayList<>();
    private Queue<File>            mSelectedFiles     = new LinkedList<>();
    private ArrayList<File>        mSelectedFilesList = new ArrayList<>();

    private int mProgressStep;
    private int mCurrentProgress = 0;
    private int mListSize;

    private final Object LOCK = new Object();

    private DropBoxTask.FileReceivedListener mListener;
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    public UploadService() {
    }

    //STATIC METHODS
    public static boolean isRunning() {
        return isRunning;
    }

    //CALLBACK FUNCTIONS
    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mThreadPool = new ArrayList<>();
        mClients.clear();
        mSelectedFiles.clear();
        mSelectedFilesList.clear();

        ArrayList<String> list = intent.getExtras().getStringArrayList("data");
        mListener = this;
        startForeground();
        for (String path : list) {
            mSelectedFiles.add(new File(path));
        }
        mSelectedFilesList.addAll(mSelectedFiles);
        isRunning = true;
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

    //OTHER FUNCTIONS
    private void sendMessageToUI(int valueToSend, File uploadedFile) {
        for (int i = mClients.size() - 1; i >= 0; i--) {
            try {
                mClients.get(i).send(Message.obtain(null, MSG_SET_PROGRESS_PERCENTAGE, valueToSend, 0));
                Bundle b = new Bundle();
                b.putString("filePath", uploadedFile.getAbsolutePath());
                Message msg = Message.obtain(null, MSG_SET_STRING_VALUE);
                msg.setData(b);
                mClients.get(i).send(msg);
            } catch (RemoteException e) {
                mClients.remove(i);
            }
        }
    }

    private void startWorkingThreads() {
        int corePoolSize = 60;
        int maximumPoolSize = 80;
        int keepAliveTime = 10;

        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>(maximumPoolSize);
        Executor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS, workQueue);

        Log.d("THREADS", "start");
        Log.d("THREADS", "1 ->" + mThreadPool.size());
        for (int i = 0; i < MAX_THREADS; i++) {
            mThreadPool.add(new DropBoxTask(mListener, i));
        }
        Log.d("THREADS", "2 ->" + mThreadPool.size());

        for (DropBoxTask thread : mThreadPool) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                Log.d("THREADS", "asdf1");

                thread.executeOnExecutor(threadPoolExecutor);
            } else {
                thread.execute("");
            }
        }
    }

    private void startForeground() {
        int ID = 1234;
        Intent intent = new Intent(this, MainActivity.class);
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
        for (DropBoxTask thread : mThreadPool) {
            if (thread.isRunning()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public synchronized File onTaskFinished(DropBoxTask currentThread, File prevFile) {
        Log.d("THREADS", "ON TASK FINISHED");
        Log.d("THREADS", "ON TASK FINISHED");

        if (mSelectedFiles.isEmpty()) {
            currentThread.stopThread();
            if (isThreadPoolFinished()) {
                sendMessageToUI(100, prevFile);
                stopSelf();
                stopForeground(true);
            }
            return null;
        } else if (!isRunning()) {
            return null;
        } else {
            if (prevFile == null) {
                return getNextFile();
            }
            mCurrentProgress += mProgressStep;
            sendMessageToUI(mCurrentProgress, prevFile);
            return getNextFile();
        }
    }

    @Override
    public void networkConnectionStopped() {
        isRunning = false;
        stopSelf();
        stopForeground(true);
    }

    private File getNextFile() {
        return mSelectedFiles.poll();
    }

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
                case MSG_SET_PROGRESS_PERCENTAGE:
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
