package com.example.ivandimitrov.myapplication;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Ivan Dimitrov on 1/6/2017.
 */

public class ProgressAwareInputStream extends InputStream {
    private InputStream        wrappedInputStream;
    private long               size;
    private long               counter;
    private long               lastPercent;
    private OnProgressListener listener;

    public ProgressAwareInputStream(InputStream in, long size) {
        wrappedInputStream = in;
        this.size = size;
    }

    public void setOnProgressListener(OnProgressListener listener) {
        this.listener = listener;
    }

    @Override
    public int read() throws IOException {
        counter += 1;
        check();
        return wrappedInputStream.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        int retVal = wrappedInputStream.read(b);
        counter += retVal;
        check();
        return retVal;
    }

    private void check() {
        int percent = (int) (counter * 100 / size);
        if (percent - lastPercent >= 10) {
            lastPercent = percent;
            if (listener != null)
                listener.onProgress(percent);
        }
    }

    public interface OnProgressListener {
        void onProgress(int percentage);
    }
}
