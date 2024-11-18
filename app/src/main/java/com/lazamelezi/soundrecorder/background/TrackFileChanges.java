package com.lazamelezi.soundrecorder.background;

import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;

import androidx.appcompat.app.AppCompatActivity;

import com.lazamelezi.soundrecorder.adapters.FileViewerAdapter;
import com.lazamelezi.soundrecorder.listeners.UpdateListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.lazamelezi.soundrecorder.fragments.FileViewerFragment.SOUND_RECORDER_WITH_SEP;

public class TrackFileChanges implements UpdateListener {

    private final FileViewerAdapter mFileViewerAdapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final CheckIfDeleted checkIfDeleted;

    public TrackFileChanges(AppCompatActivity appCompatActivity, FileViewerAdapter mFileViewerAdapter) {
        this.mFileViewerAdapter = mFileViewerAdapter;
        checkIfDeleted = new CheckIfDeleted(appCompatActivity, this);
    }

    public void execute() {
        checkIfDeleted.doInBackground();
    }

    @Override
    public void update(String progress) {

    }

    @Override
    public void finish() {

    }

    @Override
    public void finish(List<String> deletedFiles) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                mFileViewerAdapter.checkIfFileIsDeletedOutSide(deletedFiles);
                deletedFiles.clear();
                mFileViewerAdapter.notifyDataSetChanged();
            }
        });
    }

    static class CheckIfDeleted {

        private final AppCompatActivity mAppCompatActivity;
        private final UpdateListener updateListener;

        public CheckIfDeleted(AppCompatActivity mAppCompatActivity, UpdateListener updateListener) {
            this.mAppCompatActivity = mAppCompatActivity;
            this.updateListener = updateListener;
        }

        public void doInBackground() {

            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        deleteFilesQ();
                    else
                        deleteFilesNormal();
                }
            });

        }


        private void deleteFilesQ() {

            String mFileName;

            List<String> filesNotInStorage = new ArrayList<>();

            String[] projection = new String[]{
                    MediaStore.Audio.Media.DISPLAY_NAME
            };

            Uri collection;
            collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;


            try (Cursor cursor = mAppCompatActivity.getContentResolver().query(
                    collection,
                    projection,
                    null,
                    null
                    , null)
            ) {

                if (cursor != null) {

                    if (cursor.moveToFirst()) {

                        do {
                            mFileName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
                            filesNotInStorage.add(mFileName);
                        }
                        while (cursor.moveToNext());
                    }
                    cursor.close();
                }
            }

            updateListener.finish(filesNotInStorage);

        }

        private void deleteFilesNormal() {

            List<String> filesNotInStorage = new ArrayList<>();

            File file = new File(Environment.getExternalStorageDirectory() + SOUND_RECORDER_WITH_SEP);
            String[] files;

            files = file.list();

            for (String name : files) {

                File myFile = new File(file, name);
                filesNotInStorage.add(myFile.getName());

            }
            updateListener.finish(filesNotInStorage);
        }

    }

}