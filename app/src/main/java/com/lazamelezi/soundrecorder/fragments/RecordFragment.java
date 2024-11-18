package com.lazamelezi.soundrecorder.fragments;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.visualizer.amplitude.AudioRecordView;
import com.lazamelezi.soundrecorder.Helper.MySharedPreferences;
import com.lazamelezi.soundrecorder.R;
import com.lazamelezi.soundrecorder.database.DBHelper;
import com.lazamelezi.soundrecorder.service.RecordingService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.lazamelezi.soundrecorder.fragments.FileViewerFragment.SOUND_RECORDER_WITH_SEP;

public class RecordFragment extends Fragment {

    public static final int GRANTED = 0;
    public static final int DENIED = 1;
    public static final int BLOCKED_OR_NEVER_ASKED = 2;
    static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
    private String[] mPermissions;

    private Handler mHandler = new Handler();
    private AppCompatActivity appCompatActivity;
    private ExtendedFloatingActionButton mRecordButton = null;
    private TextView mRecordingPrompt;
    private int mRecordPromptCount = 0;
    private boolean mStartRecording = true;
    private boolean mPauseRecording = true;
    private Chronometer chronometer = null;
    private ConstraintLayout mainLayout;
    private boolean isRecordStarted = false;
    private AudioRecordView audioRecordView;
    private DBHelper dbHelper;

    public RecordFragment() {
    }

    public RecordFragment(AppCompatActivity appCompatActivity) {
        this.appCompatActivity = appCompatActivity;
    }

    public boolean isRecordStarted() {
        return isRecordStarted;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            mPermissions = new String[]{
                    Manifest.permission.RECORD_AUDIO
            };

        } else {

            mPermissions = new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        dbHelper = new DBHelper(appCompatActivity);

        View recordView = inflater.inflate(R.layout.fragment_record, container, false);

        audioRecordView = recordView.findViewById(R.id.audioRecordView);
        mainLayout = recordView.findViewById(R.id.fragment_record);
        chronometer = recordView.findViewById(R.id.chronometer);
        mRecordingPrompt = recordView.findViewById(R.id.recording_status_text);
        mRecordButton = recordView.findViewById(R.id.btnRecord);
        mRecordButton.shrink();

        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                switch (PermissionsAllChecker()) {
                    case GRANTED:
                        onRecord(mStartRecording);
                        mStartRecording = !mStartRecording;
                        break;
                    case DENIED:
                        CheckPermissions();
                        break;
                    case BLOCKED_OR_NEVER_ASKED:
                        permissionBlocked();
                        break;
                    default:
                        break;
                }


            }
        });

        RecordingService.setMyServiceListener(new RecordingService.MyServiceListener() {
            @Override
            public void onResult(int response) {
                audioRecordView.update(response);
            }
        });
        return recordView;
    }

    private void onRecord(boolean start) {

        Intent intent = new Intent(appCompatActivity, RecordingService.class);

        if (start) {

            isRecordStarted = true;

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mRecordButton.extend();
                    mRecordButton.setIconResource(R.drawable.ic_media_stop);
                }
            }, 150);


            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                File folder = new File(Environment.getExternalStorageDirectory() + SOUND_RECORDER_WITH_SEP);
                if (!folder.exists())
                    folder.mkdir();
            }


            chronometer.setBase(SystemClock.elapsedRealtime());
            chronometer.start();
            chronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
                @Override
                public void onChronometerTick(Chronometer chronometer) {

                    switch (mRecordPromptCount) {
                        case 0:
                            mRecordingPrompt.setText(getString(R.string.record_in_progress) + ".");
                            break;
                        case 1:
                            mRecordingPrompt.setText(getString(R.string.record_in_progress) + "..");
                            break;
                        case 2:
                            mRecordingPrompt.setText(getString(R.string.record_in_progress) + "...");
                            mRecordPromptCount = -1;
                            break;
                        default:
                            break;
                    }

                    mRecordPromptCount++;

                }
            });

            appCompatActivity.startService(intent);

            appCompatActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            mRecordingPrompt.setText(getString(R.string.record_in_progress) + ".");
            mRecordPromptCount++;

        } else {

            audioRecordView.recreate();

            isRecordStarted = false;

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mRecordButton.shrink();
                    mRecordButton.setIconResource(R.drawable.ic_mic);
                }
            }, 150);

            chronometer.stop();
            chronometer.setBase(SystemClock.elapsedRealtime());
            mRecordingPrompt.setText(getString(R.string.tap_the_button_to_start_recording));

            appCompatActivity.stopService(intent);

            appCompatActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        }
    }

    private void permissionBlocked() {

        AlertDialog.Builder dialog = new AlertDialog.Builder(appCompatActivity);

        dialog.setTitle("Permissions are blocked!");
        dialog.setMessage("Hello you have permanently blocked your permissions please go into your Settings > Apps > Sound Recorder > Permissions to enable them manually.");
        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog alertDialog = dialog.create();
        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    private void CheckPermissions() {

        int result;

        List<String> listPermissionsNeeded = new ArrayList<>();

        for (String permission : mPermissions) {

            result = ContextCompat.checkSelfPermission(appCompatActivity, permission);

            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(permission);
            }

        }

        if (!listPermissionsNeeded.isEmpty()) {
            requestPermissions(listPermissionsNeeded.toArray(new String[0]), REQUEST_ID_MULTIPLE_PERMISSIONS);
        }

    }

    private int PermissionsAllChecker() {

        if (MySharedPreferences.isFirstTimeAskingPermission(appCompatActivity)) {

            MySharedPreferences.firstTimeAskingPermission(appCompatActivity, false);

            for (String permission : mPermissions) {
                if (ActivityCompat.checkSelfPermission(appCompatActivity, permission) != PackageManager.PERMISSION_GRANTED)
                    return DENIED;
            }


        } else {

            for (String permission : mPermissions) {

                if (ActivityCompat.checkSelfPermission(appCompatActivity, permission) != PackageManager.PERMISSION_GRANTED) {

                    if (shouldShowRequestPermissionRationale(permission)) {
                        return DENIED;
                    } else {
                        return BLOCKED_OR_NEVER_ASKED;
                    }
                }
            }

        }
        return GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_ID_MULTIPLE_PERMISSIONS) {

            int recorderPermission = ContextCompat.checkSelfPermission(appCompatActivity, Manifest.permission.RECORD_AUDIO);

            if (recorderPermission == PackageManager.PERMISSION_GRANTED) {
                onRecord(mStartRecording);
                mStartRecording = !mStartRecording;
            } else {
                Toast.makeText(appCompatActivity, getString(R.string.denied_permission), Toast.LENGTH_SHORT).show();
            }

        }

    }

    public void stopService() {
        onRecord(false);
    }
}
