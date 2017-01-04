package com.example.ivandimitrov.myapplication;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Ivan Dimitrov on 12/29/2016.
 */

public class FileAdapter extends ArrayAdapter<File> {
    private ArrayList<File> files;
    private LayoutInflater  inflater;
    private ArrayList<File> selectedList;

    public FileAdapter(Activity activity, ArrayList<File> files, ArrayList<File> selectedList) {
        super(activity, R.layout.list_node, files);
        this.files = files;
        this.selectedList = selectedList;
        inflater = (LayoutInflater.from(activity));
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        Log.d("ADAPTER", "in");
        view = inflater.inflate(R.layout.list_node, null);
        final CheckedTextView simpleCheckedTextView = (CheckedTextView) view.findViewById(R.id.file_name);
        final int positionFinal = position;
        simpleCheckedTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (simpleCheckedTextView.isChecked()) {
                    simpleCheckedTextView.setCheckMarkDrawable(new ColorDrawable(Color.TRANSPARENT));
                    simpleCheckedTextView.setChecked(false);
                    selectedList.remove(files.get(positionFinal));

                } else {
                    simpleCheckedTextView.setCheckMarkDrawable(R.drawable.checked);
                    simpleCheckedTextView.setChecked(true);
                    selectedList.add(files.get(positionFinal));
                }
            }
        });
        simpleCheckedTextView.setText(files.get(position).getName());

        return view;
    }
}
