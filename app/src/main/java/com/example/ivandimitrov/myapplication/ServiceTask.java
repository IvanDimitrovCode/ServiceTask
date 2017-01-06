package com.example.ivandimitrov.myapplication;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;


public class ServiceTask extends AppCompatActivity {
    private final Messenger mMessenger = new Messenger(new IncomingHandler());

    private FileAdapter mAdapter;
    private Messenger   mService;
    private boolean     mIsBound;
    private ProgressBar mProgressBar;
    private Button      mUploadButton;
    private ListView    mFilesView;

    private ArrayList<File> mFileList     = new ArrayList<>();
    private ArrayList<File> mSelectedList = new ArrayList<>();

    @Override
    protected void onResume() {
        super.onResume();
        if (MyService.isRunning()) {
            Toast.makeText(this, getString(R.string.serviceRunning), Toast.LENGTH_LONG).show();
            doBindService();
        } else {
            Toast.makeText(this, getString(R.string.serviceNotRunning), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_task);

        mFilesView = (ListView) findViewById(R.id.files_list);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mUploadButton = (Button) findViewById(R.id.button_upload);

        mFileList = getListFiles(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));

        mUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mProgressBar.setProgress(0);
                if (getSelectedList().isEmpty()) {
                    return;
                }
                Intent intent = new Intent(getApplicationContext(), MyService.class);
                intent.putExtra("data", getSelectedList());
                startService(intent);
                doBindService();
            }
        });
        mAdapter = new FileAdapter(this, mFileList, mSelectedList);
        mFilesView.setAdapter(mAdapter);
    }

    private ArrayList<String> getSelectedList() {
        ArrayList<String> list = new ArrayList<>();
        for (File file : mSelectedList) {
            list.add(file.getAbsolutePath());
        }
        return list;
    }

    void doBindService() {
        bindService(new Intent(this, MyService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        doUnbindService();
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            try {
                Message msg = Message.obtain(null, MyService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
                //sendMessageToService(1);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MyService.MSG_SET_INT_VALUE:
                    mProgressBar.setProgress(msg.arg1);
                    break;
                case MyService.MSG_SET_STRING_VALUE:
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }


    private void sendMessageToService(int intvaluetosend) {
        if (mIsBound) {
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, MyService.MSG_SET_INT_VALUE, intvaluetosend, 0);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
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

    void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, MyService.MSG_UNREGISTER_CLIENT);
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
}
