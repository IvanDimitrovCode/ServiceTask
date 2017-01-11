package com.example.ivandimitrov.myapplication;

import android.os.AsyncTask;
import android.util.Log;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.NetworkIOException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Ivan Dimitrov on 12/29/2016.
 */

public class DropBoxTask extends AsyncTask<String, String, String> {

    private static final String ACCESS_TOKEN = "ZXhz50v7KsAAAAAAAAAADmTbW-e_UBCDw4KY_16Z7VcI2G0WoMi7bBbiqLjV04Rm";

    private FileReceivedListener mListener;
    private int                  threadNumber;
    private boolean              isRunning;

    DropBoxTask(FileReceivedListener listener, int threadNumber) {
        this.threadNumber = threadNumber;
        this.mListener = listener;
    }

    @Override
    protected String doInBackground(String... strings) {
        DbxRequestConfig config = new DbxRequestConfig("dropbox/Apps/ServiceTask");
        Log.d("THREAD", "START");
        DbxClientV2 client = new DbxClientV2(config, ACCESS_TOKEN);
        File mFileForUpload = null;
        isRunning = true;
        while ((mFileForUpload = mListener.onTaskFinished(this, mFileForUpload)) != null) {
            Log.d("THREAD", "FILE");

            InputStream in = null;
            try {
                in = new FileInputStream(mFileForUpload);
                FileMetadata metadata = client.files().uploadBuilder("/" + mFileForUpload.getName()).uploadAndFinish(in);
            } catch (NetworkIOException e) {
                mListener.networkConnectionStopped();
                e.printStackTrace();
                break;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            publishProgress();
        }
        Log.d("THREAD", "STOP");
        isRunning = false;
        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
//        mListener.onFileReceived(mItemIndex);
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void stopThread() {
        isRunning = false;
    }

    interface FileReceivedListener {
        File onTaskFinished(DropBoxTask currentThread, File prevFile);

        void networkConnectionStopped();
    }
}
