package com.example.ivandimitrov.myapplication;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;


public class ServiceTask extends AppCompatActivity implements DropBoxConnection.FileReceivedListener {
    private FileAdapter mAdapter;
    private ListView    mFilesView;
    private ArrayList<File> mFileList    = new ArrayList<>();
    private ArrayList<File> selectedList = new ArrayList<>();

    private DropBoxConnection.FileReceivedListener mListener;
//    private AdapterView.OnItemClickListener        mListViewlistener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_task);
        mFilesView = (ListView) findViewById(R.id.files_list);

        mListener = this;
        mFileList = getListFiles(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));

        Button uploadButton = (Button) findViewById(R.id.button_upload);

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DropBoxConnection(mListener, selectedList).execute("");
            }
        });
        mAdapter = new FileAdapter(this, mFileList, selectedList);
        mFilesView.setAdapter(mAdapter);
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

    @Override
    public void onFileReceived(ArrayList<File> mFileList) {
        mAdapter.clear();
        mAdapter.addAll(mFileList);
        mAdapter.notifyDataSetChanged();
    }
}
