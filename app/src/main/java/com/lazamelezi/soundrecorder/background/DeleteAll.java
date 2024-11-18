package com.lazamelezi.soundrecorder.background;

import android.content.ContentProviderOperation;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.lazamelezi.soundrecorder.R;
import com.lazamelezi.soundrecorder.adapters.FileViewerAdapter;
import com.lazamelezi.soundrecorder.database.DBHelper;
import com.lazamelezi.soundrecorder.listeners.UpdateListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.lazamelezi.soundrecorder.fragments.FileViewerFragment.SOUND_RECORDER_WITH_SEP;

public class DeleteAll implements UpdateListener {

    private final AppCompatActivity mAppCompatActivity;
    private final FileViewerAdapter mFileViewerAdapter;
    private final DBHelper mDatabase;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final DeleteAllExecutor deleteAllExecutor;
    private AlertDialog alertDialog;
    private TextView mProgressTextView;
    private ProgressBar progressBar;

    public DeleteAll(AppCompatActivity mAppCompatActivity, FileViewerAdapter mFileViewerAdapter, DBHelper mDatabase) {
        this.mAppCompatActivity = mAppCompatActivity;
        this.mFileViewerAdapter = mFileViewerAdapter;
        this.mDatabase = mDatabase;
        deleteAllExecutor = new DeleteAllExecutor(mAppCompatActivity, mDatabase, this);
    }

    public void execute() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(mAppCompatActivity);
        LayoutInflater layoutInflater = LayoutInflater.from(mAppCompatActivity);
        View dialogView = layoutInflater.inflate(R.layout.alert_dialog_database_delete, null);

        mProgressTextView = dialogView.findViewById(R.id.mProgressTextView);
        progressBar = dialogView.findViewById(R.id.progressBar);

        int maxItems = mDatabase.getCount();
        progressBar.setMax(maxItems);

        dialog.setTitle(R.string.please_wait);
        dialog.setMessage(R.string.deleting_all);
        dialog.setView(dialogView);
        alertDialog = dialog.create();
        alertDialog.setCancelable(false);

        if (!mAppCompatActivity.isFinishing())
            alertDialog.show();

        deleteAllExecutor.doInBackground();
    }

    @Override
    public void update(String progress) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (progressBar != null) {
                    mProgressTextView.setText(progress);
                    progressBar.setProgress(Integer.parseInt(progress));
                }
            }
        });
    }

    @Override
    public void finish() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                alertDialog.dismiss();
                mFileViewerAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void finish(List<String> deletedFiles) {

    }

    static class DeleteAllExecutor {

        private final AppCompatActivity mAppCompatActivity;
        private final DBHelper mDatabase;
        private final int MaxItems;
        private final UpdateListener updateListener;

        public DeleteAllExecutor(AppCompatActivity mAppCompatActivity, DBHelper mDatabase, UpdateListener updateListener) {
            this.updateListener = updateListener;
            this.mAppCompatActivity = mAppCompatActivity;
            this.mDatabase = mDatabase;

            MaxItems = mDatabase.getCount();
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

                    updateListener.finish();
                }
            });

        }

        @RequiresApi(api = Build.VERSION_CODES.Q)
        private void deleteFilesQ() {

            ArrayList<ContentProviderOperation> operationList = new ArrayList<>();
            ContentProviderOperation contentProviderOperation;

            Uri uri = null;
            String mAudioFileID;

            String[] projection = new String[]{
                    MediaStore.Audio.Media._ID
            };

            String selection = MediaStore.Audio.Media._ID + "=?";


            Uri collection;

            collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);


            try (

                    Cursor cursor = mAppCompatActivity.getContentResolver().query(
                            collection,
                            projection,
                            null,
                            null,
                            null

                    )

            ) {

                if (cursor != null) {

                    if (cursor.moveToFirst()) {

                        do {

                            mAudioFileID = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID));

                            uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "" + mAudioFileID);

                            contentProviderOperation = ContentProviderOperation
                                    .newDelete(uri)
                                    .withSelection(selection, new String[]{mAudioFileID})
                                    .build();

                            operationList.add(contentProviderOperation);
                        }
                        while (cursor.moveToNext());
                    }
                    cursor.close();
                }

                if (operationList.size() > 0) {

                    try {
                        mAppCompatActivity.getContentResolver().applyBatch(uri.getAuthority(), operationList);
                        mDatabase.deleteAllData();
                    } catch (OperationApplicationException | RemoteException e) {
                        e.printStackTrace();
                    }

                    for (int i = 0; i < operationList.size(); i++) {
                        updateListener.update(String.valueOf(i));
                    }

                    updateListener.update(String.valueOf(MaxItems));

                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private void deleteFilesNormal() {

            File file = new File(Environment.getExternalStorageDirectory() + SOUND_RECORDER_WITH_SEP);

            if (file.length() > 0) {

                String[] files;
                files = file.list();

                for (int i = 0; i < files.length; i++) {

                    File myFile = new File(file, files[i]);

                    myFile.delete();

                    updateListener.update(String.valueOf(i));

                }

                mDatabase.deleteAllData();

                updateListener.update(String.valueOf(MaxItems));

                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}