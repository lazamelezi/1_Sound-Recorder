package com.lazamelezi.soundrecorder.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.lazamelezi.soundrecorder.Helper.MySharedPreferences;
import com.lazamelezi.soundrecorder.R;
import com.lazamelezi.soundrecorder.activities.MainActivity;
import com.lazamelezi.soundrecorder.database.DBHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static com.lazamelezi.soundrecorder.fragments.FileViewerFragment.SOUND_RECORDER_WITH_SEP;

public class RecordingService extends Service {

    public static final String SOUND_RECORDER_ID = "SoundRecorderID";
    public static final String SOUND_RECORDER = "SoundRecorder";
    private static final String LOG_TAG = "RecordingServiceLog";
    @SuppressLint("ConstantLocale")
    private static final SimpleDateFormat mTimerFormat = new SimpleDateFormat("mm:ss", Locale.getDefault());
    private static MyServiceListener myServiceListener;
    private MediaRecorder mRecorder = null;
    private String mFilePath;
    private String mFileName = null;
    private ContentResolver mContentResolver;
    private ContentValues mContentValues;
    private Uri mFileURI;
    private ParcelFileDescriptor file;
    private DBHelper mDatabase;
    private long mStartingTimeMillis = 0;
    private long mElapsedMillis = 0;
    private long mElapsedSeconds = 0;
    private Timer mTimerVisual;
    private Timer mTimer;
    private TimerTask mIncrementTimerTask;

    public static MyServiceListener getMyServiceListener() {
        return RecordingService.myServiceListener;
    }

    public static void setMyServiceListener(MyServiceListener myServiceListener) {
        RecordingService.myServiceListener = myServiceListener;
    }

    @SuppressLint("Range")
    public static Uri getRealPathFromURI(Context context, String mFileName) {

        String mAudioFileID = "";

        String[] projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME
        };

        String selection = MediaStore.Audio.Media.DISPLAY_NAME + "=?";

        final String[] selectionArgs = new String[]{
                mFileName
        };

        String sortOrder = MediaStore.Audio.Media.DISPLAY_NAME + " ASC";

        Uri collection;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        try (
                Cursor cursor = context.getContentResolver().query(
                        collection,
                        projection,
                        selection,
                        selectionArgs,
                        sortOrder
                )
        ) {

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    mAudioFileID = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                }

            }

        }

        return Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "" + mAudioFileID);

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mDatabase = new DBHelper(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        startRecording();

        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {

        Log.d(LOG_TAG, "onTaskRemoved called");

        if (mRecorder != null)
            stopRecording();

        super.onTaskRemoved(rootIntent);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRecording();
    }

    @SuppressLint("ForegroundServiceType")
    private void startRecording() {

        setFileNameAndPath();

        mRecorder = new MediaRecorder();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            mContentResolver = getContentResolver();
            mContentValues = new ContentValues();
            mContentValues.put(MediaStore.Audio.Media.DATE_ADDED, (System.currentTimeMillis() / 1000));
            mContentValues.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp3");
            mContentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + File.separator + SOUND_RECORDER);

            mContentValues.put(MediaStore.Files.FileColumns.DISPLAY_NAME, mFileName);
            mFileURI = mContentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mContentValues);

            mFilePath = String.valueOf(getRealPathFromURI(this, mFileName));
            Log.d(LOG_TAG, mFilePath);

            try {
                file = mContentResolver.openFileDescriptor(mFileURI, "w");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            mRecorder.setOutputFile(file.getFileDescriptor());

        } else {
            mFilePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            mFilePath = mFilePath + SOUND_RECORDER_WITH_SEP + "/" + mFileName;
            mRecorder.setOutputFile(mFilePath);
        }

        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mRecorder.setAudioChannels(1);

        if (MySharedPreferences.getPrefHighQuality(this)) {
            mRecorder.setAudioSamplingRate(44100);
            mRecorder.setAudioEncodingBitRate(192000);
        }

        try {
            mRecorder.prepare();
            mRecorder.start();
            mStartingTimeMillis = System.currentTimeMillis();

            voiceVisual();
            startTimer();
            startForeground(1, createNotification());

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void stopRecording() {

        if (mTimerVisual != null) {
            mTimerVisual.cancel();
            mTimerVisual = null;
        }

        try {
            mRecorder.stop();
            mElapsedMillis = (System.currentTimeMillis() - mStartingTimeMillis);
            mRecorder.release();

            Toast.makeText(this, getString(R.string.toast_recording_finish) + " " + mFilePath, Toast.LENGTH_LONG).show();

            if (mIncrementTimerTask != null) {
                mIncrementTimerTask.cancel();
                mIncrementTimerTask = null;
            }

            mRecorder = null;

            try {
                mDatabase.addRecording(mFileName, mFilePath, mElapsedMillis);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Exception", e);
            }

            stopForeground(true);
        } catch (RuntimeException e) {
            Log.e(LOG_TAG, "RuntimeException", e);
        }

    }

    private void startTimer() {

        mTimer = new Timer();
        mIncrementTimerTask = new TimerTask() {
            @Override
            public void run() {
                mElapsedSeconds++;
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(1, createNotification());
            }
        };
        mTimer.scheduleAtFixedRate(mIncrementTimerTask, 1000, 1000);
    }

    private void voiceVisual() {
        mTimerVisual = new Timer();
        mTimerVisual.schedule(new TimerTask() {
            @Override
            public void run() {
                if (getMyServiceListener() != null && mRecorder != null) {
                    getMyServiceListener().onResult(mRecorder.getMaxAmplitude());
                }
            }
        }, 0, 100);
    }

    private Notification createNotification() {

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mNoBuilder = new NotificationCompat.Builder(this, SOUND_RECORDER_ID);
        NotificationChannel notificationChannel;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel(SOUND_RECORDER_ID, SOUND_RECORDER, NotificationManager.IMPORTANCE_LOW);

            if (notificationManager != null && notificationManager.getNotificationChannel(SOUND_RECORDER_ID) == null) {
                notificationManager.createNotificationChannel(notificationChannel);
            }

            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationChannel.setSound(null, null);
            notificationChannel.setShowBadge(false);
        }

        mNoBuilder.setContentTitle(getString(R.string.notification_recording))
                .setContentText(mTimerFormat.format(mElapsedSeconds * 1000))
                .setOngoing(true)
                .setSound(null)
                .setVibrate(null)
                .setSmallIcon(R.drawable.ic_mic);

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mNoBuilder.setContentIntent(PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_IMMUTABLE));

        return mNoBuilder.build();
    }


    private void setFileNameAndPath() {

        Date date = Calendar.getInstance().getTime();
        @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String strDate = dateFormat.format(date);
        strDate = strDate.replaceAll("[^a-zA-Z0-9]", "");

        mFileName = "My Recording" + "_" + strDate + ".mp3";

        Log.d("FileViewerAdapter", "mFileName - " + mFileName);
    }


    public interface MyServiceListener {
        void onResult(int response);
    }
}