package com.example.ivandimitrov.myapplication;

import android.os.AsyncTask;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by Ivan Dimitrov on 12/29/2016.
 */

public class DropBoxConnection extends AsyncTask<String, String, String> {

    private static final String ACCESS_TOKEN = "ZXhz50v7KsAAAAAAAAAADmTbW-e_UBCDw4KY_16Z7VcI2G0WoMi7bBbiqLjV04Rm";

    private ArrayList<File>      mFileList;
    private FileReceivedListener mListener;
    private int mItemIndex = 0;

    DropBoxConnection(FileReceivedListener listener, ArrayList<File> mFileList) {
        this.mListener = listener;
        this.mFileList = mFileList;
    }

    @Override
    protected String doInBackground(String... strings) {
        DbxRequestConfig config = new DbxRequestConfig("dropbox/Apps/ServiceTask");
        DbxClientV2 client = new DbxClientV2(config, ACCESS_TOKEN);

        for (int i = 0; i < mFileList.size(); i++) {
            InputStream in = null;
            try {
                in = new FileInputStream(mFileList.get(i));
                FileMetadata metadata = client.files().uploadBuilder("/" + mFileList.get(i).getName()).uploadAndFinish(in);
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
            mItemIndex++;
            publishProgress();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        mListener.onFileReceived(mItemIndex);
    }

    interface FileReceivedListener {
        void onFileReceived(int itemIndex);
    }

}
