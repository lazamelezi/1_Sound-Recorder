package com.lazamelezi.soundrecorder.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.lazamelezi.soundrecorder.Helper.util;
import com.lazamelezi.soundrecorder.R;
import com.lazamelezi.soundrecorder.adapters.FileViewerAdapter;
import com.lazamelezi.soundrecorder.background.TrackFileChanges;

import java.util.List;

public class FileViewerFragment extends Fragment {

    public static final String SOUND_RECORDER_WITH_SEP = "/SoundRecorder";
    private static final int delete_all = R.id.delete_all;
    private static final int select_multi_item = R.id.select_multi_item;
    public ActionMode actionMode;
    private AppCompatActivity appCompatActivity;
    private ConstraintLayout mainLayout;
    private RecyclerView mRecyclerView;
    private LinearLayout noDataLinearLayout;
    private FileViewerAdapter fileViewerAdapter;
    private ActionCallBack actionCallBack;

    public FileViewerFragment() {
    }

    public FileViewerFragment(AppCompatActivity appCompatActivity) {
        this.appCompatActivity = appCompatActivity;
    }

    private void toggleActionBar() {
        if (actionMode == null)
            actionMode = appCompatActivity.startSupportActionMode(actionCallBack);
    }

    private void deleteMultiListSelection() {
        List<Integer> selectedItemPositions = fileViewerAdapter.getSelectedItems();
        for (int i = selectedItemPositions.size() - 1; i >= 0; i--) {
            fileViewerAdapter.removeItems(selectedItemPositions.get(i));
        }
        fileViewerAdapter.notifyDataSetChanged();
    }

    public void trackWhenFileDelete() {
        if (fileViewerAdapter != null) {
            TrackFileChanges trackFileChanges;
            trackFileChanges = new TrackFileChanges(appCompatActivity, fileViewerAdapter);
            trackFileChanges.execute();
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {

        inflater.inflate(R.menu.saved_records_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case delete_all:

                if (!fileViewerAdapter.removeOutOfApp()) {
                    noRecordsSnackBar();
                }

                return true;
            case select_multi_item:

                if (fileViewerAdapter.getItemCount() > 0)
                    toggleActionBar();
                else
                    noRecordsSnackBar();

                return true;
            default:
                break;
        }

        return false;
    }

    private void noRecordsSnackBar() {
        Snackbar snackbar = Snackbar.make(mainLayout, "No records are saved...", Snackbar.LENGTH_SHORT)
                .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                .setBackgroundTint(getResources().getColor(R.color.primary))
                .setActionTextColor(Color.WHITE)
                .setTextColor(Color.WHITE);
        snackbar.show();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View recordView = inflater.inflate(R.layout.fragment_file_viewer, container, false);

        mainLayout = recordView.findViewById(R.id.fragment_file_viewer);
        mRecyclerView = recordView.findViewById(R.id.recyclerView);

        noDataLinearLayout = recordView.findViewById(R.id.noDataLinearLayout);


        mRecyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(appCompatActivity);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);

        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        actionCallBack = new ActionCallBack();

        fileViewerAdapter = new FileViewerAdapter(appCompatActivity, this, layoutManager);
        mRecyclerView.setAdapter(fileViewerAdapter);

        fileViewerAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                mRecyclerView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        checkData();
                    }
                }, 300);
            }
        });

        checkData();

        return recordView;
    }

    public void checkData() {
        if (fileViewerAdapter != null) {
            if (fileViewerAdapter.getItemCount() > 0) {
                noDataLinearLayout.setVisibility(View.INVISIBLE);
                mRecyclerView.setVisibility(View.VISIBLE);
            } else {
                mRecyclerView.setVisibility(View.INVISIBLE);
                noDataLinearLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkData();
    }

    private class ActionCallBack implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            util.toggleStatusBarColor(appCompatActivity, getResources().getColor(R.color.primary));
            mode.getMenuInflater().inflate(R.menu.select_menu, menu);
            mode.setTitle(appCompatActivity.getString(R.string.selected, 0));
            fileViewerAdapter.setSelectModeOn(true);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

            if (item.getItemId() == R.id.agree_select) {
                deleteMultiListSelection();
                mode.finish();
            }

            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            fileViewerAdapter.clearSelection();
            actionMode = null;
            util.toggleStatusBarColor(appCompatActivity, getResources().getColor(R.color.primary_dark));
            fileViewerAdapter.setSelectModeOn(false);
        }
    }
}
