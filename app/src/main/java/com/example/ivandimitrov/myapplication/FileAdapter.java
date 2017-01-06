package com.example.ivandimitrov.myapplication;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
    private ArrayList<File> mFiles;
    private ArrayList<File> mSelectedList;
    private Activity        mActivity;

    public FileAdapter(Activity activity, ArrayList<File> files, ArrayList<File> selectedList) {
        super(activity, R.layout.list_node, files);
        this.mFiles = files;
        this.mActivity = activity;
        this.mSelectedList = selectedList;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        final ViewHolder viewHolder;
        final int positionFinal = position;
        if (view == null) {
            LayoutInflater inflater = mActivity.getLayoutInflater();
            viewHolder = new ViewHolder();
            view = inflater.inflate(R.layout.list_node, null, true);
            viewHolder.simpleCheckedTextView = (CheckedTextView) view.findViewById(R.id.file_name);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        viewHolder.simpleCheckedTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (viewHolder.simpleCheckedTextView.isChecked()) {
                    viewHolder.simpleCheckedTextView.setCheckMarkDrawable(new ColorDrawable(Color.TRANSPARENT));
                    viewHolder.simpleCheckedTextView.setChecked(false);
                    mSelectedList.remove(mFiles.get(positionFinal));
                } else {
                    viewHolder.simpleCheckedTextView.setCheckMarkDrawable(R.drawable.checked);
                    viewHolder.simpleCheckedTextView.setChecked(true);
                    mSelectedList.add(mFiles.get(positionFinal));
                }
            }
        });
        viewHolder.simpleCheckedTextView.setText(mFiles.get(position).getName());
        return view;
    }

    static class ViewHolder {
        CheckedTextView simpleCheckedTextView;
    }
}
