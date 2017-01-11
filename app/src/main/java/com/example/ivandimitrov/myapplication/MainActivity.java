package com.example.ivandimitrov.myapplication;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

//TODO to be able to stop internet multiple times

public class MainActivity extends AppCompatActivity implements NetworkStateReceiver.NetworkListener {
    public static final String SHARED_PREF_KEY = "remainingFiles";

    private final Messenger mMessenger = new Messenger(new IncomingHandler());

    private String  ACTION             = "android.net.conn.CONNECTIVITY_CHANGE";
    private boolean isNetworkAvailable = false;
    private boolean mIsBound;

    private NetworkStateReceiver receiver = new NetworkStateReceiver(this);
    private FileAdapter mAdapter;
    private Messenger   mService;
    private ProgressBar mProgressBar;
    private ListView    mFilesView;
    private Button      mUploadButton;

    private ArrayList<File> mFileList      = new ArrayList<>();
    private ArrayList<File> mSelectedList  = new ArrayList<>();
    private ArrayList<File> mRemainingList = new ArrayList<>();

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            try {
                Message msg = Message.obtain(null, UploadService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    //CALLBACK METHODS
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_task);

        registerReceiver(receiver, new IntentFilter(ACTION));
        initResources();
        initButtonFunctionality();

        mFileList = getListFiles(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
        mAdapter = new FileAdapter(this, mFileList, mSelectedList);
        mFilesView.setAdapter(mAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, new IntentFilter(ACTION));
        if (UploadService.isRunning()) {
            doBindService();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        doUnbindService();
    }

    //LISTENERS METHODS
    @Override
    public void onInternetChanged(boolean internetWorking) {
        isNetworkAvailable = internetWorking;
        if (isNetworkAvailable) {
            mUploadButton.setEnabled(true);

            // CHECK IF THERE ARE REMAINING FILES INSIDE THE SHARE PREF
            Toast.makeText(this, "" + readFromSharedPref().size(), Toast.LENGTH_LONG).show();
            if (readFromSharedPref().size() > 0) {
                startUploadService(readFromSharedPref());
                clearSharePref();
            }
        } else {
            if (!getRemainingList().isEmpty()) {
                doUnbindService();
            }
            mUploadButton.setEnabled(false);
        }
    }

    //OTHER METHODS
    private void sendMessageToService(int valueToSend) {
        if (mIsBound) {
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, UploadService.MSG_SET_PROGRESS_PERCENTAGE, valueToSend, 0);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private ArrayList<String> getRemainingList() {
        ArrayList<String> list = new ArrayList<>();
        for (File file : mRemainingList) {
            if (file != null) {
                list.add(file.getAbsolutePath());
            }
        }
        return list;
    }

    private ArrayList<File> getListFiles(File parentDir) {
        ArrayList<File> inFiles = new ArrayList<File>();
        File[] files = parentDir.listFiles();

        for (File file : files) {
            if (file.isDirectory()) {
                inFiles.addAll(getListFiles(file));
            } else {
                if (file.getName().endsWith(".mp3")) {
                    inFiles.add(file);
                }
            }
        }
        return inFiles;
    }

    private ArrayList<String> getSelectedList() {
        ArrayList<String> list = new ArrayList<>();
        for (File file : mSelectedList) {
            list.add(file.getAbsolutePath());
        }
        return list;
    }

    private void doBindService() {
        bindService(new Intent(this, UploadService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    private void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, UploadService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                    // There is nothing special we need to do if the service has crashed.
                }
            }
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    private void initButtonFunctionality() {
        mUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mProgressBar.setProgress(0);
                if (getSelectedList().isEmpty()) {
                    return;
                }
                mRemainingList.addAll(mSelectedList);
                startUploadService(getSelectedList());
            }
        });
    }

    private void startUploadService(ArrayList<String> list) {
        Intent intent = new Intent(getApplicationContext(), UploadService.class);
        intent.putExtra("data", list);
        startService(intent);
        doBindService();
    }

    private void initResources() {
        mFilesView = (ListView) findViewById(R.id.files_list);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mUploadButton = (Button) findViewById(R.id.button_upload);
    }

    private void removeFile(String filePath) {
        for (File file : mRemainingList) {
            if (file.getAbsolutePath().equals(filePath)) {
                mRemainingList.remove(file);
                break;
            }
        }
    }

    private void writeToSharedPref() {
        Set<String> remainingSet = new HashSet<>();
        remainingSet.addAll(getRemainingList());
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putStringSet(SHARED_PREF_KEY, remainingSet);
        editor.commit();
    }

    private void clearSharePref() {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.commit();
    }

    private ArrayList<String> readFromSharedPref() {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        Set<String> remainingSet = new HashSet<>();
        remainingSet = sharedPref.getStringSet(SHARED_PREF_KEY, remainingSet);
        ArrayList<String> remainingList = new ArrayList<String>();
        remainingList.addAll(remainingSet);
        return remainingList;
    }

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UploadService.MSG_SET_PROGRESS_PERCENTAGE:
                    mProgressBar.setProgress(msg.arg1);
                    break;
                case UploadService.MSG_SET_STRING_VALUE:
                    String filePath = msg.getData().getString("filePath");
                    removeFile(filePath);
                    writeToSharedPref();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

}
